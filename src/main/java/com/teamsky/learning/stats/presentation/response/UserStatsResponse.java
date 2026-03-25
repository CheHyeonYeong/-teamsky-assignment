package com.teamsky.learning.stats.presentation.response;

public record UserStatsResponse(
        Long userId,
        Long totalSubmissions,
        Long correctSubmissions,
        Integer correctRate
) {
}

