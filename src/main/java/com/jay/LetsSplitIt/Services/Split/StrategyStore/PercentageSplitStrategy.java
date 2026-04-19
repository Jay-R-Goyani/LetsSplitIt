package com.jay.LetsSplitIt.Services.Split.StrategyStore;


import com.jay.LetsSplitIt.Entities.Expense.SplitType;
import com.jay.LetsSplitIt.Dto.ExpenseShare;
import com.jay.LetsSplitIt.Dto.SplitInput;
import com.jay.LetsSplitIt.Services.Split.SplitStrategy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Component
public class PercentageSplitStrategy implements SplitStrategy {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    @Override
    public SplitType type() {
        return SplitType.PERCENTAGE;
    }

    @Override
    public void validate(BigDecimal totalAmount, List<SplitInput> inputs) {
        if (totalAmount == null || totalAmount.signum() <= 0) {
            throw new IllegalArgumentException("Total amount must be positive");
        }
        if (inputs == null || inputs.isEmpty()) {
            throw new IllegalArgumentException("At least one participant is required");
        }

        BigDecimal sumOfPercentage = BigDecimal.ZERO;
        for (SplitInput input : inputs) {
            if (input.value() == null || input.value().signum() < 0) {
                throw new IllegalArgumentException("Each participant must have a non-negative percentage");
            }
            sumOfPercentage = sumOfPercentage.add(input.value());
        }
        if (sumOfPercentage.compareTo(HUNDRED) != 0) {
            throw new IllegalArgumentException("Percentage total must be exactly 100, got " + sumOfPercentage);
        }
    }

    @Override
    public List<ExpenseShare> split(BigDecimal totalAmount, List<SplitInput> inputs) {
        int n = inputs.size();
        List<ExpenseShare> shares = new ArrayList<>(n);
        BigDecimal running = BigDecimal.ZERO;

        for (int i = 0; i < n - 1; i++) {
            SplitInput input = inputs.get(i);
            BigDecimal amount = input.value()
                    .multiply(totalAmount)
                    .divide(HUNDRED, 2, RoundingMode.HALF_UP);
            shares.add(new ExpenseShare(input.userId(), amount));
            running = running.add(amount);
        }
        // last participant absorbs rounding remainder so totals match exactly
        BigDecimal remainder = totalAmount.subtract(running);
        shares.add(new ExpenseShare(inputs.get(n - 1).userId(), remainder));
        return shares;
    }
}
