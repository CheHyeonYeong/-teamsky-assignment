package com.teamsky.learning.problem.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "choices")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Choice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "problem_id", nullable = false)
    private Problem problem;

    @Column(nullable = false)
    private Integer choiceNumber;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Builder
    public Choice(Integer choiceNumber, String content) {
        this.choiceNumber = choiceNumber;
        this.content = content;
    }

    void setProblem(Problem problem) {
        this.problem = problem;
    }
}

