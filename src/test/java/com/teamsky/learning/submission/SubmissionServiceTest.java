package com.teamsky.learning.submission;

import com.teamsky.learning.chapter.ChapterService;
import com.teamsky.learning.chapter.entity.Chapter;
import com.teamsky.learning.problem.ProblemService;
import com.teamsky.learning.problem.entity.*;
import com.teamsky.learning.stats.StatsService;
import com.teamsky.learning.submission.entity.AnswerStatus;
import com.teamsky.learning.submission.entity.SkippedProblem;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubmissionService 테스트")
class SubmissionServiceTest {

    @InjectMocks
    private SubmissionService submissionService;

    @Mock
    private SubmissionRepository submissionRepository;

    @Mock
    private SkippedProblemRepository skippedProblemRepository;

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
    private Problem testProblem;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .name("테스트 사용자")
                .email("test@example.com")
                .build();

        testChapter = Chapter.builder()
                .name("테스트 단원")
                .description("테스트 단원 설명")
                .orderNum(1)
                .build();
    }

    @Nested
    @DisplayName("객관식 정답 판정")
    class MultipleChoiceJudgement {

        @BeforeEach
        void setUp() {
            testProblem = Problem.builder()
                    .chapter(testChapter)
                    .content("다음 중 옳은 것을 모두 고르시오.")
                    .problemType(ProblemType.MULTIPLE_CHOICE)
                    .difficulty(Difficulty.MEDIUM)
                    .explanation("정답 해설입니다.")
                    .build();

            Answer answer1 = Answer.builder().answerValue("1").build();
            Answer answer2 = Answer.builder().answerValue("2").build();
            testProblem.addAnswer(answer1);
            testProblem.addAnswer(answer2);

            for (int i = 1; i <= 5; i++) {
                Choice choice = Choice.builder()
                        .choiceNumber(i)
                        .content("선택지 " + i)
                        .build();
                testProblem.addChoice(choice);
            }
        }

        @Test
        @DisplayName("모든 정답을 선택하면 CORRECT 반환")
        void shouldReturnCorrectWhenAllAnswersMatch() {
            // given
            given(userService.findById(1L)).willReturn(testUser);
            given(problemService.findById(1L)).willReturn(testProblem);
            given(submissionRepository.save(any(Submission.class))).willAnswer(invocation -> invocation.getArgument(0));

            SubmitRequest request = new SubmitRequest(
                    1L, 1L, ProblemType.MULTIPLE_CHOICE,
                    List.of(1, 2), null, null, false
            );

            // when
            SubmitResponse response = submissionService.submit(request);

            // then
            assertThat(response.answerStatus()).isEqualTo(AnswerStatus.CORRECT);
        }

        @Test
        @DisplayName("일부 정답만 선택하면 PARTIAL 반환")
        void shouldReturnPartialWhenSomeAnswersMatch() {
            // given
            given(userService.findById(1L)).willReturn(testUser);
            given(problemService.findById(1L)).willReturn(testProblem);
            given(submissionRepository.save(any(Submission.class))).willAnswer(invocation -> invocation.getArgument(0));

            SubmitRequest request = new SubmitRequest(
                    1L, 1L, ProblemType.MULTIPLE_CHOICE,
                    List.of(1), null, null, false
            );

            // when
            SubmitResponse response = submissionService.submit(request);

            // then
            assertThat(response.answerStatus()).isEqualTo(AnswerStatus.PARTIAL);
        }

        @Test
        @DisplayName("정답과 오답을 함께 선택하면 WRONG 반환")
        void shouldReturnWrongWhenContainsCorrectAndWrong() {
            // given
            given(userService.findById(1L)).willReturn(testUser);
            given(problemService.findById(1L)).willReturn(testProblem);
            given(submissionRepository.save(any(Submission.class))).willAnswer(invocation -> invocation.getArgument(0));

            SubmitRequest request = new SubmitRequest(
                    1L, 1L, ProblemType.MULTIPLE_CHOICE,
                    List.of(1, 3), null, null, false
            );

            // when
            SubmitResponse response = submissionService.submit(request);

            // then
            assertThat(response.answerStatus()).isEqualTo(AnswerStatus.WRONG);
        }

        @Test
        @DisplayName("정답이 하나도 없으면 WRONG 반환")
        void shouldReturnWrongWhenNoCorrectAnswers() {
            // given
            given(userService.findById(1L)).willReturn(testUser);
            given(problemService.findById(1L)).willReturn(testProblem);
            given(submissionRepository.save(any(Submission.class))).willAnswer(invocation -> invocation.getArgument(0));

            SubmitRequest request = new SubmitRequest(
                    1L, 1L, ProblemType.MULTIPLE_CHOICE,
                    List.of(3, 4), null, null, false
            );

            // when
            SubmitResponse response = submissionService.submit(request);

            // then
            assertThat(response.answerStatus()).isEqualTo(AnswerStatus.WRONG);
        }

        @Test
        @DisplayName("힌트 사용 시 통계 업데이트 하지 않음")
        void shouldNotUpdateStatsWhenHintUsed() {
            // given
            given(userService.findById(1L)).willReturn(testUser);
            given(problemService.findById(1L)).willReturn(testProblem);
            given(submissionRepository.save(any(Submission.class))).willAnswer(invocation -> invocation.getArgument(0));

            SubmitRequest request = new SubmitRequest(
                    1L, 1L, ProblemType.MULTIPLE_CHOICE,
                    List.of(1, 2), null, null, true
            );

            // when
            submissionService.submit(request);

            // then
            verify(statsService, never()).updateStats(anyLong(), anyBoolean());
        }
    }

    @Nested
    @DisplayName("주관식 정답 판정")
    class SubjectiveJudgement {

        @BeforeEach
        void setUp() {
            testProblem = Problem.builder()
                    .chapter(testChapter)
                    .content("대한민국의 수도는?")
                    .problemType(ProblemType.SUBJECTIVE)
                    .difficulty(Difficulty.LOW)
                    .explanation("대한민국의 수도는 서울입니다.")
                    .build();

            Answer answer = Answer.builder().answerValue("서울").build();
            testProblem.addAnswer(answer);
        }

        @Test
        @DisplayName("정답과 일치하면 CORRECT 반환")
        void shouldReturnCorrectWhenAnswerMatches() {
            // given
            given(userService.findById(1L)).willReturn(testUser);
            given(problemService.findById(1L)).willReturn(testProblem);
            given(submissionRepository.save(any(Submission.class))).willAnswer(invocation -> invocation.getArgument(0));

            SubmitRequest request = new SubmitRequest(
                    1L, 1L, ProblemType.SUBJECTIVE,
                    null, "서울", null, false
            );

            // when
            SubmitResponse response = submissionService.submit(request);

            // then
            assertThat(response.answerStatus()).isEqualTo(AnswerStatus.CORRECT);
        }

        @Test
        @DisplayName("대소문자 무시하고 정답 판정")
        void shouldIgnoreCaseWhenJudging() {
            // given
            testProblem = Problem.builder()
                    .chapter(testChapter)
                    .content("What is the capital of Korea?")
                    .problemType(ProblemType.SUBJECTIVE)
                    .difficulty(Difficulty.LOW)
                    .explanation("Seoul is the capital.")
                    .build();
            Answer answer = Answer.builder().answerValue("Seoul").build();
            testProblem.addAnswer(answer);

            given(userService.findById(1L)).willReturn(testUser);
            given(problemService.findById(1L)).willReturn(testProblem);
            given(submissionRepository.save(any(Submission.class))).willAnswer(invocation -> invocation.getArgument(0));

            SubmitRequest request = new SubmitRequest(
                    1L, 1L, ProblemType.SUBJECTIVE,
                    null, "SEOUL", null, false
            );

            // when
            SubmitResponse response = submissionService.submit(request);

            // then
            assertThat(response.answerStatus()).isEqualTo(AnswerStatus.CORRECT);
        }

        @Test
        @DisplayName("정답과 일치하지 않으면 WRONG 반환")
        void shouldReturnWrongWhenAnswerDoesNotMatch() {
            // given
            given(userService.findById(1L)).willReturn(testUser);
            given(problemService.findById(1L)).willReturn(testProblem);
            given(submissionRepository.save(any(Submission.class))).willAnswer(invocation -> invocation.getArgument(0));

            SubmitRequest request = new SubmitRequest(
                    1L, 1L, ProblemType.SUBJECTIVE,
                    null, "부산", null, false
            );

            // when
            SubmitResponse response = submissionService.submit(request);

            // then
            assertThat(response.answerStatus()).isEqualTo(AnswerStatus.WRONG);
        }

        @Test
        @DisplayName("빈 답안 제출 시 WRONG 반환")
        void shouldReturnWrongWhenAnswerIsEmpty() {
            // given
            given(userService.findById(1L)).willReturn(testUser);
            given(problemService.findById(1L)).willReturn(testProblem);
            given(submissionRepository.save(any(Submission.class))).willAnswer(invocation -> invocation.getArgument(0));

            SubmitRequest request = new SubmitRequest(
                    1L, 1L, ProblemType.SUBJECTIVE,
                    null, "", null, false
            );

            // when
            SubmitResponse response = submissionService.submit(request);

            // then
            assertThat(response.answerStatus()).isEqualTo(AnswerStatus.WRONG);
        }
    }

    @Nested
    @DisplayName("제출 시간 기록")
    class TimeSpentRecording {

        @BeforeEach
        void setUp() {
            testProblem = Problem.builder()
                    .chapter(testChapter)
                    .content("테스트 문제")
                    .problemType(ProblemType.SUBJECTIVE)
                    .difficulty(Difficulty.LOW)
                    .explanation("해설")
                    .build();
            Answer answer = Answer.builder().answerValue("정답").build();
            testProblem.addAnswer(answer);
        }

        @Test
        @DisplayName("제출 시 소요 시간이 저장됨")
        void shouldRecordTimeSpent() {
            // given
            given(userService.findById(1L)).willReturn(testUser);
            given(problemService.findById(1L)).willReturn(testProblem);

            ArgumentCaptor<Submission> submissionCaptor = ArgumentCaptor.forClass(Submission.class);
            given(submissionRepository.save(submissionCaptor.capture())).willAnswer(invocation -> invocation.getArgument(0));

            SubmitRequest request = new SubmitRequest(
                    1L, 1L, ProblemType.SUBJECTIVE,
                    null, "정답", 120L, false
            );

            // when
            submissionService.submit(request);

            // then
            Submission savedSubmission = submissionCaptor.getValue();
            assertThat(savedSubmission.getTimeSpentSeconds()).isEqualTo(120L);
        }
    }

    @Nested
    @DisplayName("문제 넘기기")
    class SkipProblemHandling {

        @BeforeEach
        void setUp() {
            testProblem = Problem.builder()
                    .chapter(testChapter)
                    .content("건너뛸 문제")
                    .problemType(ProblemType.SUBJECTIVE)
                    .difficulty(Difficulty.LOW)
                    .explanation("해설")
                    .build();
        }

        @Test
        @DisplayName("같은 단원 이전 skip 기록을 지우고 마지막 1건만 남긴다")
        void shouldReplacePreviousSkipStateWithinChapter() {
            given(userService.findById(1L)).willReturn(testUser);
            given(problemService.findById(1L)).willReturn(testProblem);
            given(chapterService.findById(1L)).willReturn(testChapter);
            given(skippedProblemRepository.save(any(SkippedProblem.class))).willAnswer(invocation -> invocation.getArgument(0));

            SkipRequest request = new SkipRequest(1L, 1L, 1L);

            submissionService.skipProblem(request);

            verify(skippedProblemRepository).deleteByUserIdAndChapterId(1L, 1L);
            verify(skippedProblemRepository).save(any(SkippedProblem.class));
        }
    }
}
