package com.teamsky.learning.submission.request;

import com.teamsky.learning.problem.entity.ProblemType;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record SubmitRequest(
        @NotNull(message = "problemId is required")
        Long problemId,

        @NotNull(message = "userId is required")
        Long userId,

        @NotNull(message = "answerType is required")
        ProblemType answerType,

        List<Integer> multipleChoiceAnswers,

        String subjectiveAnswer,

        Long timeSpentSeconds,

        Boolean hintUsed
) {
    public String getUserAnswerAsString() {
        if (answerType == ProblemType.MULTIPLE_CHOICE && multipleChoiceAnswers != null) {
            return multipleChoiceAnswers.stream()
                    .sorted()
                    .map(String::valueOf)
                    .reduce((a, b) -> a + "," + b)
                    .orElse("");
        }
        return subjectiveAnswer != null ? subjectiveAnswer : "";
    }
}
