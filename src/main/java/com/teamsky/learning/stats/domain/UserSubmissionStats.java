package com.teamsky.learning.stats.domain;

import com.teamsky.learning.shared.domain.BaseTimeEntity;
import com.teamsky.learning.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "user_submission_stats",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_submission_stats_user", columnNames = "user_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserSubmissionStats extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private long totalSubmissions;

    @Column(nullable = false)
    private long correctSubmissions;

    @Builder
    public UserSubmissionStats(User user, long totalSubmissions, long correctSubmissions) {
        this.user = user;
        this.totalSubmissions = totalSubmissions;
        this.correctSubmissions = correctSubmissions;
    }
}

