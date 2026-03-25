package com.teamsky.learning.stats.entity;

import com.teamsky.learning.problem.entity.Problem;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "problem_stats")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProblemStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "problem_id", nullable = false, unique = true)
    private Problem problem;

    @Column(nullable = false)
    private Long totalCount;

    @Column(nullable = false)
    private Long correctCount;

    @Builder
    public ProblemStats(Problem problem) {
        this.problem = problem;
        this.totalCount = 0L;
        this.correctCount = 0L;
    }

    public void incrementTotal() {
        this.totalCount++;
    }

    public void incrementCorrect() {
        this.correctCount++;
    }

    public Integer calculateCorrectRate() {
        if (totalCount < 30) {
            return null;
        }
        double rate = (double) correctCount / totalCount * 100;
        return (int) Math.round(rate);
    }
}
