package com.teamsky.learning.problem;

import com.teamsky.learning.chapter.ChapterService;
import com.teamsky.learning.chapter.entity.Chapter;
import com.teamsky.learning.common.exception.BusinessException;
import com.teamsky.learning.common.exception.ErrorCode;
import com.teamsky.learning.problem.entity.*;
import com.teamsky.learning.problem.request.RandomProblemRequest;
import com.teamsky.learning.problem.response.ProblemResponse;
import com.teamsky.learning.stats.StatsService;
import com.teamsky.learning.submission.SubmissionRepository;
import com.teamsky.learning.submission.SkippedProblemRepository;
import com.teamsky.learning.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProblemService 테스트")
class ProblemServiceTest {

    @InjectMocks
    private ProblemService problemService;

    @Mock
    private ProblemRepository problemRepository;

    @Mock
    private SubmissionRepository submissionRepository;

    @Mock
    private SkippedProblemRepository skippedProblemRepository;

    @Mock
    private UserService userService;

    @Mock
    private ChapterService chapterService;

    @Mock
    private StatsService statsService;

    private Chapter testChapter;
    private Problem testProblem;

    @BeforeEach
    void setUp() {
        testChapter = Chapter.builder()
                .name("테스트 단원")
                .description("테스트 단원 설명")
                .orderNum(1)
                .build();

        testProblem = Problem.builder()
                .chapter(testChapter)
                .content("테스트 문제입니다.")
                .problemType(ProblemType.MULTIPLE_CHOICE)
                .difficulty(Difficulty.MEDIUM)
                .explanation("테스트 해설입니다.")
                .build();

        for (int i = 1; i <= 5; i++) {
            Choice choice = Choice.builder()
                    .choiceNumber(i)
                    .content("선택지 " + i)
                    .build();
            testProblem.addChoice(choice);
        }
    }

    @Nested
    @DisplayName("랜덤 문제 조회")
    class GetRandomProblem {

        @Test
        @DisplayName("풀지 않은 문제 중 랜덤으로 1개 반환")
        void shouldReturnRandomUnsolvedProblem() {
            // given
            RandomProblemRequest request = new RandomProblemRequest(1L, 1L, null);

            doNothing().when(userService).validateUserExists(1L);
            doNothing().when(chapterService).validateChapterExists(1L);
            given(submissionRepository.findProblemIdsByUserId(1L)).willReturn(List.of());
            given(skippedProblemRepository.findLastSkippedProblemId(1L, 1L)).willReturn(Optional.empty());
            given(problemRepository.findByChapterIdExcluding(eq(1L), anyList())).willReturn(List.of(testProblem));
            given(statsService.calculateCorrectRate(any())).willReturn(67);

            // when
            ProblemResponse response = problemService.getRandomProblem(request);

            // then
            assertThat(response).isNotNull();
            assertThat(response.content()).isEqualTo("테스트 문제입니다.");
            assertThat(response.choices()).hasSize(5);
            assertThat(response.answerCorrectRate()).isEqualTo(67);
        }

        @Test
        @DisplayName("더 이상 풀 문제가 없으면 NO_MORE_PROBLEMS 예외")
        void shouldThrowExceptionWhenNoMoreProblems() {
            // given
            RandomProblemRequest request = new RandomProblemRequest(1L, 1L, null);

            doNothing().when(userService).validateUserExists(1L);
            doNothing().when(chapterService).validateChapterExists(1L);
            given(submissionRepository.findProblemIdsByUserId(1L)).willReturn(List.of(1L, 2L, 3L));
            given(skippedProblemRepository.findLastSkippedProblemId(1L, 1L)).willReturn(Optional.empty());
            given(problemRepository.findByChapterIdExcluding(eq(1L), anyList())).willReturn(List.of());

            // when & then
            assertThatThrownBy(() -> problemService.getRandomProblem(request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.NO_MORE_PROBLEMS);
        }

        @Test
        @DisplayName("직전에 건너뛴 문제는 제외하고 조회")
        void shouldExcludeLastSkippedProblem() {
            // given
            RandomProblemRequest request = new RandomProblemRequest(1L, 1L, null);

            doNothing().when(userService).validateUserExists(1L);
            doNothing().when(chapterService).validateChapterExists(1L);
            given(submissionRepository.findProblemIdsByUserId(1L)).willReturn(List.of(1L));
            given(skippedProblemRepository.findLastSkippedProblemId(1L, 1L)).willReturn(Optional.of(2L));
            given(problemRepository.findByChapterIdExcluding(eq(1L), eq(List.of(1L, 2L)))).willReturn(List.of(testProblem));
            given(statsService.calculateCorrectRate(any())).willReturn(null);

            // when
            ProblemResponse response = problemService.getRandomProblem(request);

            // then
            assertThat(response).isNotNull();
        }
    }

    @Nested
    @DisplayName("난이도별 문제 필터링")
    class FilterByDifficulty {

        @Test
        @DisplayName("지정한 난이도의 문제만 조회")
        void shouldReturnProblemWithSpecifiedDifficulty() {
            // given
            Problem hardProblem = Problem.builder()
                    .chapter(testChapter)
                    .content("어려운 문제입니다.")
                    .problemType(ProblemType.MULTIPLE_CHOICE)
                    .difficulty(Difficulty.HIGH)
                    .explanation("어려운 문제 해설입니다.")
                    .build();

            RandomProblemRequest request = new RandomProblemRequest(1L, 1L, Difficulty.HIGH);

            doNothing().when(userService).validateUserExists(1L);
            doNothing().when(chapterService).validateChapterExists(1L);
            given(submissionRepository.findProblemIdsByUserId(1L)).willReturn(List.of());
            given(skippedProblemRepository.findLastSkippedProblemId(1L, 1L)).willReturn(Optional.empty());
            given(problemRepository.findByChapterIdAndDifficultyExcluding(eq(1L), eq(Difficulty.HIGH), anyList()))
                    .willReturn(List.of(hardProblem));
            given(statsService.calculateCorrectRate(any())).willReturn(45);

            // when
            ProblemResponse response = problemService.getRandomProblem(request);

            // then
            assertThat(response.difficulty()).isEqualTo(Difficulty.HIGH);
        }
    }

    @Nested
    @DisplayName("오답 문제 재풀이")
    class GetWrongProblems {

        @Test
        @DisplayName("틀린 문제 중 랜덤으로 1개 반환")
        void shouldReturnRandomWrongProblem() {
            // given
            doNothing().when(userService).validateUserExists(1L);
            doNothing().when(chapterService).validateChapterExists(1L);
            given(submissionRepository.findWrongProblemIdsByUserIdAndChapterId(1L, 1L))
                    .willReturn(List.of(1L, 2L));
            given(problemRepository.findById(anyLong())).willReturn(Optional.of(testProblem));
            given(statsService.calculateCorrectRate(any())).willReturn(50);

            // when
            ProblemResponse response = problemService.getRandomWrongProblem(1L, 1L);

            // then
            assertThat(response).isNotNull();
        }

        @Test
        @DisplayName("틀린 문제가 없으면 NO_MORE_PROBLEMS 예외")
        void shouldThrowExceptionWhenNoWrongProblems() {
            // given
            doNothing().when(userService).validateUserExists(1L);
            doNothing().when(chapterService).validateChapterExists(1L);
            given(submissionRepository.findWrongProblemIdsByUserIdAndChapterId(1L, 1L))
                    .willReturn(List.of());

            // when & then
            assertThatThrownBy(() -> problemService.getRandomWrongProblem(1L, 1L))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.NO_MORE_PROBLEMS);
        }
    }
}
