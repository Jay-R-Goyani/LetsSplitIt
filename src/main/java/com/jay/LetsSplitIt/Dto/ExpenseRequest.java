package com.jay.LetsSplitIt.Dto;

import com.jay.LetsSplitIt.Entities.Expense.SplitType;

import java.math.BigDecimal;
import java.util.List;

public record ExpenseRequest(BigDecimal amount, SplitType splitType, List<SplitInput> participants) {
}
