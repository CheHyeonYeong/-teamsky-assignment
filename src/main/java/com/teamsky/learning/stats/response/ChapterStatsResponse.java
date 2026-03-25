package com.teamsky.learning.stats.response;

public record ChapterStatsResponse(
        Long chapterId,
        Long totalSubmissions,
        Long correctSubmissions,
        Integer correctRate
) {
}

