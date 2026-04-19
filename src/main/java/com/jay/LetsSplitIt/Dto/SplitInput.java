package com.jay.LetsSplitIt.Dto;

import java.math.BigDecimal;
import java.util.UUID;

public record SplitInput(UUID userId, BigDecimal value) {
}
