package com.teamsky.learning.submission.presentation.request;

import jakarta.validation.constraints.NotNull;

public record SkipRequest(
        @NotNull(message = "problemId is required")
        Long problemId,

        @NotNull(message = "userId is required")
        Long userId,

        @NotNull(message = "chapterId is required")
        Long chapterId
) {
}

