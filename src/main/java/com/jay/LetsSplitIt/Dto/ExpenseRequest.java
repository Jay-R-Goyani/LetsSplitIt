package com.jay.LetsSplitIt.Dto;

import com.jay.LetsSplitIt.Entities.Expense.Category;
import com.jay.LetsSplitIt.Entities.Expense.SplitType;

import java.math.BigDecimal;
import java.util.List;

public record ExpenseRequest(
        String title,
        Category category,
        String description,
        BigDecimal amount,
        SplitType splitType,
        List<SplitInput> participants
) {
}