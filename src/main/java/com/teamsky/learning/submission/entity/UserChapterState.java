package com.teamsky.learning.submission.entity;

import com.teamsky.learning.chapter.entity.Chapter;
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
        name = "user_chapter_state",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_chapter_state_user_chapter",
                        columnNames = {"user_id", "chapter_id"}
                )
        },
        indexes = {
                @Index(name = "idx_user_chapter_state_user_chapter", columnList = "user_id, chapter_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserChapterState extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chapter_id", nullable = false)
    private Chapter chapter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_skipped_problem_id", nullable = false)
    private Problem lastSkippedProblem;

    @Builder
    public UserChapterState(User user, Chapter chapter, Problem lastSkippedProblem) {
        this.user = user;
        this.chapter = chapter;
        this.lastSkippedProblem = lastSkippedProblem;
    }

    public static UserChapterState create(User user, Chapter chapter, Problem lastSkippedProblem) {
        return UserChapterState.builder()
                .user(user)
                .chapter(chapter)
                .lastSkippedProblem(lastSkippedProblem)
                .build();
    }

    public void updateLastSkippedProblem(Problem problem) {
        this.lastSkippedProblem = problem;
    }
}
