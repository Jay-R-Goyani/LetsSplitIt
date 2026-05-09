package com.jay.LetsSplitIt.Dto;

import com.jay.LetsSplitIt.Entities.Expense;

import java.util.List;
import java.util.UUID;

public record ExpenseCreatedEvent(
        UUID expenseId,
        UUID paidBy,
        Expense.SplitType splitType,
        java.math.BigDecimal totalAmount,
        List<ExpenseShare> shares
) {
}