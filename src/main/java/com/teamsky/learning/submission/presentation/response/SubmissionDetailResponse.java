package com.teamsky.learning.submission.presentation.response;

import com.teamsky.learning.problem.domain.Answer;
import com.teamsky.learning.problem.domain.Problem;
import com.teamsky.learning.submission.domain.AnswerStatus;
import com.teamsky.learning.submission.domain.Submission;

import java.util.List;

public record SubmissionDetailResponse(
        Long problemId,
        AnswerStatus answerStatus,
        String explanation,
        List<String> problemAnswers,
        String userAnswers,
        Integer answerCorrectRate
) {
    public static SubmissionDetailResponse of(Submission submission, Integer correctRate) {
        Problem problem = submission.getProblem();
        List<String> answers = problem.getAnswers().stream()
                .map(Answer::getAnswerValue)
                .toList();

        return new SubmissionDetailResponse(
                problem.getId(),
                submission.getAnswerStatus(),
                problem.getExplanation(),
                answers,
                submission.getUserAnswer(),
                correctRate
        );
    }
}

