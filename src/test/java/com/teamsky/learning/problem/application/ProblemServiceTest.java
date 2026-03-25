package com.teamsky.learning.problem.application;

import com.teamsky.learning.chapter.application.ChapterService;
import com.teamsky.learning.chapter.domain.Chapter;
import com.teamsky.learning.shared.exception.BusinessException;
import com.teamsky.learning.shared.exception.ErrorCode;
import com.teamsky.learning.problem.domain.Choice;
import com.teamsky.learning.problem.domain.Difficulty;
import com.teamsky.learning.problem.domain.Problem;
import com.teamsky.learning.problem.domain.ProblemType;
import com.teamsky.learning.problem.infrastructure.ProblemRepository;
import com.teamsky.learning.problem.presentation.request.RandomProblemRequest;
import com.teamsky.learning.problem.presentation.response.ProblemResponse;
import com.teamsky.learning.stats.application.StatsService;
import com.teamsky.learning.submission.infrastructure.UserChapterStateRepository;
import com.teamsky.learning.user.application.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProblemService tests")
class ProblemServiceTest {

    @InjectMocks
    private ProblemService problemService;

    @Mock
    private ProblemRepository problemRepository;

    @Mock
    private UserChapterStateRepository userChapterStateRepository;

    @Mock
    private UserService userService;

    @Mock
    private ChapterService chapterService;

    @Mock
    private StatsService statsService;

    private Problem testProblem;

    @BeforeEach
    void setUp() {
        Chapter chapter = Chapter.builder()
                .name("Chapter 1")
                .description("Basic chapter")
                .orderNum(1)
                .build();
        ReflectionTestUtils.setField(chapter, "id", 1L);

        testProblem = Problem.builder()
                .chapter(chapter)
                .content("Test problem")
                .problemType(ProblemType.MULTIPLE_CHOICE)
                .difficulty(Difficulty.MEDIUM)
                .explanation("Explanation")
                .build();
        ReflectionTestUtils.setField(testProblem, "id", 1L);

        for (int i = 1; i <= 5; i++) {
            testProblem.addChoice(Choice.builder()
                    .choiceNumber(i)
                    .content("Choice " + i)
                    .build());
        }
    }

    @Nested
    @DisplayName("Random problem")
    class GetRandomProblem {

        @Test
        @DisplayName("returns one available problem")
        void shouldReturnRandomUnsolvedProblem() {
            RandomProblemRequest request = new RandomProblemRequest(1L, 1L, null);

            doNothing().when(userService).validateUserExists(1L);
            doNothing().when(chapterService).validateChapterExists(1L);
            given(userChapterStateRepository.findLastSkippedProblemId(1L, 1L)).willReturn(Optional.empty());
            given(problemRepository.countAvailableProblems(1L, 1L, null, null)).willReturn(1L);
            given(problemRepository.findAvailableProblemIds(eq(1L), eq(1L), isNull(), isNull(), any()))
                    .willReturn(List.of(1L));
            given(problemRepository.findById(1L)).willReturn(Optional.of(testProblem));
            given(statsService.calculateCorrectRate(1L)).willReturn(67);

            ProblemResponse response = problemService.getRandomProblem(request);

            assertThat(response.problemId()).isEqualTo(1L);
            assertThat(response.content()).isEqualTo("Test problem");
            assertThat(response.choices()).hasSize(5);
            assertThat(response.answerCorrectRate()).isEqualTo(67);
        }

        @Test
        @DisplayName("throws when there are no available problems")
        void shouldThrowExceptionWhenNoMoreProblems() {
            RandomProblemRequest request = new RandomProblemRequest(1L, 1L, null);

            doNothing().when(userService).validateUserExists(1L);
            doNothing().when(chapterService).validateChapterExists(1L);
            given(userChapterStateRepository.findLastSkippedProblemId(1L, 1L)).willReturn(Optional.empty());
            given(problemRepository.countAvailableProblems(1L, 1L, null, null)).willReturn(0L);

            assertThatThrownBy(() -> problemService.getRandomProblem(request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.NO_MORE_PROBLEMS);
        }

        @Test
        @DisplayName("excludes the last skipped problem in the chapter")
        void shouldExcludeLastSkippedProblem() {
            RandomProblemRequest request = new RandomProblemRequest(1L, 1L, null);

            doNothing().when(userService).validateUserExists(1L);
            doNothing().when(chapterService).validateChapterExists(1L);
            given(userChapterStateRepository.findLastSkippedProblemId(1L, 1L)).willReturn(Optional.of(2L));
            given(problemRepository.countAvailableProblems(1L, 1L, null, 2L)).willReturn(1L);
            given(problemRepository.findAvailableProblemIds(eq(1L), eq(1L), isNull(), eq(2L), any()))
                    .willReturn(List.of(1L));
            given(problemRepository.findById(1L)).willReturn(Optional.of(testProblem));

            ProblemResponse response = problemService.getRandomProblem(request);

            assertThat(response.problemId()).isEqualTo(1L);
        }
    }

    @Test
    @DisplayName("filters available problems by difficulty")
    void shouldReturnProblemWithSpecifiedDifficulty() {
        Chapter chapter = Chapter.builder()
                .name("Chapter 1")
                .description("Basic chapter")
                .orderNum(1)
                .build();

        Problem hardProblem = Problem.builder()
                .chapter(chapter)
                .content("Hard problem")
                .problemType(ProblemType.MULTIPLE_CHOICE)
                .difficulty(Difficulty.HIGH)
                .explanation("Explanation")
                .build();
        ReflectionTestUtils.setField(chapter, "id", 1L);
        ReflectionTestUtils.setField(hardProblem, "id", 1L);

        RandomProblemRequest request = new RandomProblemRequest(1L, 1L, Difficulty.HIGH);

        doNothing().when(userService).validateUserExists(1L);
        doNothing().when(chapterService).validateChapterExists(1L);
        given(userChapterStateRepository.findLastSkippedProblemId(1L, 1L)).willReturn(Optional.empty());
        given(problemRepository.countAvailableProblems(1L, 1L, Difficulty.HIGH, null)).willReturn(1L);
        given(problemRepository.findAvailableProblemIds(eq(1L), eq(1L), eq(Difficulty.HIGH), isNull(), any()))
                .willReturn(List.of(1L));
        given(problemRepository.findById(1L)).willReturn(Optional.of(hardProblem));
        given(statsService.calculateCorrectRate(1L)).willReturn(45);

        ProblemResponse response = problemService.getRandomProblem(request);

        assertThat(response.difficulty()).isEqualTo(Difficulty.HIGH);
    }

    @Nested
    @DisplayName("Wrong problem retry")
    class GetWrongProblems {

        @Test
        @DisplayName("returns one current wrong problem")
        void shouldReturnRandomWrongProblem() {
            doNothing().when(userService).validateUserExists(1L);
            doNothing().when(chapterService).validateChapterExists(1L);
            given(problemRepository.countWrongProblemIdsByUserIdAndChapterId(eq(1L), eq(1L), anyList()))
                    .willReturn(1L);
            given(problemRepository.findWrongProblemIdsByUserIdAndChapterId(eq(1L), eq(1L), anyList(), any()))
                    .willReturn(List.of(1L));
            given(problemRepository.findById(anyLong())).willReturn(Optional.of(testProblem));
            given(statsService.calculateCorrectRate(anyLong())).willReturn(50);

            ProblemResponse response = problemService.getRandomWrongProblem(1L, 1L);

            assertThat(response.problemId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("throws when there are no current wrong problems")
        void shouldThrowExceptionWhenNoWrongProblems() {
            doNothing().when(userService).validateUserExists(1L);
            doNothing().when(chapterService).validateChapterExists(1L);
            given(problemRepository.countWrongProblemIdsByUserIdAndChapterId(eq(1L), eq(1L), anyList()))
                    .willReturn(0L);

            assertThatThrownBy(() -> problemService.getRandomWrongProblem(1L, 1L))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.NO_MORE_PROBLEMS);
        }
    }
}

