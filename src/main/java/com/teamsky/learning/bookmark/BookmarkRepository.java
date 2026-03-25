package com.teamsky.learning.bookmark;

import com.teamsky.learning.bookmark.entity.Bookmark;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            value = """
                    INSERT IGNORE INTO bookmarks (user_id, problem_id, created_at, updated_at)
                    VALUES (:userId, :problemId, NOW(6), NOW(6))
                    """,
            nativeQuery = true
    )
    int insertIgnore(@Param("userId") Long userId, @Param("problemId") Long problemId);

    @Query("""
            SELECT b FROM Bookmark b
            JOIN FETCH b.problem p
            JOIN FETCH p.chapter
            WHERE b.user.id = :userId
            ORDER BY b.createdAt DESC
            """)
    Page<Bookmark> findByUserIdWithProblem(@Param("userId") Long userId, Pageable pageable);

    @Query("""
            SELECT b FROM Bookmark b
            JOIN FETCH b.problem p
            JOIN FETCH p.chapter
            WHERE b.user.id = :userId
            AND p.id = :problemId
            """)
    Optional<Bookmark> findByUserIdAndProblemIdWithProblem(
            @Param("userId") Long userId,
            @Param("problemId") Long problemId);

    boolean existsByUserIdAndProblemId(Long userId, Long problemId);
}

