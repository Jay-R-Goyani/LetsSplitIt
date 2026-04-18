package com.jay.LetsSplitIt.Dto;

import java.time.Instant;

public record UserStats(
        long totalUsers,
        long totalAdmins,
        long totalRegularUsers,
        Instant generatedAt
) {}
