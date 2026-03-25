package com.teamsky.learning.problem.response;

import com.teamsky.learning.problem.entity.Difficulty;
import com.teamsky.learning.problem.entity.Problem;
import com.teamsky.learning.problem.entity.ProblemType;

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
