package com.jay.LetsSplitIt.Services.Split;

import com.jay.LetsSplitIt.Dto.ExpenseShare;
import com.jay.LetsSplitIt.Dto.SplitInput;
import com.jay.LetsSplitIt.Entities.Expense.SplitType;

import java.math.BigDecimal;
import java.util.List;

public interface SplitStrategy {

    SplitType type();

    void validate(BigDecimal totalAmount, List<SplitInput> inputs);

    List<ExpenseShare> split(BigDecimal totalAmount, List<SplitInput> inputs);
}
