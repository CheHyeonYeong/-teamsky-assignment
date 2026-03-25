package com.teamsky.learning.bookmark.application;

import com.teamsky.learning.bookmark.domain.Bookmark;
import com.teamsky.learning.bookmark.infrastructure.BookmarkRepository;
import com.teamsky.learning.bookmark.presentation.request.BookmarkRequest;
import com.teamsky.learning.bookmark.presentation.response.BookmarkResponse;
import com.teamsky.learning.chapter.domain.Chapter;
import com.teamsky.learning.problem.application.ProblemService;
import com.teamsky.learning.problem.domain.Difficulty;
import com.teamsky.learning.problem.domain.Problem;
import com.teamsky.learning.problem.domain.ProblemType;
import com.teamsky.learning.shared.exception.BusinessException;
import com.teamsky.learning.shared.exception.ErrorCode;
import com.teamsky.learning.user.application.UserService;
import com.teamsky.learning.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookmarkService tests")
class BookmarkServiceTest {

    @InjectMocks
    private BookmarkService bookmarkService;

    @Mock
    private BookmarkRepository bookmarkRepository;

    @Mock
    private UserService userService;

    @Mock
    private ProblemService problemService;

    private User testUser;
    private Chapter testChapter;
    private Problem testProblem;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .name("Test User")
                .email("test@example.com")
                .build();

        testChapter = Chapter.builder()
                .name("Test Chapter")
                .description("Description")
                .orderNum(1)
                .build();

        testProblem = Problem.builder()
                .chapter(testChapter)
                .content("Test problem")
                .problemType(ProblemType.MULTIPLE_CHOICE)
                .difficulty(Difficulty.MEDIUM)
                .explanation("Explanation")
                .build();
    }

    @Nested
    @DisplayName("Add bookmark")
    class AddBookmark {

        @Test
        @DisplayName("adds bookmark successfully")
        void shouldAddBookmarkSuccessfully() {
            BookmarkRequest request = new BookmarkRequest(1L, 1L);
            Bookmark bookmark = Bookmark.builder()
                    .user(testUser)
                    .problem(testProblem)
                    .build();

            doNothing().when(userService).validateUserExists(1L);
            given(problemService.findById(1L)).willReturn(testProblem);
            given(bookmarkRepository.insertIgnore(1L, 1L)).willReturn(1);
            given(bookmarkRepository.findByUserIdAndProblemIdWithProblem(1L, 1L)).willReturn(Optional.of(bookmark));

            BookmarkResponse response = bookmarkService.addBookmark(request);

            assertThat(response.problemId()).isEqualTo(testProblem.getId());
        }

        @Test
        @DisplayName("throws when problem is already bookmarked")
        void shouldThrowExceptionWhenAlreadyBookmarked() {
            BookmarkRequest request = new BookmarkRequest(1L, 1L);

            doNothing().when(userService).validateUserExists(1L);
            given(problemService.findById(1L)).willReturn(testProblem);
            given(bookmarkRepository.insertIgnore(1L, 1L)).willReturn(0);

            assertThatThrownBy(() -> bookmarkService.addBookmark(request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.BOOKMARK_ALREADY_EXISTS);
        }
    }

    @Nested
    @DisplayName("Remove bookmark")
    class RemoveBookmark {

        @Test
        @DisplayName("removes bookmark successfully")
        void shouldRemoveBookmarkSuccessfully() {
            Bookmark bookmark = Bookmark.builder()
                    .user(testUser)
                    .problem(testProblem)
                    .build();

            given(bookmarkRepository.findById(1L)).willReturn(Optional.of(bookmark));

            bookmarkService.removeBookmark(1L);

            verify(bookmarkRepository).delete(bookmark);
        }

        @Test
        @DisplayName("throws when bookmark is missing")
        void shouldThrowExceptionWhenBookmarkNotFound() {
            given(bookmarkRepository.findById(1L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> bookmarkService.removeBookmark(1L))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.BOOKMARK_NOT_FOUND);
        }
    }
}