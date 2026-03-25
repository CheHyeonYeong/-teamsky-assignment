package com.teamsky.learning.bookmark;

import com.teamsky.learning.bookmark.entity.Bookmark;
import com.teamsky.learning.bookmark.request.BookmarkRequest;
import com.teamsky.learning.bookmark.response.BookmarkResponse;
import com.teamsky.learning.common.exception.BusinessException;
import com.teamsky.learning.common.exception.ErrorCode;
import com.teamsky.learning.problem.ProblemService;
import com.teamsky.learning.problem.entity.Problem;
import com.teamsky.learning.user.UserService;
import com.teamsky.learning.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookmarkService {

    private final BookmarkRepository bookmarkRepository;
    private final UserService userService;
    private final ProblemService problemService;

    @Transactional
    public BookmarkResponse addBookmark(BookmarkRequest request) {
        User user = userService.findById(request.userId());
        Problem problem = problemService.findById(request.problemId());
        Bookmark bookmark = Bookmark.builder()
                .user(user)
                .problem(problem)
                .build();

        try {
            bookmarkRepository.saveAndFlush(bookmark);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.BOOKMARK_ALREADY_EXISTS);
        }

        return BookmarkResponse.of(bookmark);
    }

    public Page<BookmarkResponse> getBookmarks(Long userId, Pageable pageable) {
        userService.validateUserExists(userId);

        return bookmarkRepository.findByUserIdWithProblem(userId, pageable)
                .map(BookmarkResponse::of);
    }

    @Transactional
    public void removeBookmark(Long bookmarkId) {
        Bookmark bookmark = bookmarkRepository.findById(bookmarkId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOOKMARK_NOT_FOUND));

        bookmarkRepository.delete(bookmark);
    }
}
