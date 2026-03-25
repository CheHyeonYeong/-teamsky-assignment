package com.teamsky.learning.submission.entity;

import com.teamsky.learning.chapter.entity.Chapter;
import com.teamsky.learning.problem.entity.Problem;
import com.teamsky.learning.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "skipped_problems",
        indexes = {
                @Index(name = "idx_skipped_user_chapter", columnList = "user_id, chapter_id, skipped_at DESC")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SkippedProblem {

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
    @JoinColumn(name = "chapter_id", nullable = false)
    private Chapter chapter;

    @Column(nullable = false)
    private LocalDateTime skippedAt;

    @Builder
    public SkippedProblem(User user, Problem problem, Chapter chapter) {
        this.user = user;
        this.problem = problem;
        this.chapter = chapter;
        this.skippedAt = LocalDateTime.now();
    }
}
