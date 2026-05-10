package com.jay.LetsSplitIt.Dto;

import com.jay.LetsSplitIt.Entities.Expense;

import java.util.List;
import java.util.UUID;

public record ExpenseCreatedEvent(
        UUID expenseId,
        UUID paidBy,
        String title,
        Expense.Category category,
        String description,
        Expense.SplitType splitType,
        java.math.BigDecimal totalAmount,
        List<ExpenseShare> shares
) {
}