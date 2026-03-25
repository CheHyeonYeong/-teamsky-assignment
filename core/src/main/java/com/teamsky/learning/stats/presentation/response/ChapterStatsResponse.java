package com.teamsky.learning.stats.presentation.response;

public record ChapterStatsResponse(
        Long chapterId,
        Long totalSubmissions,
        Long correctSubmissions,
        Integer correctRate
) {
}

