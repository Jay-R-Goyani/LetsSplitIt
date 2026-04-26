package com.jay.LetsSplitIt.Services;

import com.jay.LetsSplitIt.Dto.ExpenseRequest;
import com.jay.LetsSplitIt.Dto.ExpenseResponse;
import com.jay.LetsSplitIt.Dto.ExpenseShare;
import com.jay.LetsSplitIt.Dto.SplitInput;
import com.jay.LetsSplitIt.Entities.Expense;
import com.jay.LetsSplitIt.Entities.Group;
import com.jay.LetsSplitIt.Entities.SharedExpense;
import com.jay.LetsSplitIt.Entities.User;
import com.jay.LetsSplitIt.Repository.ExpenseRepository;
import com.jay.LetsSplitIt.Repository.GroupRepository;
import com.jay.LetsSplitIt.Repository.ShareExpenseRepository;
import com.jay.LetsSplitIt.Repository.UserRepository;
import com.jay.LetsSplitIt.Services.Split.SplitStrategy;
import com.jay.LetsSplitIt.Services.Split.SplitStrategyRegistry;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;

@Service
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final ShareExpenseRepository shareExpenseRepository;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final SplitStrategyRegistry registry;
    private final BalanceService balanceService;

    ExpenseService(ExpenseRepository expenseRepository,
                   ShareExpenseRepository shareExpenseRepository,
                   UserRepository userRepository,
                   GroupRepository groupRepository,
                   SplitStrategyRegistry registry,
                   BalanceService balanceService) {
        this.expenseRepository = expenseRepository;
        this.shareExpenseRepository = shareExpenseRepository;
        this.userRepository = userRepository;
        this.groupRepository = groupRepository;
        this.registry = registry;
        this.balanceService = balanceService;
    }

    @Transactional
    public ExpenseResponse createExpense(UserDetails userDetails, ExpenseRequest request) {
        return createExpense(userDetails, request, null);
    }

    @Transactional
    public ExpenseResponse createExpense(UserDetails userDetails, ExpenseRequest request, UUID groupId) {
        SplitStrategy strategy = registry.get(request.splitType());
        strategy.validate(request.amount(), request.participants());
        List<ExpenseShare> shares = strategy.split(request.amount(), request.participants());

        UUID paidBy = currentUserId(userDetails);

        if (groupId != null) {
            validateGroupParticipants(groupId, paidBy, request.participants());
        }

        Expense expense = new Expense();
        expense.setPaidBy(paidBy);
        expense.setAmount(request.amount());
        expense.setSplitType(request.splitType());
        expense.setGroupId(groupId);

        Expense saved = expenseRepository.save(expense);

        List<SharedExpense> sharedRows = shares.stream()
                .filter(share -> !share.userId().equals(paidBy))
                .map(share -> new SharedExpense(null, saved.getId(), share.userId(), share.amountOwed()))
                .toList();
        shareExpenseRepository.saveAll(sharedRows);

        for (SharedExpense row : sharedRows) {
            balanceService.applyDebt(row.getUserId(), paidBy, row.getAmount(), groupId);
        }

        return new ExpenseResponse(
                saved.getId(),
                saved.getPaidBy(),
                saved.getAmount(),
                saved.getSplitType(),
                shares
        );
    }

    private void validateGroupParticipants(UUID groupId, UUID paidBy, List<SplitInput> participants) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new NoSuchElementException("Group not found: " + groupId));
        Set<UUID> members = new HashSet<>(group.getMembers());
        if (!members.contains(paidBy)) {
            throw new IllegalArgumentException("Payer is not a member of this group");
        }
        for (SplitInput p : participants) {
            if (!members.contains(p.userId())) {
                throw new IllegalArgumentException("Participant " + p.userId() + " is not a member of this group");
            }
        }
    }

    private UUID currentUserId(UserDetails userDetails) {
        String email = userDetails.getUsername();
        return userRepository.findByEmail(email)
                .map(User::getId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
    }
}
