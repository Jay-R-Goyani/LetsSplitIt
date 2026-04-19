package com.jay.LetsSplitIt.Dto;

import java.math.BigDecimal;
import java.util.UUID;

public record FriendBalance(UUID friendId, BigDecimal amount, Direction direction) {

    public enum Direction {
        I_OWE, OWES_ME, SETTLED
    }
}
