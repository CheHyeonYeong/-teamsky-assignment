package com.teamsky.learning.problem.presentation.request;

import com.teamsky.learning.problem.domain.Difficulty;
import jakarta.validation.constraints.NotNull;

public record RandomProblemRequest(
        @NotNull(message = "chapterId is required")
        Long chapterId,

        @NotNull(message = "userId is required")
        Long userId,

        Difficulty difficulty
) {
}

