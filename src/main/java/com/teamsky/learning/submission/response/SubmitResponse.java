package com.teamsky.learning.submission.response;

import com.teamsky.learning.problem.entity.Answer;
import com.teamsky.learning.problem.entity.Problem;
import com.teamsky.learning.submission.entity.AnswerStatus;
import com.teamsky.learning.submission.entity.Submission;

import java.util.List;

public record SubmitResponse(
        Long problemId,
        AnswerStatus answerStatus,
        String explanation,
        List<String> problemAnswers
) {
    public static SubmitResponse of(Submission submission) {
        Problem problem = submission.getProblem();
        List<String> answers = problem.getAnswers().stream()
                .map(Answer::getAnswerValue)
                .toList();

        return new SubmitResponse(
                problem.getId(),
                submission.getAnswerStatus(),
                problem.getExplanation(),
                answers
        );
    }
}
