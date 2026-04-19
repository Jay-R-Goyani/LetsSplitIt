package com.jay.LetsSplitIt.Dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ExpenseShare(UUID userId, BigDecimal amountOwed) {
}
