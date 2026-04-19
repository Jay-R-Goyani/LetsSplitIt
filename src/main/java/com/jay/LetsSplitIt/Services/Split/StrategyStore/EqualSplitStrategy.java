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
public class EqualSplitStrategy implements SplitStrategy {

    @Override
    public SplitType type() {
        return SplitType.EQUAL;
    }

    @Override
    public void validate(BigDecimal totalAmount, List<SplitInput> inputs) {
        if (totalAmount == null || totalAmount.signum() <= 0) {
            throw new IllegalArgumentException("Total amount must be positive");
        }
        if (inputs == null || inputs.isEmpty()) {
            throw new IllegalArgumentException("At least one participant is required");
        }
    }

    @Override
    public List<ExpenseShare> split(BigDecimal totalAmount, List<SplitInput> inputs) {
        int n = inputs.size();
        BigDecimal perHead = totalAmount.divide(BigDecimal.valueOf(n), 2, RoundingMode.HALF_UP);

        List<ExpenseShare> shares = new ArrayList<>(n);
        BigDecimal running = BigDecimal.ZERO;
        for (int i = 0; i < n - 1; i++) {
            shares.add(new ExpenseShare(inputs.get(i).userId(), perHead));
            running = running.add(perHead);
        }
        // last participant absorbs any rounding remainder
        BigDecimal remainder = totalAmount.subtract(running);
        shares.add(new ExpenseShare(inputs.get(n - 1).userId(), remainder));
        return shares;
    }
}
