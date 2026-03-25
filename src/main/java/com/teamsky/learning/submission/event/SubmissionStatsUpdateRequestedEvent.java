package com.teamsky.learning.submission.event;

public record SubmissionStatsUpdateRequestedEvent(
        Long userId,
        Long chapterId,
        Long problemId,
        boolean correct,
        boolean firstProblemAttempt
) {
}
