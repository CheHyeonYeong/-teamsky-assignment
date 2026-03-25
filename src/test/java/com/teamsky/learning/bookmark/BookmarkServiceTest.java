package com.teamsky.learning.bookmark;

import com.teamsky.learning.bookmark.entity.Bookmark;
import com.teamsky.learning.bookmark.BookmarkRepository;
import com.teamsky.learning.bookmark.request.BookmarkRequest;
import com.teamsky.learning.bookmark.response.BookmarkResponse;
import com.teamsky.learning.chapter.entity.Chapter;
import com.teamsky.learning.problem.ProblemService;
import com.teamsky.learning.problem.entity.Difficulty;
import com.teamsky.learning.problem.entity.Problem;
import com.teamsky.learning.problem.entity.ProblemType;
import com.teamsky.learning.common.exception.BusinessException;
import com.teamsky.learning.common.exception.ErrorCode;
import com.teamsky.learning.user.UserService;
import com.teamsky.learning.user.entity.User;
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

    private static final long TEST_USER_ID = 1L;
    private static final long TEST_PROBLEM_ID = 1L;
    private static final long TEST_BOOKMARK_ID = 1L;
    private static final String TEST_USER_NAME = "Test User";
    private static final String TEST_USER_EMAIL = "test@example.com";
    private static final String TEST_CHAPTER_NAME = "Test Chapter";
    private static final String TEST_CHAPTER_DESCRIPTION = "Description";
    private static final int TEST_CHAPTER_ORDER = 1;
    private static final String TEST_PROBLEM_CONTENT = "Test problem";
    private static final String TEST_PROBLEM_EXPLANATION = "Explanation";

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
                .name(TEST_USER_NAME)
                .email(TEST_USER_EMAIL)
                .build();

        testChapter = Chapter.builder()
                .name(TEST_CHAPTER_NAME)
                .description(TEST_CHAPTER_DESCRIPTION)
                .orderNum(TEST_CHAPTER_ORDER)
                .build();

        testProblem = Problem.builder()
                .chapter(testChapter)
                .content(TEST_PROBLEM_CONTENT)
                .problemType(ProblemType.MULTIPLE_CHOICE)
                .difficulty(Difficulty.MEDIUM)
                .explanation(TEST_PROBLEM_EXPLANATION)
                .build();
    }

    @Nested
    @DisplayName("Add bookmark")
    class AddBookmark {

        @Test
        @DisplayName("adds bookmark successfully")
        void shouldAddBookmarkSuccessfully() {
            BookmarkRequest request = new BookmarkRequest(TEST_USER_ID, TEST_PROBLEM_ID);
            Bookmark bookmark = Bookmark.builder()
                    .user(testUser)
                    .problem(testProblem)
                    .build();

            doNothing().when(userService).validateUserExists(TEST_USER_ID);
            given(problemService.findById(TEST_PROBLEM_ID)).willReturn(testProblem);
            given(bookmarkRepository.insertIgnore(TEST_USER_ID, TEST_PROBLEM_ID)).willReturn(1);
            given(bookmarkRepository.findByUserIdAndProblemIdWithProblem(TEST_USER_ID, TEST_PROBLEM_ID))
                    .willReturn(Optional.of(bookmark));

            BookmarkResponse response = bookmarkService.addBookmark(request);

            assertThat(response.problemId()).isEqualTo(testProblem.getId());
        }

        @Test
        @DisplayName("throws when problem is already bookmarked")
        void shouldThrowExceptionWhenAlreadyBookmarked() {
            BookmarkRequest request = new BookmarkRequest(TEST_USER_ID, TEST_PROBLEM_ID);

            doNothing().when(userService).validateUserExists(TEST_USER_ID);
            given(problemService.findById(TEST_PROBLEM_ID)).willReturn(testProblem);
            given(bookmarkRepository.insertIgnore(TEST_USER_ID, TEST_PROBLEM_ID)).willReturn(0);

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

            given(bookmarkRepository.findById(TEST_BOOKMARK_ID)).willReturn(Optional.of(bookmark));

            bookmarkService.removeBookmark(TEST_BOOKMARK_ID);

            verify(bookmarkRepository).delete(bookmark);
        }

        @Test
        @DisplayName("throws when bookmark is missing")
        void shouldThrowExceptionWhenBookmarkNotFound() {
            given(bookmarkRepository.findById(TEST_BOOKMARK_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> bookmarkService.removeBookmark(TEST_BOOKMARK_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.BOOKMARK_NOT_FOUND);
        }
    }
}
