package com.jay.LetsSplitIt.Dto;

import java.math.BigDecimal;
import java.util.UUID;

public record SettleResponse(
        UUID payerId,
        UUID receiverId,
        UUID groupId,
        BigDecimal amountSettled,
        Scope scope
) {
    public enum Scope {
        FRIEND, GROUP, FULL
    }
}