package com.teamsky.learning.submission.response;

import com.teamsky.learning.problem.entity.Answer;
import com.teamsky.learning.problem.entity.Problem;
import com.teamsky.learning.submission.entity.AnswerStatus;
import com.teamsky.learning.submission.entity.Submission;

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
