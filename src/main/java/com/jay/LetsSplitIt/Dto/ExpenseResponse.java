package com.jay.LetsSplitIt.Dto;

import com.jay.LetsSplitIt.Entities.Expense.Category;
import com.jay.LetsSplitIt.Entities.Expense.SplitType;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ExpenseResponse(
        UUID id,
        UUID paidBy,
        String title,
        Category category,
        String description,
        BigDecimal amount,
        SplitType splitType,
        List<ExpenseShare> shares
) {
}