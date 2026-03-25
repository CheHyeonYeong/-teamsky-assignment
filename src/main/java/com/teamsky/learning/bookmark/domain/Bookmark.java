package com.teamsky.learning.bookmark.domain;

import com.teamsky.learning.shared.domain.BaseTimeEntity;
import com.teamsky.learning.problem.domain.Problem;
import com.teamsky.learning.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "bookmarks",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "problem_id"})
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Bookmark extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "problem_id", nullable = false)
    private Problem problem;

    @Builder
    public Bookmark(User user, Problem problem) {
        this.user = user;
        this.problem = problem;
    }
}

