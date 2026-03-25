package com.teamsky.learning.submission;

import com.teamsky.learning.submission.entity.AnswerStatus;
import com.teamsky.learning.submission.entity.Submission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    @Query("SELECT s.problem.id FROM Submission s WHERE s.user.id = :userId")
    List<Long> findProblemIdsByUserId(@Param("userId") Long userId);

    @Query("""
            SELECT s.problem.id FROM Submission s
            WHERE s.user.id = :userId
            AND s.problem.chapter.id = :chapterId
            AND s.answerStatus IN ('WRONG', 'PARTIAL')
            """)
    List<Long> findWrongProblemIdsByUserIdAndChapterId(
            @Param("userId") Long userId,
            @Param("chapterId") Long chapterId);

    Optional<Submission> findByUserIdAndProblemId(Long userId, Long problemId);

    @Query("""
            SELECT s FROM Submission s
            JOIN FETCH s.problem p
            JOIN FETCH p.chapter
            WHERE s.user.id = :userId
            AND p.chapter.id = :chapterId
            ORDER BY s.createdAt DESC
            """)
    Page<Submission> findByUserIdAndChapterId(
            @Param("userId") Long userId,
            @Param("chapterId") Long chapterId,
            Pageable pageable);

    @Query("""
            SELECT s FROM Submission s
            JOIN FETCH s.problem p
            JOIN FETCH p.chapter
            WHERE s.user.id = :userId
            AND s.answerStatus IN :statuses
            ORDER BY s.createdAt DESC
            """)
    Page<Submission> findByUserIdAndAnswerStatusIn(
            @Param("userId") Long userId,
            @Param("statuses") List<AnswerStatus> statuses,
            Pageable pageable);

    @Query("SELECT COUNT(s) FROM Submission s WHERE s.problem.id = :problemId")
    long countByProblemId(@Param("problemId") Long problemId);

    @Query("SELECT COUNT(s) FROM Submission s WHERE s.problem.id = :problemId AND s.answerStatus = 'CORRECT'")
    long countCorrectByProblemId(@Param("problemId") Long problemId);
}
