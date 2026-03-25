package com.teamsky.learning.bookmark.presentation.response;

import com.teamsky.learning.bookmark.domain.Bookmark;
import com.teamsky.learning.problem.domain.Difficulty;
import com.teamsky.learning.problem.domain.ProblemType;

import java.time.LocalDateTime;

public record BookmarkResponse(
        Long bookmarkId,
        Long problemId,
        String problemContent,
        ProblemType problemType,
        Difficulty difficulty,
        String chapterName,
        LocalDateTime bookmarkedAt
) {
    public static BookmarkResponse of(Bookmark bookmark) {
        return new BookmarkResponse(
                bookmark.getId(),
                bookmark.getProblem().getId(),
                bookmark.getProblem().getContent(),
                bookmark.getProblem().getProblemType(),
                bookmark.getProblem().getDifficulty(),
                bookmark.getProblem().getChapter().getName(),
                bookmark.getCreatedAt()
        );
    }
}

