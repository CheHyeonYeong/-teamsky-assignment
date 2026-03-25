package com.teamsky.learning.stats.infrastructure;

import com.teamsky.learning.stats.domain.ProblemStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProblemStatsRepository extends JpaRepository<ProblemStats, Long> {

    Optional<ProblemStats> findByProblemId(Long problemId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @org.springframework.data.jpa.repository.Query("""
            UPDATE ProblemStats ps
            SET ps.totalCount = ps.totalCount + 1
            WHERE ps.problem.id = :problemId
            """)
    int incrementTotalCount(@Param("problemId") Long problemId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @org.springframework.data.jpa.repository.Query("""
            UPDATE ProblemStats ps
            SET ps.correctCount = ps.correctCount + 1
            WHERE ps.problem.id = :problemId
            """)
    int incrementCorrectCount(@Param("problemId") Long problemId);
}

