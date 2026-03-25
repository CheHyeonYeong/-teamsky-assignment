package com.teamsky.learning.chapter;

import com.teamsky.learning.chapter.entity.Chapter;
import com.teamsky.learning.common.exception.BusinessException;
import com.teamsky.learning.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChapterService {

    private final ChapterRepository chapterRepository;

    public Chapter findById(Long chapterId) {
        return chapterRepository.findById(chapterId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAPTER_NOT_FOUND));
    }

    public void validateChapterExists(Long chapterId) {
        if (!chapterRepository.existsById(chapterId)) {
            throw new BusinessException(ErrorCode.CHAPTER_NOT_FOUND);
        }
    }
}
