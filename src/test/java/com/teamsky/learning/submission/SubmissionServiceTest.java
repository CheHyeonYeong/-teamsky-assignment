package com.teamsky.learning.submission;

import com.teamsky.learning.chapter.ChapterService;
import com.teamsky.learning.chapter.entity.Chapter;
import com.teamsky.learning.common.exception.BusinessException;
import com.teamsky.learning.common.exception.ErrorCode;
import com.teamsky.learning.problem.ProblemService;
import com.teamsky.learning.problem.entity.Answer;
import com.teamsky.learning.problem.entity.Choice;
import com.teamsky.learning.problem.entity.Difficulty;
import com.teamsky.learning.problem.entity.Problem;
import com.teamsky.learning.problem.entity.ProblemType;
import com.teamsky.learning.stats.StatsService;
import com.teamsky.learning.submission.entity.AnswerStatus;
import com.teamsky.learning.submission.entity.Submission;
import com.teamsky.learning.submission.event.SubmissionStatsUpdateRequestedEvent;
import com.teamsky.learning.submission.SubmissionRepository;
import com.teamsky.learning.submission.UserChapterStateRepository;
import com.teamsky.learning.submission.UserProblemStateRepository;
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
import org.springframework.context.ApplicationEventPublisher;
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

    private static final long TEST_USER_ID = 1L;
    private static final long TEST_CHAPTER_ID = 1L;
    private static final long TEST_PROBLEM_ID = 1L;
    private static final long PERSISTED_SUBMISSION_ID = 100L;
    private static final long TIME_SPENT_SECONDS = 120L;
    private static final int CHOICE_COUNT = 5;
    private static final String TEST_USER_NAME = "Test User";
    private static final String TEST_USER_EMAIL = "test@example.com";
    private static final String TEST_CHAPTER_NAME = "Chapter 1";
    private static final String TEST_CHAPTER_DESCRIPTION = "Basic chapter";
    private static final String MULTIPLE_CHOICE_CONTENT = "Select all correct choices.";
    private static final String MULTIPLE_CHOICE_EXPLANATION = "Explanation";
    private static final String SUBJECTIVE_CONTENT = "What is the capital of Korea?";
    private static final String SUBJECTIVE_EXPLANATION = "Seoul is the capital.";
    private static final String CORRECT_SUBJECTIVE_ANSWER = "Seoul";
    private static final String UPPERCASE_SUBJECTIVE_ANSWER = "SEOUL";
    private static final String WRONG_SUBJECTIVE_ANSWER = "Busan";
    private static final String BLANK_SUBJECTIVE_ANSWER = "";
    private static final String CHOICE_CONTENT_PREFIX = "Choice ";

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

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    private User testUser;
    private Chapter testChapter;
    private Problem multipleChoiceProblem;
    private Problem subjectiveProblem;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .name(TEST_USER_NAME)
                .email(TEST_USER_EMAIL)
                .build();
        ReflectionTestUtils.setField(testUser, "id", TEST_USER_ID);

        testChapter = Chapter.builder()
                .name(TEST_CHAPTER_NAME)
                .description(TEST_CHAPTER_DESCRIPTION)
                .orderNum(1)
                .build();
        ReflectionTestUtils.setField(testChapter, "id", TEST_CHAPTER_ID);

        multipleChoiceProblem = Problem.builder()
                .chapter(testChapter)
                .content(MULTIPLE_CHOICE_CONTENT)
                .problemType(ProblemType.MULTIPLE_CHOICE)
                .difficulty(Difficulty.MEDIUM)
                .explanation(MULTIPLE_CHOICE_EXPLANATION)
                .build();
        ReflectionTestUtils.setField(multipleChoiceProblem, "id", TEST_PROBLEM_ID);
        multipleChoiceProblem.addAnswer(Answer.builder().answerValue("1").build());
        multipleChoiceProblem.addAnswer(Answer.builder().answerValue("2").build());
        for (int i = 1; i <= CHOICE_COUNT; i++) {
            multipleChoiceProblem.addChoice(Choice.builder()
                    .choiceNumber(i)
                    .content(CHOICE_CONTENT_PREFIX + i)
                    .build());
        }

        subjectiveProblem = Problem.builder()
                .chapter(testChapter)
                .content(SUBJECTIVE_CONTENT)
                .problemType(ProblemType.SUBJECTIVE)
                .difficulty(Difficulty.LOW)
                .explanation(SUBJECTIVE_EXPLANATION)
                .build();
        ReflectionTestUtils.setField(subjectiveProblem, "id", TEST_PROBLEM_ID);
        subjectiveProblem.addAnswer(Answer.builder().answerValue(CORRECT_SUBJECTIVE_ANSWER).build());
    }

    @Nested
    @DisplayName("Multiple choice judging")
    class MultipleChoiceJudgement {

        @Test
        @DisplayName("returns CORRECT when all answers match")
        void shouldReturnCorrectWhenAllAnswersMatch() {
            stubMultipleChoiceSubmit();

            SubmitRequest request = multipleChoiceRequest(List.of(1, 2));

            SubmitResponse response = submissionService.submit(request);

            assertThat(response.answerStatus()).isEqualTo(AnswerStatus.CORRECT);
        }

        @Test
        @DisplayName("returns PARTIAL when only a subset of correct answers is selected")
        void shouldReturnPartialWhenSomeAnswersMatch() {
            stubMultipleChoiceSubmit();

            SubmitRequest request = multipleChoiceRequest(List.of(1));

            SubmitResponse response = submissionService.submit(request);

            assertThat(response.answerStatus()).isEqualTo(AnswerStatus.PARTIAL);
        }

        @Test
        @DisplayName("returns PARTIAL when correct and wrong answers are mixed")
        void shouldReturnPartialWhenContainsCorrectAndWrong() {
            stubMultipleChoiceSubmit();

            SubmitRequest request = multipleChoiceRequest(List.of(1, 3));

            SubmitResponse response = submissionService.submit(request);

            assertThat(response.answerStatus()).isEqualTo(AnswerStatus.PARTIAL);
        }

        @Test
        @DisplayName("returns WRONG when no correct answer is included")
        void shouldReturnWrongWhenNoCorrectAnswerIsIncluded() {
            stubMultipleChoiceSubmit();

            SubmitRequest request = multipleChoiceRequest(List.of(3, 4));

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

            SubmitRequest request = subjectiveRequest(CORRECT_SUBJECTIVE_ANSWER, null);

            SubmitResponse response = submissionService.submit(request);

            assertThat(response.answerStatus()).isEqualTo(AnswerStatus.CORRECT);
        }

        @Test
        @DisplayName("ignores case when judging subjective answers")
        void shouldIgnoreCaseWhenJudging() {
            stubSubjectiveSubmit();

            SubmitRequest request = subjectiveRequest(UPPERCASE_SUBJECTIVE_ANSWER, null);

            SubmitResponse response = submissionService.submit(request);

            assertThat(response.answerStatus()).isEqualTo(AnswerStatus.CORRECT);
        }

        @Test
        @DisplayName("returns WRONG when the answer does not match")
        void shouldReturnWrongWhenAnswerDoesNotMatch() {
            stubSubjectiveSubmit();

            SubmitRequest request = subjectiveRequest(WRONG_SUBJECTIVE_ANSWER, null);

            SubmitResponse response = submissionService.submit(request);

            assertThat(response.answerStatus()).isEqualTo(AnswerStatus.WRONG);
        }

        @Test
        @DisplayName("returns WRONG for a blank answer")
        void shouldReturnWrongWhenAnswerIsBlank() {
            stubSubjectiveSubmit();

            SubmitRequest request = subjectiveRequest(BLANK_SUBJECTIVE_ANSWER, null);

            SubmitResponse response = submissionService.submit(request);

            assertThat(response.answerStatus()).isEqualTo(AnswerStatus.WRONG);
        }
    }

    @Test
    @DisplayName("records time spent in the saved submission")
    void shouldRecordTimeSpent() {
        given(userService.findById(TEST_USER_ID)).willReturn(testUser);
        given(problemService.findByIdWithAnswers(TEST_PROBLEM_ID)).willReturn(subjectiveProblem);

        ArgumentCaptor<Submission> submissionCaptor = ArgumentCaptor.forClass(Submission.class);
        given(submissionRepository.save(submissionCaptor.capture()))
                .willAnswer(invocation -> withPersistedId(invocation.getArgument(0)));

        SubmitRequest request = subjectiveRequest(CORRECT_SUBJECTIVE_ANSWER, TIME_SPENT_SECONDS);

        submissionService.submit(request);

        assertThat(submissionCaptor.getValue().getTimeSpentSeconds()).isEqualTo(TIME_SPENT_SECONDS);
    }

    @Test
    @DisplayName("upserts user problem state on submission")
    void shouldUpsertUserProblemStateOnSubmission() {
        given(userService.findById(TEST_USER_ID)).willReturn(testUser);
        given(problemService.findByIdWithAnswers(TEST_PROBLEM_ID)).willReturn(subjectiveProblem);
        given(submissionRepository.save(any(Submission.class))).willAnswer(invocation -> withPersistedId(invocation.getArgument(0)));
        given(userProblemStateRepository.upsertSubmissionState(anyLong(), anyLong(), anyLong(), anyString(), anyBoolean()))
                .willReturn(1);

        SubmitRequest request = subjectiveRequest(CORRECT_SUBJECTIVE_ANSWER, null);

        submissionService.submit(request);

        verify(userProblemStateRepository).upsertSubmissionState(
                TEST_USER_ID, TEST_PROBLEM_ID, PERSISTED_SUBMISSION_ID, "CORRECT", true);
        ArgumentCaptor<SubmissionStatsUpdateRequestedEvent> eventCaptor =
                ArgumentCaptor.forClass(SubmissionStatsUpdateRequestedEvent.class);
        verify(applicationEventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isEqualTo(new SubmissionStatsUpdateRequestedEvent(
                TEST_USER_ID, TEST_CHAPTER_ID, TEST_PROBLEM_ID, true, true
        ));
    }

    @Test
    @DisplayName("upserts wrong submission state with the latest status")
    void shouldUpsertWrongSubmissionState() {
        given(userService.findById(TEST_USER_ID)).willReturn(testUser);
        given(problemService.findByIdWithAnswers(TEST_PROBLEM_ID)).willReturn(subjectiveProblem);
        given(submissionRepository.save(any(Submission.class))).willAnswer(invocation -> withPersistedId(invocation.getArgument(0)));
        given(userProblemStateRepository.upsertSubmissionState(anyLong(), anyLong(), anyLong(), anyString(), anyBoolean()))
                .willReturn(1);

        SubmitRequest request = subjectiveRequest(WRONG_SUBJECTIVE_ANSWER, null);

        submissionService.submit(request);

        verify(userProblemStateRepository).upsertSubmissionState(
                TEST_USER_ID, TEST_PROBLEM_ID, PERSISTED_SUBMISSION_ID, "WRONG", false);
        ArgumentCaptor<SubmissionStatsUpdateRequestedEvent> eventCaptor =
                ArgumentCaptor.forClass(SubmissionStatsUpdateRequestedEvent.class);
        verify(applicationEventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isEqualTo(new SubmissionStatsUpdateRequestedEvent(
                TEST_USER_ID, TEST_CHAPTER_ID, TEST_PROBLEM_ID, false, true
        ));
    }

    @Test
    @DisplayName("does not count repeat submissions twice in problem correct-rate stats")
    void shouldSkipProblemStatsForRepeatProblemAttempts() {
        given(userService.findById(TEST_USER_ID)).willReturn(testUser);
        given(problemService.findByIdWithAnswers(TEST_PROBLEM_ID)).willReturn(subjectiveProblem);
        given(submissionRepository.save(any(Submission.class))).willAnswer(invocation -> withPersistedId(invocation.getArgument(0)));
        given(userProblemStateRepository.upsertSubmissionState(anyLong(), anyLong(), anyLong(), anyString(), anyBoolean()))
                .willReturn(2);

        SubmitRequest request = subjectiveRequest(CORRECT_SUBJECTIVE_ANSWER, null);

        submissionService.submit(request);

        ArgumentCaptor<SubmissionStatsUpdateRequestedEvent> eventCaptor =
                ArgumentCaptor.forClass(SubmissionStatsUpdateRequestedEvent.class);
        verify(applicationEventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isEqualTo(new SubmissionStatsUpdateRequestedEvent(
                TEST_USER_ID, TEST_CHAPTER_ID, TEST_PROBLEM_ID, true, false
        ));
    }

    @Test
    @DisplayName("upserts chapter skip state")
    void shouldUpsertSkipStateWithinChapter() {
        given(problemService.findByIdInChapter(TEST_PROBLEM_ID, TEST_CHAPTER_ID)).willReturn(subjectiveProblem);

        submissionService.skipProblem(new SkipRequest(TEST_USER_ID, TEST_CHAPTER_ID, TEST_PROBLEM_ID));

        verify(userService).validateUserExists(TEST_USER_ID);
        verify(chapterService).validateChapterExists(TEST_CHAPTER_ID);
        verify(userChapterStateRepository).upsertLastSkippedProblem(TEST_USER_ID, TEST_CHAPTER_ID, TEST_PROBLEM_ID);
    }

    @Test
    @DisplayName("rejects skip requests when the problem does not belong to the chapter")
    void shouldRejectSkipWhenProblemIsOutsideChapter() {
        given(problemService.findByIdInChapter(TEST_PROBLEM_ID, TEST_CHAPTER_ID))
                .willThrow(new BusinessException(ErrorCode.PROBLEM_NOT_FOUND));

        assertThatThrownBy(() -> submissionService.skipProblem(new SkipRequest(TEST_USER_ID, TEST_CHAPTER_ID, TEST_PROBLEM_ID)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Problem not found");

        verify(userChapterStateRepository, never()).upsertLastSkippedProblem(anyLong(), anyLong(), anyLong());
    }

    private void stubMultipleChoiceSubmit() {
        given(userService.findById(TEST_USER_ID)).willReturn(testUser);
        given(problemService.findByIdWithAnswers(TEST_PROBLEM_ID)).willReturn(multipleChoiceProblem);
        given(submissionRepository.save(any(Submission.class))).willAnswer(invocation -> withPersistedId(invocation.getArgument(0)));
        given(userProblemStateRepository.upsertSubmissionState(anyLong(), anyLong(), anyLong(), anyString(), anyBoolean()))
                .willReturn(1);
    }

    private void stubSubjectiveSubmit() {
        given(userService.findById(TEST_USER_ID)).willReturn(testUser);
        given(problemService.findByIdWithAnswers(TEST_PROBLEM_ID)).willReturn(subjectiveProblem);
        given(submissionRepository.save(any(Submission.class))).willAnswer(invocation -> withPersistedId(invocation.getArgument(0)));
        given(userProblemStateRepository.upsertSubmissionState(anyLong(), anyLong(), anyLong(), anyString(), anyBoolean()))
                .willReturn(1);
    }

    private SubmitRequest multipleChoiceRequest(List<Integer> choiceNumbers) {
        return new SubmitRequest(
                TEST_USER_ID, TEST_PROBLEM_ID, ProblemType.MULTIPLE_CHOICE,
                choiceNumbers, null, null, false
        );
    }

    private SubmitRequest subjectiveRequest(String userAnswer, Long timeSpentSeconds) {
        return new SubmitRequest(
                TEST_USER_ID, TEST_PROBLEM_ID, ProblemType.SUBJECTIVE,
                null, userAnswer, timeSpentSeconds, false
        );
    }

    private Submission withPersistedId(Submission submission) {
        ReflectionTestUtils.setField(submission, "id", PERSISTED_SUBMISSION_ID);
        return submission;
    }
}

