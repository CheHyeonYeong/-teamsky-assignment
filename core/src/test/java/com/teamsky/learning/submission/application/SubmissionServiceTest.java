package com.teamsky.learning.submission.application;

import com.teamsky.learning.chapter.application.ChapterService;
import com.teamsky.learning.chapter.domain.Chapter;
import com.teamsky.learning.problem.application.ProblemService;
import com.teamsky.learning.problem.domain.Answer;
import com.teamsky.learning.problem.domain.Choice;
import com.teamsky.learning.problem.domain.Difficulty;
import com.teamsky.learning.problem.domain.Problem;
import com.teamsky.learning.problem.domain.ProblemType;
import com.teamsky.learning.stats.application.StatsService;
import com.teamsky.learning.submission.domain.AnswerStatus;
import com.teamsky.learning.submission.domain.Submission;
import com.teamsky.learning.submission.infrastructure.SubmissionRepository;
import com.teamsky.learning.submission.infrastructure.UserChapterStateRepository;
import com.teamsky.learning.submission.infrastructure.UserProblemStateRepository;
import com.teamsky.learning.submission.presentation.request.SkipRequest;
import com.teamsky.learning.submission.presentation.request.SubmitRequest;
import com.teamsky.learning.submission.presentation.response.SubmitResponse;
import com.teamsky.learning.user.application.UserService;
import com.teamsky.learning.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubmissionService tests")
class SubmissionServiceTest {

    @InjectMocks
    private SubmissionService submissionService;

    @Mock
    private SubmissionRepository submissionRepository;

    @Mock
    private UserProblemStateRepository userProblemStateRepository;

    @Mock
    private UserChapterStateRepository userChapterStateRepository;

    @Mock
    private UserService userService;

    @Mock
    private ProblemService problemService;

    @Mock
    private ChapterService chapterService;

    @Mock
    private StatsService statsService;

    private User testUser;
    private Chapter testChapter;
    private Problem multipleChoiceProblem;
    private Problem subjectiveProblem;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .name("Test User")
                .email("test@example.com")
                .build();
        ReflectionTestUtils.setField(testUser, "id", 1L);

        testChapter = Chapter.builder()
                .name("Chapter 1")
                .description("Basic chapter")
                .orderNum(1)
                .build();
        ReflectionTestUtils.setField(testChapter, "id", 1L);

        multipleChoiceProblem = Problem.builder()
                .chapter(testChapter)
                .content("Select all correct choices.")
                .problemType(ProblemType.MULTIPLE_CHOICE)
                .difficulty(Difficulty.MEDIUM)
                .explanation("Explanation")
                .build();
        ReflectionTestUtils.setField(multipleChoiceProblem, "id", 1L);
        multipleChoiceProblem.addAnswer(Answer.builder().answerValue("1").build());
        multipleChoiceProblem.addAnswer(Answer.builder().answerValue("2").build());
        for (int i = 1; i <= 5; i++) {
            multipleChoiceProblem.addChoice(Choice.builder()
                    .choiceNumber(i)
                    .content("Choice " + i)
                    .build());
        }

        subjectiveProblem = Problem.builder()
                .chapter(testChapter)
                .content("What is the capital of Korea?")
                .problemType(ProblemType.SUBJECTIVE)
                .difficulty(Difficulty.LOW)
                .explanation("Seoul is the capital.")
                .build();
        ReflectionTestUtils.setField(subjectiveProblem, "id", 1L);
        subjectiveProblem.addAnswer(Answer.builder().answerValue("Seoul").build());
    }

    @Nested
    @DisplayName("Multiple choice judging")
    class MultipleChoiceJudgement {

        @Test
        @DisplayName("returns CORRECT when all answers match")
        void shouldReturnCorrectWhenAllAnswersMatch() {
            stubMultipleChoiceSubmit();

            SubmitRequest request = new SubmitRequest(
                    1L, 1L, ProblemType.MULTIPLE_CHOICE,
                    List.of(1, 2), null, null, false
            );

            SubmitResponse response = submissionService.submit(request);

            assertThat(response.answerStatus()).isEqualTo(AnswerStatus.CORRECT);
        }

        @Test
        @DisplayName("returns PARTIAL when only a subset of correct answers is selected")
        void shouldReturnPartialWhenSomeAnswersMatch() {
            stubMultipleChoiceSubmit();

            SubmitRequest request = new SubmitRequest(
                    1L, 1L, ProblemType.MULTIPLE_CHOICE,
                    List.of(1), null, null, false
            );

            SubmitResponse response = submissionService.submit(request);

            assertThat(response.answerStatus()).isEqualTo(AnswerStatus.PARTIAL);
        }

        @Test
        @DisplayName("returns PARTIAL when correct and wrong answers are mixed")
        void shouldReturnPartialWhenContainsCorrectAndWrong() {
            stubMultipleChoiceSubmit();

            SubmitRequest request = new SubmitRequest(
                    1L, 1L, ProblemType.MULTIPLE_CHOICE,
                    List.of(1, 3), null, null, false
            );

            SubmitResponse response = submissionService.submit(request);

            assertThat(response.answerStatus()).isEqualTo(AnswerStatus.PARTIAL);
        }

        @Test
        @DisplayName("returns WRONG when no correct answer is included")
        void shouldReturnWrongWhenNoCorrectAnswerIsIncluded() {
            stubMultipleChoiceSubmit();

            SubmitRequest request = new SubmitRequest(
                    1L, 1L, ProblemType.MULTIPLE_CHOICE,
                    List.of(3, 4), null, null, false
            );

            SubmitResponse response = submissionService.submit(request);

            assertThat(response.answerStatus()).isEqualTo(AnswerStatus.WRONG);
        }
    }

    @Nested
    @DisplayName("Subjective judging")
    class SubjectiveJudgement {

        @Test
        @DisplayName("returns CORRECT when the answer matches")
        void shouldReturnCorrectWhenAnswerMatches() {
            stubSubjectiveSubmit();

            SubmitRequest request = new SubmitRequest(
                    1L, 1L, ProblemType.SUBJECTIVE,
                    null, "Seoul", null, false
            );

            SubmitResponse response = submissionService.submit(request);

            assertThat(response.answerStatus()).isEqualTo(AnswerStatus.CORRECT);
        }

        @Test
        @DisplayName("ignores case when judging subjective answers")
        void shouldIgnoreCaseWhenJudging() {
            stubSubjectiveSubmit();

            SubmitRequest request = new SubmitRequest(
                    1L, 1L, ProblemType.SUBJECTIVE,
                    null, "SEOUL", null, false
            );

            SubmitResponse response = submissionService.submit(request);

            assertThat(response.answerStatus()).isEqualTo(AnswerStatus.CORRECT);
        }

        @Test
        @DisplayName("returns WRONG when the answer does not match")
        void shouldReturnWrongWhenAnswerDoesNotMatch() {
            stubSubjectiveSubmit();

            SubmitRequest request = new SubmitRequest(
                    1L, 1L, ProblemType.SUBJECTIVE,
                    null, "Busan", null, false
            );

            SubmitResponse response = submissionService.submit(request);

            assertThat(response.answerStatus()).isEqualTo(AnswerStatus.WRONG);
        }

        @Test
        @DisplayName("returns WRONG for a blank answer")
        void shouldReturnWrongWhenAnswerIsBlank() {
            stubSubjectiveSubmit();

            SubmitRequest request = new SubmitRequest(
                    1L, 1L, ProblemType.SUBJECTIVE,
                    null, "", null, false
            );

            SubmitResponse response = submissionService.submit(request);

            assertThat(response.answerStatus()).isEqualTo(AnswerStatus.WRONG);
        }
    }

    @Test
    @DisplayName("records time spent in the saved submission")
    void shouldRecordTimeSpent() {
        given(userService.findById(1L)).willReturn(testUser);
        given(problemService.findByIdWithAnswers(1L)).willReturn(subjectiveProblem);

        ArgumentCaptor<Submission> submissionCaptor = ArgumentCaptor.forClass(Submission.class);
        given(submissionRepository.save(submissionCaptor.capture()))
                .willAnswer(invocation -> withPersistedId(invocation.getArgument(0)));

        SubmitRequest request = new SubmitRequest(
                1L, 1L, ProblemType.SUBJECTIVE,
                null, "Seoul", 120L, false
        );

        submissionService.submit(request);

        assertThat(submissionCaptor.getValue().getTimeSpentSeconds()).isEqualTo(120L);
    }

    @Test
    @DisplayName("upserts user problem state on submission")
    void shouldUpsertUserProblemStateOnSubmission() {
        given(userService.findById(1L)).willReturn(testUser);
        given(problemService.findByIdWithAnswers(1L)).willReturn(subjectiveProblem);
        given(submissionRepository.save(any(Submission.class))).willAnswer(invocation -> withPersistedId(invocation.getArgument(0)));
        given(userProblemStateRepository.upsertSubmissionState(anyLong(), anyLong(), anyLong(), anyString(), anyBoolean()))
                .willReturn(1);

        SubmitRequest request = new SubmitRequest(
                1L, 1L, ProblemType.SUBJECTIVE,
                null, "Seoul", null, false
        );

        submissionService.submit(request);

        verify(userProblemStateRepository).upsertSubmissionState(1L, 1L, 100L, "CORRECT", true);
        verify(statsService).updateSubmissionStats(1L, 1L, true);
        verify(statsService).updateProblemStats(1L, true, true);
    }

    @Test
    @DisplayName("upserts wrong submission state with the latest status")
    void shouldUpsertWrongSubmissionState() {
        given(userService.findById(1L)).willReturn(testUser);
        given(problemService.findByIdWithAnswers(1L)).willReturn(subjectiveProblem);
        given(submissionRepository.save(any(Submission.class))).willAnswer(invocation -> withPersistedId(invocation.getArgument(0)));
        given(userProblemStateRepository.upsertSubmissionState(anyLong(), anyLong(), anyLong(), anyString(), anyBoolean()))
                .willReturn(1);

        SubmitRequest request = new SubmitRequest(
                1L, 1L, ProblemType.SUBJECTIVE,
                null, "Busan", null, false
        );

        submissionService.submit(request);

        verify(userProblemStateRepository).upsertSubmissionState(1L, 1L, 100L, "WRONG", false);
        verify(statsService).updateSubmissionStats(1L, 1L, false);
        verify(statsService).updateProblemStats(1L, false, true);
    }

    @Test
    @DisplayName("does not count repeat submissions twice in problem correct-rate stats")
    void shouldSkipProblemStatsForRepeatProblemAttempts() {
        given(userService.findById(1L)).willReturn(testUser);
        given(problemService.findByIdWithAnswers(1L)).willReturn(subjectiveProblem);
        given(submissionRepository.save(any(Submission.class))).willAnswer(invocation -> withPersistedId(invocation.getArgument(0)));
        given(userProblemStateRepository.upsertSubmissionState(anyLong(), anyLong(), anyLong(), anyString(), anyBoolean()))
                .willReturn(2);

        SubmitRequest request = new SubmitRequest(
                1L, 1L, ProblemType.SUBJECTIVE,
                null, "Seoul", null, false
        );

        submissionService.submit(request);

        verify(statsService).updateProblemStats(1L, true, false);
    }

    @Test
    @DisplayName("upserts chapter skip state")
    void shouldUpsertSkipStateWithinChapter() {
        given(problemService.findByIdInChapter(1L, 1L)).willReturn(subjectiveProblem);

        submissionService.skipProblem(new SkipRequest(1L, 1L, 1L));

        verify(userService).validateUserExists(1L);
        verify(chapterService).validateChapterExists(1L);
        verify(userChapterStateRepository).upsertLastSkippedProblem(1L, 1L, 1L);
    }

    @Test
    @DisplayName("rejects skip requests when the problem does not belong to the chapter")
    void shouldRejectSkipWhenProblemIsOutsideChapter() {
        given(problemService.findByIdInChapter(1L, 1L))
                .willThrow(new com.teamsky.learning.shared.exception.BusinessException(
                        com.teamsky.learning.shared.exception.ErrorCode.PROBLEM_NOT_FOUND
                ));

        assertThatThrownBy(() -> submissionService.skipProblem(new SkipRequest(1L, 1L, 1L)))
                .isInstanceOf(com.teamsky.learning.shared.exception.BusinessException.class)
                .hasMessage("Problem not found");

        verify(userChapterStateRepository, never()).upsertLastSkippedProblem(anyLong(), anyLong(), anyLong());
    }

    private void stubMultipleChoiceSubmit() {
        given(userService.findById(1L)).willReturn(testUser);
        given(problemService.findByIdWithAnswers(1L)).willReturn(multipleChoiceProblem);
        given(submissionRepository.save(any(Submission.class))).willAnswer(invocation -> withPersistedId(invocation.getArgument(0)));
        given(userProblemStateRepository.upsertSubmissionState(anyLong(), anyLong(), anyLong(), anyString(), anyBoolean()))
                .willReturn(1);
    }

    private void stubSubjectiveSubmit() {
        given(userService.findById(1L)).willReturn(testUser);
        given(problemService.findByIdWithAnswers(1L)).willReturn(subjectiveProblem);
        given(submissionRepository.save(any(Submission.class))).willAnswer(invocation -> withPersistedId(invocation.getArgument(0)));
        given(userProblemStateRepository.upsertSubmissionState(anyLong(), anyLong(), anyLong(), anyString(), anyBoolean()))
                .willReturn(1);
    }

    private Submission withPersistedId(Submission submission) {
        ReflectionTestUtils.setField(submission, "id", 100L);
        return submission;
    }
}

