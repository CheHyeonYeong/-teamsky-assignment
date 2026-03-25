package com.teamsky.learning.submission;

import com.teamsky.learning.submission.entity.AnswerStatus;
import com.teamsky.learning.submission.entity.Submission;
import com.teamsky.learning.submission.response.SubmissionHistoryResponse;
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

    Optional<Submission> findFirstByUser_IdAndProblem_IdOrderByCreatedAtDescIdDesc(Long userId, Long problemId);

    @Query(
            value = """
                    SELECT new com.teamsky.learning.submission.response.SubmissionHistoryResponse(
                        s.id,
                        p.id,
                        p.content,
                        s.answerStatus,
                        s.createdAt
                    )
                    FROM Submission s
                    JOIN s.problem p
                    WHERE s.user.id = :userId
                    AND p.chapter.id = :chapterId
                    ORDER BY s.createdAt DESC
                    """,
            countQuery = """
                    SELECT COUNT(s.id)
                    FROM Submission s
                    JOIN s.problem p
                    WHERE s.user.id = :userId
                    AND p.chapter.id = :chapterId
                    """
    )
    Page<SubmissionHistoryResponse> findHistoryResponsesByUserIdAndChapterId(
            @Param("userId") Long userId,
            @Param("chapterId") Long chapterId,
            Pageable pageable);

    @Query(
            value = """
                    SELECT new com.teamsky.learning.submission.response.SubmissionHistoryResponse(
                        s.id,
                        p.id,
                        p.content,
                        s.answerStatus,
                        s.createdAt
                    )
                    FROM Submission s
                    JOIN s.problem p
                    WHERE s.user.id = :userId
                    AND s.answerStatus IN :statuses
                    ORDER BY s.createdAt DESC
                    """,
            countQuery = """
                    SELECT COUNT(s.id)
                    FROM Submission s
                    WHERE s.user.id = :userId
                    AND s.answerStatus IN :statuses
                    """
    )
    Page<SubmissionHistoryResponse> findWrongSubmissionResponsesByUserId(
            @Param("userId") Long userId,
            @Param("statuses") List<AnswerStatus> statuses,
            Pageable pageable);
}

