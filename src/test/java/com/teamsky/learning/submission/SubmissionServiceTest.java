package com.teamsky.learning.submission;

import com.teamsky.learning.chapter.ChapterService;
import com.teamsky.learning.chapter.entity.Chapter;
import com.teamsky.learning.problem.ProblemService;
import com.teamsky.learning.problem.entity.Answer;
import com.teamsky.learning.problem.entity.Choice;
import com.teamsky.learning.problem.entity.Difficulty;
import com.teamsky.learning.problem.entity.Problem;
import com.teamsky.learning.problem.entity.ProblemType;
import com.teamsky.learning.stats.StatsService;
import com.teamsky.learning.submission.entity.AnswerStatus;
import com.teamsky.learning.submission.entity.Submission;
import com.teamsky.learning.submission.request.SkipRequest;
import com.teamsky.learning.submission.request.SubmitRequest;
import com.teamsky.learning.submission.response.SubmitResponse;
import com.teamsky.learning.user.UserService;
import com.teamsky.learning.user.entity.User;
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
        @DisplayName("returns WRONG when correct and wrong answers are mixed")
        void shouldReturnWrongWhenContainsCorrectAndWrong() {
            stubMultipleChoiceSubmit();

            SubmitRequest request = new SubmitRequest(
                    1L, 1L, ProblemType.MULTIPLE_CHOICE,
                    List.of(1, 3), null, null, false
            );

            SubmitResponse response = submissionService.submit(request);

            assertThat(response.answerStatus()).isEqualTo(AnswerStatus.WRONG);
        }

        @Test
        @DisplayName("does not update stats when a hint was used")
        void shouldNotUpdateStatsWhenHintUsed() {
            stubMultipleChoiceSubmit();

            SubmitRequest request = new SubmitRequest(
                    1L, 1L, ProblemType.MULTIPLE_CHOICE,
                    List.of(1, 2), null, null, true
            );

            submissionService.submit(request);

            verify(statsService, never()).updateStats(anyLong(), anyBoolean());
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

        SubmitRequest request = new SubmitRequest(
                1L, 1L, ProblemType.SUBJECTIVE,
                null, "Seoul", null, false
        );

        submissionService.submit(request);

        verify(userProblemStateRepository).upsertSubmissionState(1L, 1L, 100L, "CORRECT", true);
        verify(statsService).updateSubmissionStats(1L, 1L, true);
    }

    @Test
    @DisplayName("upserts wrong submission state with the latest status")
    void shouldUpsertWrongSubmissionState() {
        given(userService.findById(1L)).willReturn(testUser);
        given(problemService.findByIdWithAnswers(1L)).willReturn(subjectiveProblem);
        given(submissionRepository.save(any(Submission.class))).willAnswer(invocation -> withPersistedId(invocation.getArgument(0)));

        SubmitRequest request = new SubmitRequest(
                1L, 1L, ProblemType.SUBJECTIVE,
                null, "Busan", null, false
        );

        submissionService.submit(request);

        verify(userProblemStateRepository).upsertSubmissionState(1L, 1L, 100L, "WRONG", false);
        verify(statsService).updateSubmissionStats(1L, 1L, false);
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
                .willThrow(new com.teamsky.learning.common.exception.BusinessException(
                        com.teamsky.learning.common.exception.ErrorCode.PROBLEM_NOT_FOUND
                ));

        assertThatThrownBy(() -> submissionService.skipProblem(new SkipRequest(1L, 1L, 1L)))
                .isInstanceOf(com.teamsky.learning.common.exception.BusinessException.class)
                .hasMessage("Problem not found");

        verify(userChapterStateRepository, never()).upsertLastSkippedProblem(anyLong(), anyLong(), anyLong());
    }

    private void stubMultipleChoiceSubmit() {
        given(userService.findById(1L)).willReturn(testUser);
        given(problemService.findByIdWithAnswers(1L)).willReturn(multipleChoiceProblem);
        given(submissionRepository.save(any(Submission.class))).willAnswer(invocation -> withPersistedId(invocation.getArgument(0)));
    }

    private void stubSubjectiveSubmit() {
        given(userService.findById(1L)).willReturn(testUser);
        given(problemService.findByIdWithAnswers(1L)).willReturn(subjectiveProblem);
        given(submissionRepository.save(any(Submission.class))).willAnswer(invocation -> withPersistedId(invocation.getArgument(0)));
    }

    private Submission withPersistedId(Submission submission) {
        ReflectionTestUtils.setField(submission, "id", 100L);
        return submission;
    }
}
