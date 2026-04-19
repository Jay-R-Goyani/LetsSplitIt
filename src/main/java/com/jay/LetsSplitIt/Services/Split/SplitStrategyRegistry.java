package com.jay.LetsSplitIt.Services.Split;

import com.jay.LetsSplitIt.Entities.Expense.SplitType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class SplitStrategyRegistry {

    private final Map<SplitType, SplitStrategy> strategies;

    SplitStrategyRegistry(List<SplitStrategy> strategies) {
        this.strategies = strategies.stream()
                .collect(Collectors.toMap(SplitStrategy::type, s -> s));
    }

    public SplitStrategy get(SplitType type) {
        SplitStrategy strategy = strategies.get(type);
        if (strategy == null) {
            throw new IllegalArgumentException("No strategy for split type: " + type);
        }
        return strategy;
    }
}
