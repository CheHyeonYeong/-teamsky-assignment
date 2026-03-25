package com.teamsky.learning.problem;

import com.teamsky.learning.problem.entity.Difficulty;
import com.teamsky.learning.problem.entity.Problem;
import com.teamsky.learning.submission.entity.AnswerStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProblemRepository extends JpaRepository<Problem, Long> {

    @Query("SELECT p FROM Problem p WHERE p.chapter.id = :chapterId")
    List<Problem> findByChapterId(@Param("chapterId") Long chapterId);

    @Query("SELECT p FROM Problem p WHERE p.chapter.id = :chapterId AND p.difficulty = :difficulty")
    List<Problem> findByChapterIdAndDifficulty(
            @Param("chapterId") Long chapterId,
            @Param("difficulty") Difficulty difficulty);

    @Query("""
            SELECT p FROM Problem p
            WHERE p.chapter.id = :chapterId
            AND p.id NOT IN :excludedIds
            """)
    List<Problem> findByChapterIdExcluding(
            @Param("chapterId") Long chapterId,
            @Param("excludedIds") List<Long> excludedIds);

    @Query("""
            SELECT p FROM Problem p
            WHERE p.chapter.id = :chapterId
            AND p.difficulty = :difficulty
            AND p.id NOT IN :excludedIds
            """)
    List<Problem> findByChapterIdAndDifficultyExcluding(
            @Param("chapterId") Long chapterId,
            @Param("difficulty") Difficulty difficulty,
            @Param("excludedIds") List<Long> excludedIds);

    @Query("""
            SELECT p.id FROM Problem p
            WHERE p.chapter.id = :chapterId
            AND (:difficulty IS NULL OR p.difficulty = :difficulty)
            AND (:lastSkippedProblemId IS NULL OR p.id <> :lastSkippedProblemId)
            AND NOT EXISTS (
                SELECT s.id FROM Submission s
                WHERE s.user.id = :userId
                AND s.problem.id = p.id
            )
            ORDER BY function('RAND')
            """)
    List<Long> findRandomAvailableProblemIds(
            @Param("chapterId") Long chapterId,
            @Param("userId") Long userId,
            @Param("difficulty") Difficulty difficulty,
            @Param("lastSkippedProblemId") Long lastSkippedProblemId,
            Pageable pageable);

    @Query("""
            SELECT DISTINCT s.problem.id FROM Submission s
            WHERE s.user.id = :userId
            AND s.problem.chapter.id = :chapterId
            AND s.answerStatus IN :statuses
            ORDER BY function('RAND')
            """)
    List<Long> findRandomWrongProblemIdsByUserIdAndChapterId(
            @Param("userId") Long userId,
            @Param("chapterId") Long chapterId,
            @Param("statuses") List<AnswerStatus> statuses,
            Pageable pageable);
}
