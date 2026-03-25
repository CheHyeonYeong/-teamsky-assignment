package com.teamsky.learning.submission;

import com.teamsky.learning.submission.entity.UserChapterState;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
