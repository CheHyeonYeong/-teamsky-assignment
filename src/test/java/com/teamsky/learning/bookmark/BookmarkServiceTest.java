package com.teamsky.learning.bookmark;

import com.teamsky.learning.bookmark.entity.Bookmark;
import com.teamsky.learning.bookmark.request.BookmarkRequest;
import com.teamsky.learning.bookmark.response.BookmarkResponse;
import com.teamsky.learning.chapter.entity.Chapter;
import com.teamsky.learning.common.exception.BusinessException;
import com.teamsky.learning.common.exception.ErrorCode;
import com.teamsky.learning.problem.ProblemService;
import com.teamsky.learning.problem.entity.Difficulty;
import com.teamsky.learning.problem.entity.Problem;
import com.teamsky.learning.problem.entity.ProblemType;
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
@DisplayName("BookmarkService 테스트")
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
                .name("테스트 사용자")
                .email("test@example.com")
                .build();

        testChapter = Chapter.builder()
                .name("테스트 단원")
                .description("설명")
                .orderNum(1)
                .build();

        testProblem = Problem.builder()
                .chapter(testChapter)
                .content("테스트 문제")
                .problemType(ProblemType.MULTIPLE_CHOICE)
                .difficulty(Difficulty.MEDIUM)
                .explanation("해설")
                .build();
    }

    @Nested
    @DisplayName("북마크 추가")
    class AddBookmark {

        @Test
        @DisplayName("북마크 추가 성공")
        void shouldAddBookmarkSuccessfully() {
            // given
            BookmarkRequest request = new BookmarkRequest(1L, 1L);
            Bookmark bookmark = Bookmark.builder()
                    .user(testUser)
                    .problem(testProblem)
                    .build();

            doNothing().when(userService).validateUserExists(1L);
            given(problemService.findById(1L)).willReturn(testProblem);
            given(bookmarkRepository.insertIgnore(1L, 1L)).willReturn(1);
            given(bookmarkRepository.findByUserIdAndProblemIdWithProblem(1L, 1L)).willReturn(Optional.of(bookmark));

            // when
            BookmarkResponse response = bookmarkService.addBookmark(request);

            // then
            assertThat(response.problemId()).isEqualTo(testProblem.getId());
        }

        @Test
        @DisplayName("이미 북마크한 문제면 예외 발생")
        void shouldThrowExceptionWhenAlreadyBookmarked() {
            // given
            BookmarkRequest request = new BookmarkRequest(1L, 1L);

            doNothing().when(userService).validateUserExists(1L);
            given(problemService.findById(1L)).willReturn(testProblem);
            given(bookmarkRepository.insertIgnore(1L, 1L)).willReturn(0);

            // when & then
            assertThatThrownBy(() -> bookmarkService.addBookmark(request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.BOOKMARK_ALREADY_EXISTS);
        }
    }

    @Nested
    @DisplayName("북마크 삭제")
    class RemoveBookmark {

        @Test
        @DisplayName("북마크 삭제 성공")
        void shouldRemoveBookmarkSuccessfully() {
            // given
            Bookmark bookmark = Bookmark.builder()
                    .user(testUser)
                    .problem(testProblem)
                    .build();

            given(bookmarkRepository.findById(1L)).willReturn(Optional.of(bookmark));

            // when
            bookmarkService.removeBookmark(1L);

            // then
            verify(bookmarkRepository).delete(bookmark);
        }

        @Test
        @DisplayName("존재하지 않는 북마크 삭제 시 예외 발생")
        void shouldThrowExceptionWhenBookmarkNotFound() {
            // given
            given(bookmarkRepository.findById(1L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> bookmarkService.removeBookmark(1L))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.BOOKMARK_NOT_FOUND);
        }
    }
}
