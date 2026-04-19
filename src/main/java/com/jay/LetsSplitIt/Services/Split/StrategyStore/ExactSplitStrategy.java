package com.jay.LetsSplitIt.Services.Split.StrategyStore;

import com.jay.LetsSplitIt.Entities.Expense.SplitType;
import com.jay.LetsSplitIt.Dto.ExpenseShare;
import com.jay.LetsSplitIt.Dto.SplitInput;
import com.jay.LetsSplitIt.Services.Split.SplitStrategy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class ExactSplitStrategy implements SplitStrategy {

    @Override
    public SplitType type() {
        return SplitType.EXACT;
    }

    @Override
    public void validate(BigDecimal totalAmount, List<SplitInput> inputs) {
        if (totalAmount == null || totalAmount.signum() <= 0) {
            throw new IllegalArgumentException("Total amount must be positive");
        }
        if (inputs == null || inputs.isEmpty()) {
            throw new IllegalArgumentException("At least one participant is required");
        }

        BigDecimal sum = BigDecimal.ZERO;
        for (SplitInput input : inputs) {
            if (input.value() == null || input.value().signum() < 0) {
                throw new IllegalArgumentException("Each participant must have a non-negative amount");
            }
            sum = sum.add(input.value());
        }
        if (sum.compareTo(totalAmount) != 0) {
            throw new IllegalArgumentException("Sum of exact amounts (" + sum + ") must equal total (" + totalAmount + ")");
        }
    }

    @Override
    public List<ExpenseShare> split(BigDecimal totalAmount, List<SplitInput> inputs) {
        return inputs.stream()
                .map(input -> new ExpenseShare(input.userId(), input.value()))
                .toList();
    }
}
