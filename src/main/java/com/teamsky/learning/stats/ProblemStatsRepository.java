package com.teamsky.learning.stats;

import com.teamsky.learning.stats.entity.ProblemStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProblemStatsRepository extends JpaRepository<ProblemStats, Long> {

    Optional<ProblemStats> findByProblemId(Long problemId);

    @Query("""
            SELECT ps FROM ProblemStats ps
            WHERE ps.problem.id = :problemId
            """)
    Optional<ProblemStats> findByProblemIdWithLock(@Param("problemId") Long problemId);
}
