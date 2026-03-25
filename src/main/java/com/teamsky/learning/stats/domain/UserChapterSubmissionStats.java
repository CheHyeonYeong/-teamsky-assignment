package com.teamsky.learning.stats.domain;

import com.teamsky.learning.chapter.domain.Chapter;
import com.teamsky.learning.shared.domain.BaseTimeEntity;
import com.teamsky.learning.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "user_chapter_submission_stats",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_chapter_submission_stats_user_chapter",
                        columnNames = {"user_id", "chapter_id"}
                )
        },
        indexes = {
                @Index(name = "idx_user_chapter_submission_stats_user_chapter", columnList = "user_id, chapter_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserChapterSubmissionStats extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chapter_id", nullable = false)
    private Chapter chapter;

    @Column(nullable = false)
    private long totalSubmissions;

    @Column(nullable = false)
    private long correctSubmissions;

    @Builder
    public UserChapterSubmissionStats(User user, Chapter chapter, long totalSubmissions, long correctSubmissions) {
        this.user = user;
        this.chapter = chapter;
        this.totalSubmissions = totalSubmissions;
        this.correctSubmissions = correctSubmissions;
    }
}

