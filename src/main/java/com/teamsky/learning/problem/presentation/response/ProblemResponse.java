package com.teamsky.learning.problem.presentation.response;

import com.teamsky.learning.problem.domain.Difficulty;
import com.teamsky.learning.problem.domain.Problem;
import com.teamsky.learning.problem.domain.ProblemType;

import java.util.List;

public record ProblemResponse(
        Long problemId,
        String content,
        ProblemType problemType,
        Difficulty difficulty,
        List<String> choices,
        Integer answerCorrectRate
) {
    public static ProblemResponse of(Problem problem, Integer answerCorrectRate) {
        List<String> choiceContents = problem.getChoices().stream()
                .map(choice -> choice.getContent())
                .toList();

        return new ProblemResponse(
                problem.getId(),
                problem.getContent(),
                problem.getProblemType(),
                problem.getDifficulty(),
                choiceContents,
                answerCorrectRate
        );
    }
}

