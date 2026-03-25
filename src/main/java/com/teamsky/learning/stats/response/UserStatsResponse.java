package com.teamsky.learning.stats.response;

public record UserStatsResponse(
        Long userId,
        Long totalSubmissions,
        Long correctSubmissions,
        Integer correctRate
) {
}

