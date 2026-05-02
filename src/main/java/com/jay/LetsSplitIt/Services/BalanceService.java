package com.jay.LetsSplitIt.Services;

import com.jay.LetsSplitIt.Dto.BalanceSummary;
import com.jay.LetsSplitIt.Dto.FriendBalance;
import com.jay.LetsSplitIt.Entities.Group;
import com.jay.LetsSplitIt.Entities.PairBalance;
import com.jay.LetsSplitIt.Entities.User;
import com.jay.LetsSplitIt.Repository.GroupRepository;
import com.jay.LetsSplitIt.Repository.PairBalanceRepository;
import com.jay.LetsSplitIt.Repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.UUID;

@Service
public class BalanceService {

    private final PairBalanceRepository pairBalanceRepository;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;

    BalanceService(PairBalanceRepository pairBalanceRepository,
                   UserRepository userRepository,
                   GroupRepository groupRepository) {
        this.pairBalanceRepository = pairBalanceRepository;
        this.userRepository = userRepository;
        this.groupRepository = groupRepository;
    }

    @Transactional
    public void applyDebt(UUID debtorId, UUID creditorId, BigDecimal amount) {
        applyDebt(debtorId, creditorId, amount, null);
    }

    @Transactional
    public void applyDebt(UUID debtorId, UUID creditorId, BigDecimal amount, UUID groupId) {
        if (debtorId.equals(creditorId)) return;
        if (amount == null || amount.signum() <= 0) return;

        Optional<PairBalance> oppositeOpt = findPair(creditorId, debtorId, groupId);
        if (oppositeOpt.isPresent()) {
            PairBalance opposite = oppositeOpt.get();
            int cmp = opposite.getAmount().compareTo(amount);
            if (cmp > 0) {
                opposite.setAmount(opposite.getAmount().subtract(amount));
                pairBalanceRepository.save(opposite);
                return;
            }
            if (cmp == 0) {
                pairBalanceRepository.delete(opposite);
                return;
            }
            amount = amount.subtract(opposite.getAmount());
            pairBalanceRepository.delete(opposite);
        }

        Optional<PairBalance> existingOpt = findPair(debtorId, creditorId, groupId);
        if (existingOpt.isPresent()) {
            PairBalance existing = existingOpt.get();
            existing.setAmount(existing.getAmount().add(amount));
            pairBalanceRepository.save(existing);
        } else {
            PairBalance fresh = new PairBalance(null, debtorId, creditorId, groupId, amount, null);
            pairBalanceRepository.save(fresh);
        }
    }

    @Transactional(readOnly = true)
    public BalanceSummary getSummary(UserDetails userDetails) {
        UUID me = currentUserId(userDetails);

        BigDecimal youOwe = sum(pairBalanceRepository.findByDebtorId(me));
        BigDecimal youAreOwed = sum(pairBalanceRepository.findByCreditorId(me));
        BigDecimal net = youAreOwed.subtract(youOwe);

        return new BalanceSummary(youOwe, youAreOwed, net);
    }

    @Transactional(readOnly = true)
    public List<FriendBalance> getFriendsBalances(UserDetails userDetails) {
        UUID me = currentUserId(userDetails);

        List<FriendBalance> result = new ArrayList<>();
        for (PairBalance debt : pairBalanceRepository.findByDebtorId(me)) {
            result.add(new FriendBalance(debt.getCreditorId(), debt.getAmount(), FriendBalance.Direction.I_OWE));
        }
        for (PairBalance credit : pairBalanceRepository.findByCreditorId(me)) {
            result.add(new FriendBalance(credit.getDebtorId(), credit.getAmount(), FriendBalance.Direction.OWES_ME));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public FriendBalance getPairDetail(UserDetails userDetails, UUID friendId) {
        UUID me = currentUserId(userDetails);

        BigDecimal iOwe = sum(pairBalanceRepository.findByDebtorIdAndCreditorId(me, friendId));
        BigDecimal owesMe = sum(pairBalanceRepository.findByDebtorIdAndCreditorId(friendId, me));
        BigDecimal net = owesMe.subtract(iOwe);

        if (net.signum() > 0) {
            return new FriendBalance(friendId, net, FriendBalance.Direction.OWES_ME);
        }
        if (net.signum() < 0) {
            return new FriendBalance(friendId, net.negate(), FriendBalance.Direction.I_OWE);
        }
        return new FriendBalance(friendId, BigDecimal.ZERO, FriendBalance.Direction.SETTLED);
    }

    @Transactional
    public List<PairBalance> simplifyGroupDebts(UserDetails userDetails, UUID groupId) {
        UUID me = currentUserId(userDetails);
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new NoSuchElementException("Group not found: " + groupId));
        if (!group.getMembers().contains(me)) {
            throw new AccessDeniedException("Only group members can simplify debts");
        }

        List<PairBalance> existing = pairBalanceRepository.findByGroupId(groupId);
        if (existing.isEmpty()) {
            return List.of();
        }

        Map<UUID, BigDecimal> net = new HashMap<>();
        for (PairBalance pb : existing) {
            net.merge(pb.getCreditorId(), pb.getAmount(), BigDecimal::add);
            net.merge(pb.getDebtorId(), pb.getAmount().negate(), BigDecimal::add);
        }

        Comparator<NetEntry> byAmountDesc = (a, b) -> b.amount.compareTo(a.amount);
        PriorityQueue<NetEntry> creditors = new PriorityQueue<>(byAmountDesc);
        PriorityQueue<NetEntry> debtors = new PriorityQueue<>(byAmountDesc);
        for (Map.Entry<UUID, BigDecimal> e : net.entrySet()) {
            int sgn = e.getValue().signum();
            if (sgn > 0) {
                creditors.add(new NetEntry(e.getKey(), e.getValue()));
            } else if (sgn < 0) {
                debtors.add(new NetEntry(e.getKey(), e.getValue().negate()));
            }
        }

        List<PairBalance> simplified = new ArrayList<>();
        while (!creditors.isEmpty() && !debtors.isEmpty()) {
            NetEntry c = creditors.poll();
            NetEntry d = debtors.poll();
            BigDecimal pay = c.amount.min(d.amount);
            simplified.add(new PairBalance(null, d.id, c.id, groupId, pay, null));
            BigDecimal cRem = c.amount.subtract(pay);
            BigDecimal dRem = d.amount.subtract(pay);
            if (cRem.signum() > 0) creditors.add(new NetEntry(c.id, cRem));
            if (dRem.signum() > 0) debtors.add(new NetEntry(d.id, dRem));
        }

        pairBalanceRepository.deleteAllInBatch(existing);
        pairBalanceRepository.flush();
        return pairBalanceRepository.saveAll(simplified);
    }

    private record NetEntry(UUID id, BigDecimal amount) {}

    private Optional<PairBalance> findPair(UUID debtorId, UUID creditorId, UUID groupId) {
        if (groupId == null) {
            return pairBalanceRepository.findByDebtorIdAndCreditorIdAndGroupIdIsNull(debtorId, creditorId);
        }
        return pairBalanceRepository.findByDebtorIdAndCreditorIdAndGroupId(debtorId, creditorId, groupId);
    }

    private BigDecimal sum(List<PairBalance> balances) {
        return balances.stream()
                .map(PairBalance::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private UUID currentUserId(UserDetails userDetails) {
        String email = userDetails.getUsername();
        return userRepository.findByEmail(email)
                .map(User::getId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
    }
}
