package com.teamsky.learning.stats;

import com.teamsky.learning.stats.entity.UserSubmissionStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserSubmissionStatsRepository extends JpaRepository<UserSubmissionStats, Long> {

    Optional<UserSubmissionStats> findByUser_Id(Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            value = """
                    INSERT INTO user_submission_stats (
                        user_id, total_submissions, correct_submissions, created_at, updated_at
                    )
                    VALUES (:userId, 1, :correctIncrement, NOW(6), NOW(6))
                    ON DUPLICATE KEY UPDATE
                        total_submissions = total_submissions + 1,
                        correct_submissions = correct_submissions + :correctIncrement,
                        updated_at = NOW(6)
                    """,
            nativeQuery = true
    )
    int upsertSubmissionStats(@Param("userId") Long userId, @Param("correctIncrement") int correctIncrement);
}
