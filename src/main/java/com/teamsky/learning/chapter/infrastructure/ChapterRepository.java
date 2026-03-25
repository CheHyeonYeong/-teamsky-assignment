package com.teamsky.learning.chapter.infrastructure;

import com.teamsky.learning.chapter.domain.Chapter;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChapterRepository extends JpaRepository<Chapter, Long> {
}

