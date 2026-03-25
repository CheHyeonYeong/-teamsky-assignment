package com.teamsky.learning.bookmark.request;

import jakarta.validation.constraints.NotNull;

public record BookmarkRequest(
        @NotNull(message = "userId is required")
        Long userId,

        @NotNull(message = "problemId is required")
        Long problemId
) {
}
