package com.teamsky.learning.submission.infrastructure;

import com.teamsky.learning.submission.domain.UserProblemState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserProblemStateRepository extends JpaRepository<UserProblemState, Long> {

    Optional<UserProblemState> findByUser_IdAndProblem_Id(Long userId, Long problemId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            value = """
                    INSERT INTO user_problem_state (
                        user_id, problem_id, last_submission_id, last_answer_status, solved, attempt_count, created_at, updated_at
                    )
                    VALUES (:userId, :problemId, :submissionId, :answerStatus, :solved, 1, NOW(6), NOW(6))
                    ON DUPLICATE KEY UPDATE
                        last_submission_id = VALUES(last_submission_id),
                        last_answer_status = VALUES(last_answer_status),
                        solved = VALUES(solved),
                        attempt_count = attempt_count + 1,
                        updated_at = NOW(6)
                    """,
            nativeQuery = true
    )
    int upsertSubmissionState(
            @Param("userId") Long userId,
            @Param("problemId") Long problemId,
            @Param("submissionId") Long submissionId,
            @Param("answerStatus") String answerStatus,
            @Param("solved") boolean solved);
}

