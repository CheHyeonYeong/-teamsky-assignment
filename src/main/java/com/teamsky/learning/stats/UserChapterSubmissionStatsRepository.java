package com.teamsky.learning.stats;

import com.teamsky.learning.stats.entity.UserChapterSubmissionStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserChapterSubmissionStatsRepository extends JpaRepository<UserChapterSubmissionStats, Long> {

    Optional<UserChapterSubmissionStats> findByUser_IdAndChapter_Id(Long userId, Long chapterId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            value = """
                    INSERT INTO user_chapter_submission_stats (
                        user_id, chapter_id, total_submissions, correct_submissions, created_at, updated_at
                    )
                    VALUES (:userId, :chapterId, 1, :correctIncrement, NOW(6), NOW(6))
                    ON DUPLICATE KEY UPDATE
                        total_submissions = total_submissions + 1,
                        correct_submissions = correct_submissions + :correctIncrement,
                        updated_at = NOW(6)
                    """,
            nativeQuery = true
    )
    int upsertSubmissionStats(
            @Param("userId") Long userId,
            @Param("chapterId") Long chapterId,
            @Param("correctIncrement") int correctIncrement);
}
