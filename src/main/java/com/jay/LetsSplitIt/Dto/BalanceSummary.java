package com.jay.LetsSplitIt.Dto;

import java.math.BigDecimal;

public record BalanceSummary(BigDecimal youOwe, BigDecimal youAreOwed, BigDecimal net) {
}
