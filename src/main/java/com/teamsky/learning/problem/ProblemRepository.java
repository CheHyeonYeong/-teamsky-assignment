package com.teamsky.learning.problem;

import com.teamsky.learning.problem.entity.Difficulty;
import com.teamsky.learning.problem.entity.Problem;
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
}
