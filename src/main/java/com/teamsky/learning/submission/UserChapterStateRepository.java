package com.teamsky.learning.submission;

import com.teamsky.learning.submission.entity.UserChapterState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserChapterStateRepository extends JpaRepository<UserChapterState, Long> {

    Optional<UserChapterState> findByUser_IdAndChapter_Id(Long userId, Long chapterId);

    @Query("""
            SELECT ucs.lastSkippedProblem.id FROM UserChapterState ucs
            WHERE ucs.user.id = :userId
            AND ucs.chapter.id = :chapterId
            """)
    Optional<Long> findLastSkippedProblemId(
            @Param("userId") Long userId,
            @Param("chapterId") Long chapterId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            value = """
                    INSERT INTO user_chapter_state (
                        user_id, chapter_id, last_skipped_problem_id, created_at, updated_at
                    )
                    VALUES (:userId, :chapterId, :problemId, NOW(6), NOW(6))
                    ON DUPLICATE KEY UPDATE
                        last_skipped_problem_id = VALUES(last_skipped_problem_id),
                        updated_at = NOW(6)
                    """,
            nativeQuery = true
    )
    int upsertLastSkippedProblem(
            @Param("userId") Long userId,
            @Param("chapterId") Long chapterId,
            @Param("problemId") Long problemId);
}
