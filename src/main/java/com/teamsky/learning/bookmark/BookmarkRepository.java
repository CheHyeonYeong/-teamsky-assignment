package com.teamsky.learning.bookmark;

import com.teamsky.learning.bookmark.entity.Bookmark;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    @Query("""
            SELECT b FROM Bookmark b
            JOIN FETCH b.problem p
            JOIN FETCH p.chapter
            WHERE b.user.id = :userId
            ORDER BY b.createdAt DESC
            """)
    Page<Bookmark> findByUserIdWithProblem(@Param("userId") Long userId, Pageable pageable);

    Optional<Bookmark> findByUserIdAndProblemId(Long userId, Long problemId);

    boolean existsByUserIdAndProblemId(Long userId, Long problemId);
}
