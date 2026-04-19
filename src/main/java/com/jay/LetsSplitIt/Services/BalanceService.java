package com.jay.LetsSplitIt.Services;

import com.jay.LetsSplitIt.Dto.BalanceSummary;
import com.jay.LetsSplitIt.Dto.FriendBalance;
import com.jay.LetsSplitIt.Entities.PairBalance;
import com.jay.LetsSplitIt.Entities.User;
import com.jay.LetsSplitIt.Repository.PairBalanceRepository;
import com.jay.LetsSplitIt.Repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class BalanceService {

    private final PairBalanceRepository pairBalanceRepository;
    private final UserRepository userRepository;

    BalanceService(PairBalanceRepository pairBalanceRepository, UserRepository userRepository) {
        this.pairBalanceRepository = pairBalanceRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void applyDebt(UUID debtorId, UUID creditorId, BigDecimal amount) {
        if (debtorId.equals(creditorId)) return;
        if (amount == null || amount.signum() <= 0) return;

        Optional<PairBalance> oppositeOpt =
                pairBalanceRepository.findByDebtorIdAndCreditorId(creditorId, debtorId);
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

        Optional<PairBalance> existingOpt =
                pairBalanceRepository.findByDebtorIdAndCreditorId(debtorId, creditorId);
        if (existingOpt.isPresent()) {
            PairBalance existing = existingOpt.get();
            existing.setAmount(existing.getAmount().add(amount));
            pairBalanceRepository.save(existing);
        } else {
            PairBalance fresh = new PairBalance(null, debtorId, creditorId, amount, null);
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

        Optional<PairBalance> iOwe = pairBalanceRepository.findByDebtorIdAndCreditorId(me, friendId);
        if (iOwe.isPresent()) {
            return new FriendBalance(friendId, iOwe.get().getAmount(), FriendBalance.Direction.I_OWE);
        }
        Optional<PairBalance> owesMe = pairBalanceRepository.findByDebtorIdAndCreditorId(friendId, me);
        if (owesMe.isPresent()) {
            return new FriendBalance(friendId, owesMe.get().getAmount(), FriendBalance.Direction.OWES_ME);
        }
        return new FriendBalance(friendId, BigDecimal.ZERO, FriendBalance.Direction.SETTLED);
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
