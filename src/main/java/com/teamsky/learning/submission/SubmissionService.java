package com.teamsky.learning.submission;

import com.teamsky.learning.chapter.ChapterService;
import com.teamsky.learning.common.exception.BusinessException;
import com.teamsky.learning.common.exception.ErrorCode;
import com.teamsky.learning.problem.ProblemService;
import com.teamsky.learning.problem.entity.Answer;
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
import com.teamsky.learning.submission.response.SubmissionDetailResponse;
import com.teamsky.learning.submission.response.SubmissionHistoryResponse;
import com.teamsky.learning.submission.response.SubmitResponse;
import com.teamsky.learning.user.UserService;
import com.teamsky.learning.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final UserProblemStateRepository userProblemStateRepository;
    private final UserChapterStateRepository userChapterStateRepository;
    private final UserService userService;
    private final ProblemService problemService;
    private final ChapterService chapterService;
    private final StatsService statsService;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Transactional
    public SubmitResponse submit(SubmitRequest request) {
        User user = userService.findById(request.userId());
        Problem problem = problemService.findByIdWithAnswers(request.problemId());

        AnswerStatus status = judgeAnswer(problem, request);

        Submission submission = Submission.builder()
                .user(user)
                .problem(problem)
                .answerStatus(status)
                .userAnswer(request.getUserAnswerAsString())
                .timeSpentSeconds(request.timeSpentSeconds())
                .hintUsed(request.hintUsed())
                .build();

        Submission savedSubmission = submissionRepository.save(submission);
        int updatedRows = userProblemStateRepository.upsertSubmissionState(
                user.getId(),
                problem.getId(),
                savedSubmission.getId(),
                status.name(),
                status == AnswerStatus.CORRECT
        );
        boolean isFirstProblemAttempt = updatedRows == 1;

        applicationEventPublisher.publishEvent(new SubmissionStatsUpdateRequestedEvent(
                user.getId(),
                problem.getChapter().getId(),
                problem.getId(),
                status == AnswerStatus.CORRECT,
                isFirstProblemAttempt
        ));

        return SubmitResponse.of(savedSubmission);
    }

    @Transactional
    public void skipProblem(SkipRequest request) {
        userService.validateUserExists(request.userId());
        chapterService.validateChapterExists(request.chapterId());
        Problem problem = problemService.findByIdInChapter(request.problemId(), request.chapterId());

        userChapterStateRepository.upsertLastSkippedProblem(
                request.userId(),
                request.chapterId(),
                problem.getId()
        );
    }

    public SubmissionDetailResponse getSubmissionDetail(Long userId, Long problemId) {
        userService.validateUserExists(userId);

        Submission submission = submissionRepository.findFirstByUser_IdAndProblem_IdOrderByCreatedAtDescIdDesc(userId, problemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SUBMISSION_NOT_FOUND));

        Integer correctRate = statsService.calculateCorrectRate(problemId);

        return SubmissionDetailResponse.of(submission, correctRate);
    }

    public Page<SubmissionHistoryResponse> getSubmissionHistory(Long userId, Long chapterId, Pageable pageable) {
        userService.validateUserExists(userId);
        chapterService.validateChapterExists(chapterId);

        return submissionRepository.findHistoryResponsesByUserIdAndChapterId(userId, chapterId, pageable);
    }

    public Page<SubmissionHistoryResponse> getWrongSubmissions(Long userId, Pageable pageable) {
        userService.validateUserExists(userId);

        List<AnswerStatus> wrongStatuses = List.of(AnswerStatus.WRONG, AnswerStatus.PARTIAL);

        return submissionRepository.findWrongSubmissionResponsesByUserId(userId, wrongStatuses, pageable);
    }

    private AnswerStatus judgeAnswer(Problem problem, SubmitRequest request) {
        List<String> correctAnswers = problem.getAnswers().stream()
                .map(Answer::getAnswerValue)
                .toList();

        if (problem.getProblemType() == ProblemType.SUBJECTIVE) {
            return judgeSubjectiveAnswer(correctAnswers, request.subjectiveAnswer());
        }

        return judgeMultipleChoiceAnswer(correctAnswers, request.multipleChoiceAnswers());
    }

    private AnswerStatus judgeSubjectiveAnswer(List<String> correctAnswers, String userAnswer) {
        if (userAnswer == null || userAnswer.isBlank()) {
            return AnswerStatus.WRONG;
        }

        String normalizedUserAnswer = userAnswer.trim().toLowerCase();

        boolean isCorrect = correctAnswers.stream()
                .anyMatch(answer -> answer.trim().toLowerCase().equals(normalizedUserAnswer));

        return isCorrect ? AnswerStatus.CORRECT : AnswerStatus.WRONG;
    }

    private AnswerStatus judgeMultipleChoiceAnswer(List<String> correctAnswers, List<Integer> userAnswers) {
        if (userAnswers == null || userAnswers.isEmpty()) {
            return AnswerStatus.WRONG;
        }

        Set<Integer> correctSet = correctAnswers.stream()
                .flatMap(answer -> Arrays.stream(answer.split(",")))
                .map(String::trim)
                .map(Integer::parseInt)
                .collect(Collectors.toSet());

        Set<Integer> userSet = userAnswers.stream().collect(Collectors.toSet());

        if (correctSet.equals(userSet)) {
            return AnswerStatus.CORRECT;
        }

        boolean hasAnyCorrectAnswer = userSet.stream().anyMatch(correctSet::contains);

        return hasAnyCorrectAnswer ? AnswerStatus.PARTIAL : AnswerStatus.WRONG;
    }
}

