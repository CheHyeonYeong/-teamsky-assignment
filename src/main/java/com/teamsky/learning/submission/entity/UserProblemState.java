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
@Table(
        name = "user_problem_state",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_problem_state_user_problem",
                        columnNames = {"user_id", "problem_id"}
                )
        },
        indexes = {
                @Index(name = "idx_user_problem_state_user_problem", columnList = "user_id, problem_id"),
                @Index(name = "idx_user_problem_state_user_status", columnList = "user_id, last_answer_status")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserProblemState extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "problem_id", nullable = false)
    private Problem problem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_submission_id", nullable = false)
    private Submission lastSubmission;

    @Enumerated(EnumType.STRING)
    @Column(name = "last_answer_status", nullable = false, length = 20)
    private AnswerStatus lastAnswerStatus;

    @Column(nullable = false)
    private boolean solved;

    @Column(nullable = false)
    private long attemptCount;

    @Builder
    public UserProblemState(User user, Problem problem, Submission lastSubmission,
                            AnswerStatus lastAnswerStatus, boolean solved, long attemptCount) {
        this.user = user;
        this.problem = problem;
        this.lastSubmission = lastSubmission;
        this.lastAnswerStatus = lastAnswerStatus;
        this.solved = solved;
        this.attemptCount = attemptCount;
    }

    public static UserProblemState create(User user, Problem problem, Submission submission) {
        return UserProblemState.builder()
                .user(user)
                .problem(problem)
                .lastSubmission(submission)
                .lastAnswerStatus(submission.getAnswerStatus())
                .solved(submission.isCorrect())
                .attemptCount(1L)
                .build();
    }

    public void recordSubmission(Submission submission) {
        this.lastSubmission = submission;
        this.lastAnswerStatus = submission.getAnswerStatus();
        this.solved = submission.isCorrect();
        this.attemptCount++;
    }
}

