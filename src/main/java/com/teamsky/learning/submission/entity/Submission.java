package com.teamsky.learning.submission.entity;

import com.teamsky.learning.common.entity.BaseTimeEntity;
import com.teamsky.learning.problem.entity.Problem;
import com.teamsky.learning.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "submissions",
        indexes = {
                @Index(name = "idx_submission_user_problem", columnList = "user_id, problem_id"),
                @Index(name = "idx_submission_user_created", columnList = "user_id, created_at"),
                @Index(name = "idx_submission_user_status_created", columnList = "user_id, answer_status, created_at")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Submission extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "problem_id", nullable = false)
    private Problem problem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AnswerStatus answerStatus;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String userAnswer;

    @Column
    private Long timeSpentSeconds;

    @Column(nullable = false)
    private Boolean hintUsed;

    @Builder
    public Submission(User user, Problem problem, AnswerStatus answerStatus,
                      String userAnswer, Long timeSpentSeconds, Boolean hintUsed) {
        this.user = user;
        this.problem = problem;
        this.answerStatus = answerStatus;
        this.userAnswer = userAnswer;
        this.timeSpentSeconds = timeSpentSeconds;
        this.hintUsed = hintUsed != null ? hintUsed : false;
    }

    public boolean isCorrect() {
        return this.answerStatus == AnswerStatus.CORRECT;
    }

    public boolean isWrong() {
        return this.answerStatus == AnswerStatus.WRONG;
    }

    public boolean isPartial() {
        return this.answerStatus == AnswerStatus.PARTIAL;
    }
}

