package com.teamsky.learning.chapter;

import com.teamsky.learning.chapter.entity.Chapter;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChapterRepository extends JpaRepository<Chapter, Long> {
}

