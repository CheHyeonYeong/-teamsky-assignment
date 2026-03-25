package com.teamsky.learning.submission.response;

import com.teamsky.learning.submission.entity.AnswerStatus;
import com.teamsky.learning.submission.entity.Submission;

import java.time.LocalDateTime;

public record SubmissionHistoryResponse(
        Long submissionId,
        Long problemId,
        String problemContent,
        AnswerStatus answerStatus,
        LocalDateTime submittedAt
) {
    public static SubmissionHistoryResponse of(Submission submission) {
        return new SubmissionHistoryResponse(
                submission.getId(),
                submission.getProblem().getId(),
                submission.getProblem().getContent(),
                submission.getAnswerStatus(),
                submission.getCreatedAt()
        );
    }
}
