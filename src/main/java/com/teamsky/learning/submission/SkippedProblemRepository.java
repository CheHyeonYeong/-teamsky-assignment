package com.teamsky.learning.submission;

import com.teamsky.learning.submission.entity.SkippedProblem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SkippedProblemRepository extends JpaRepository<SkippedProblem, Long> {

    void deleteByUserIdAndChapterId(Long userId, Long chapterId);

    @Query("""
            SELECT sp.problem.id FROM SkippedProblem sp
            WHERE sp.user.id = :userId
            AND sp.chapter.id = :chapterId
            ORDER BY sp.skippedAt DESC
            LIMIT 1
            """)
    Optional<Long> findLastSkippedProblemId(
            @Param("userId") Long userId,
            @Param("chapterId") Long chapterId);

    @Query("""
            SELECT COUNT(sp) FROM SkippedProblem sp
            WHERE sp.user.id = :userId
            AND sp.chapter.id = :chapterId
            """)
    long countByUserIdAndChapterId(
            @Param("userId") Long userId,
            @Param("chapterId") Long chapterId);
}
