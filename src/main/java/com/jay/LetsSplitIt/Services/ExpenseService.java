package com.jay.LetsSplitIt.Services;

import com.jay.LetsSplitIt.Dto.ExpenseRequest;
import com.jay.LetsSplitIt.Dto.ExpenseResponse;
import com.jay.LetsSplitIt.Entities.Expense;
import com.jay.LetsSplitIt.Entities.SharedExpense;
import com.jay.LetsSplitIt.Entities.User;
import com.jay.LetsSplitIt.Repository.ExpenseRepository;
import com.jay.LetsSplitIt.Repository.ShareExpenseRepository;
import com.jay.LetsSplitIt.Repository.UserRepository;
import com.jay.LetsSplitIt.Dto.ExpenseShare;
import com.jay.LetsSplitIt.Services.Split.SplitStrategy;
import com.jay.LetsSplitIt.Services.Split.SplitStrategyRegistry;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final ShareExpenseRepository shareExpenseRepository;
    private final UserRepository userRepository;
    private final SplitStrategyRegistry registry;
    private final BalanceService balanceService;

    ExpenseService(ExpenseRepository expenseRepository,
                   ShareExpenseRepository shareExpenseRepository,
                   UserRepository userRepository,
                   SplitStrategyRegistry registry,
                   BalanceService balanceService) {
        this.expenseRepository = expenseRepository;
        this.shareExpenseRepository = shareExpenseRepository;
        this.userRepository = userRepository;
        this.registry = registry;
        this.balanceService = balanceService;
    }

    @Transactional
    public ExpenseResponse createExpense(UserDetails userDetails, ExpenseRequest request) {
        SplitStrategy strategy = registry.get(request.splitType());
        strategy.validate(request.amount(), request.participants());
        List<ExpenseShare> shares = strategy.split(request.amount(), request.participants());

        UUID paidBy = currentUserId(userDetails);

        Expense expense = new Expense();
        expense.setPaidBy(paidBy);
        expense.setAmount(request.amount());
        expense.setSplitType(request.splitType());

        Expense saved = expenseRepository.save(expense);

        List<SharedExpense> sharedRows = shares.stream()
                .filter(share -> !share.userId().equals(paidBy))
                .map(share -> new SharedExpense(null, saved.getId(), share.userId(), share.amountOwed()))
                .toList();
        shareExpenseRepository.saveAll(sharedRows);

        for (SharedExpense row : sharedRows) {
            balanceService.applyDebt(row.getUserId(), paidBy, row.getAmount());
        }

        return new ExpenseResponse(
                saved.getId(),
                saved.getPaidBy(),
                saved.getAmount(),
                saved.getSplitType(),
                shares
        );
    }

    private UUID currentUserId(UserDetails userDetails) {
        String email = userDetails.getUsername();
        return userRepository.findByEmail(email)
                .map(User::getId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
    }
}
