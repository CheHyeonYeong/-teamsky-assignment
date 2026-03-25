package com.teamsky.learning.problem;

import com.teamsky.learning.chapter.ChapterService;
import com.teamsky.learning.chapter.entity.Chapter;
import com.teamsky.learning.common.exception.BusinessException;
import com.teamsky.learning.common.exception.ErrorCode;
import com.teamsky.learning.problem.entity.Choice;
import com.teamsky.learning.problem.entity.Difficulty;
import com.teamsky.learning.problem.entity.Problem;
import com.teamsky.learning.problem.entity.ProblemType;
import com.teamsky.learning.problem.ProblemRepository;
import com.teamsky.learning.problem.request.RandomProblemRequest;
import com.teamsky.learning.problem.response.ProblemResponse;
import com.teamsky.learning.stats.StatsService;
import com.teamsky.learning.submission.UserChapterStateRepository;
import com.teamsky.learning.user.UserService;
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

    private static final long TEST_USER_ID = 1L;
    private static final long TEST_CHAPTER_ID = 1L;
    private static final long TEST_PROBLEM_ID = 1L;
    private static final long TEST_SKIPPED_PROBLEM_ID = 2L;
    private static final int CHOICE_COUNT = 5;
    private static final String TEST_CHAPTER_NAME = "Chapter 1";
    private static final String TEST_CHAPTER_DESCRIPTION = "Basic chapter";
    private static final String TEST_PROBLEM_CONTENT = "Test problem";
    private static final String TEST_HARD_PROBLEM_CONTENT = "Hard problem";
    private static final String TEST_PROBLEM_EXPLANATION = "Explanation";
    private static final String CHOICE_CONTENT_PREFIX = "Choice ";

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
                .name(TEST_CHAPTER_NAME)
                .description(TEST_CHAPTER_DESCRIPTION)
                .orderNum(1)
                .build();
        ReflectionTestUtils.setField(chapter, "id", TEST_CHAPTER_ID);

        testProblem = Problem.builder()
                .chapter(chapter)
                .content(TEST_PROBLEM_CONTENT)
                .problemType(ProblemType.MULTIPLE_CHOICE)
                .difficulty(Difficulty.MEDIUM)
                .explanation(TEST_PROBLEM_EXPLANATION)
                .build();
        ReflectionTestUtils.setField(testProblem, "id", TEST_PROBLEM_ID);

        for (int i = 1; i <= CHOICE_COUNT; i++) {
            testProblem.addChoice(Choice.builder()
                    .choiceNumber(i)
                    .content(CHOICE_CONTENT_PREFIX + i)
                    .build());
        }
    }

    @Nested
    @DisplayName("Random problem")
    class GetRandomProblem {

        @Test
        @DisplayName("returns one available problem")
        void shouldReturnRandomUnsolvedProblem() {
            RandomProblemRequest request = new RandomProblemRequest(TEST_USER_ID, TEST_CHAPTER_ID, null);

            doNothing().when(userService).validateUserExists(TEST_USER_ID);
            doNothing().when(chapterService).validateChapterExists(TEST_CHAPTER_ID);
            given(userChapterStateRepository.findLastSkippedProblemId(TEST_USER_ID, TEST_CHAPTER_ID)).willReturn(Optional.empty());
            given(problemRepository.countAvailableProblems(TEST_CHAPTER_ID, TEST_USER_ID, null, null)).willReturn(1L);
            given(problemRepository.findAvailableProblemIds(eq(TEST_CHAPTER_ID), eq(TEST_USER_ID), isNull(), isNull(), any()))
                    .willReturn(List.of(TEST_PROBLEM_ID));
            given(problemRepository.findById(TEST_PROBLEM_ID)).willReturn(Optional.of(testProblem));
            given(statsService.calculateCorrectRate(TEST_PROBLEM_ID)).willReturn(67);

            ProblemResponse response = problemService.getRandomProblem(request);

            assertThat(response.problemId()).isEqualTo(TEST_PROBLEM_ID);
            assertThat(response.content()).isEqualTo(TEST_PROBLEM_CONTENT);
            assertThat(response.choices()).hasSize(CHOICE_COUNT);
            assertThat(response.answerCorrectRate()).isEqualTo(67);
        }

        @Test
        @DisplayName("throws when there are no available problems")
        void shouldThrowExceptionWhenNoMoreProblems() {
            RandomProblemRequest request = new RandomProblemRequest(TEST_USER_ID, TEST_CHAPTER_ID, null);

            doNothing().when(userService).validateUserExists(TEST_USER_ID);
            doNothing().when(chapterService).validateChapterExists(TEST_CHAPTER_ID);
            given(userChapterStateRepository.findLastSkippedProblemId(TEST_USER_ID, TEST_CHAPTER_ID)).willReturn(Optional.empty());
            given(problemRepository.countAvailableProblems(TEST_CHAPTER_ID, TEST_USER_ID, null, null)).willReturn(0L);

            assertThatThrownBy(() -> problemService.getRandomProblem(request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.NO_MORE_PROBLEMS);
        }

        @Test
        @DisplayName("excludes the last skipped problem in the chapter")
        void shouldExcludeLastSkippedProblem() {
            RandomProblemRequest request = new RandomProblemRequest(TEST_USER_ID, TEST_CHAPTER_ID, null);

            doNothing().when(userService).validateUserExists(TEST_USER_ID);
            doNothing().when(chapterService).validateChapterExists(TEST_CHAPTER_ID);
            given(userChapterStateRepository.findLastSkippedProblemId(TEST_USER_ID, TEST_CHAPTER_ID))
                    .willReturn(Optional.of(TEST_SKIPPED_PROBLEM_ID));
            given(problemRepository.countAvailableProblems(TEST_CHAPTER_ID, TEST_USER_ID, null, TEST_SKIPPED_PROBLEM_ID))
                    .willReturn(1L);
            given(problemRepository.findAvailableProblemIds(
                    eq(TEST_CHAPTER_ID), eq(TEST_USER_ID), isNull(), eq(TEST_SKIPPED_PROBLEM_ID), any()))
                    .willReturn(List.of(TEST_PROBLEM_ID));
            given(problemRepository.findById(TEST_PROBLEM_ID)).willReturn(Optional.of(testProblem));

            ProblemResponse response = problemService.getRandomProblem(request);

            assertThat(response.problemId()).isEqualTo(TEST_PROBLEM_ID);
        }
    }

    @Test
    @DisplayName("filters available problems by difficulty")
    void shouldReturnProblemWithSpecifiedDifficulty() {
        Chapter chapter = Chapter.builder()
                .name(TEST_CHAPTER_NAME)
                .description(TEST_CHAPTER_DESCRIPTION)
                .orderNum(1)
                .build();

        Problem hardProblem = Problem.builder()
                .chapter(chapter)
                .content(TEST_HARD_PROBLEM_CONTENT)
                .problemType(ProblemType.MULTIPLE_CHOICE)
                .difficulty(Difficulty.HIGH)
                .explanation(TEST_PROBLEM_EXPLANATION)
                .build();
        ReflectionTestUtils.setField(chapter, "id", TEST_CHAPTER_ID);
        ReflectionTestUtils.setField(hardProblem, "id", TEST_PROBLEM_ID);

        RandomProblemRequest request = new RandomProblemRequest(TEST_USER_ID, TEST_CHAPTER_ID, Difficulty.HIGH);

        doNothing().when(userService).validateUserExists(TEST_USER_ID);
        doNothing().when(chapterService).validateChapterExists(TEST_CHAPTER_ID);
        given(userChapterStateRepository.findLastSkippedProblemId(TEST_USER_ID, TEST_CHAPTER_ID)).willReturn(Optional.empty());
        given(problemRepository.countAvailableProblems(TEST_CHAPTER_ID, TEST_USER_ID, Difficulty.HIGH, null)).willReturn(1L);
        given(problemRepository.findAvailableProblemIds(eq(TEST_CHAPTER_ID), eq(TEST_USER_ID), eq(Difficulty.HIGH), isNull(), any()))
                .willReturn(List.of(TEST_PROBLEM_ID));
        given(problemRepository.findById(TEST_PROBLEM_ID)).willReturn(Optional.of(hardProblem));
        given(statsService.calculateCorrectRate(TEST_PROBLEM_ID)).willReturn(45);

        ProblemResponse response = problemService.getRandomProblem(request);

        assertThat(response.difficulty()).isEqualTo(Difficulty.HIGH);
    }

    @Nested
    @DisplayName("Wrong problem retry")
    class GetWrongProblems {

        @Test
        @DisplayName("returns one current wrong problem")
        void shouldReturnRandomWrongProblem() {
            doNothing().when(userService).validateUserExists(TEST_USER_ID);
            doNothing().when(chapterService).validateChapterExists(TEST_CHAPTER_ID);
            given(problemRepository.countWrongProblemIdsByUserIdAndChapterId(eq(TEST_USER_ID), eq(TEST_CHAPTER_ID), anyList()))
                    .willReturn(1L);
            given(problemRepository.findWrongProblemIdsByUserIdAndChapterId(eq(TEST_USER_ID), eq(TEST_CHAPTER_ID), anyList(), any()))
                    .willReturn(List.of(TEST_PROBLEM_ID));
            given(problemRepository.findById(anyLong())).willReturn(Optional.of(testProblem));
            given(statsService.calculateCorrectRate(anyLong())).willReturn(50);

            ProblemResponse response = problemService.getRandomWrongProblem(TEST_USER_ID, TEST_CHAPTER_ID);

            assertThat(response.problemId()).isEqualTo(TEST_PROBLEM_ID);
        }

        @Test
        @DisplayName("throws when there are no current wrong problems")
        void shouldThrowExceptionWhenNoWrongProblems() {
            doNothing().when(userService).validateUserExists(TEST_USER_ID);
            doNothing().when(chapterService).validateChapterExists(TEST_CHAPTER_ID);
            given(problemRepository.countWrongProblemIdsByUserIdAndChapterId(eq(TEST_USER_ID), eq(TEST_CHAPTER_ID), anyList()))
                    .willReturn(0L);

            assertThatThrownBy(() -> problemService.getRandomWrongProblem(TEST_USER_ID, TEST_CHAPTER_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.NO_MORE_PROBLEMS);
        }
    }
}

