package com.teamsky.learning.problem;

import com.teamsky.learning.chapter.ChapterService;
import com.teamsky.learning.common.exception.BusinessException;
import com.teamsky.learning.common.exception.ErrorCode;
import com.teamsky.learning.problem.entity.Difficulty;
import com.teamsky.learning.problem.entity.Problem;
import com.teamsky.learning.problem.request.RandomProblemRequest;
import com.teamsky.learning.problem.response.ProblemResponse;
import com.teamsky.learning.stats.StatsService;
import com.teamsky.learning.submission.UserChapterStateRepository;
import com.teamsky.learning.submission.entity.AnswerStatus;
import com.teamsky.learning.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProblemService {

    private final ProblemRepository problemRepository;
    private final UserChapterStateRepository userChapterStateRepository;
    private final UserService userService;
    private final ChapterService chapterService;
    private final StatsService statsService;

    private static final List<AnswerStatus> WRONG_STATUSES = List.of(AnswerStatus.WRONG, AnswerStatus.PARTIAL);

    public ProblemResponse getRandomProblem(RandomProblemRequest request) {
        userService.validateUserExists(request.userId());
        chapterService.validateChapterExists(request.chapterId());

        Long lastSkippedProblemId = userChapterStateRepository
                .findLastSkippedProblemId(request.userId(), request.chapterId())
                .orElse(null);

        long totalAvailableProblems = problemRepository.countAvailableProblems(
                request.chapterId(),
                request.userId(),
                request.difficulty(),
                lastSkippedProblemId
        );

        Long selectedProblemId = selectRandomProblemId(
                totalAvailableProblems,
                randomIndex -> problemRepository.findAvailableProblemIds(
                        request.chapterId(),
                        request.userId(),
                        request.difficulty(),
                        lastSkippedProblemId,
                        PageRequest.of(randomIndex, 1)
                )
        );

        Problem selectedProblem = findById(selectedProblemId);
        Integer correctRate = statsService.calculateCorrectRate(selectedProblem.getId());

        return ProblemResponse.of(selectedProblem, correctRate);
    }

    public ProblemResponse getRandomWrongProblem(Long userId, Long chapterId) {
        userService.validateUserExists(userId);
        chapterService.validateChapterExists(chapterId);

        long totalWrongProblems = problemRepository.countWrongProblemIdsByUserIdAndChapterId(
                userId,
                chapterId,
                WRONG_STATUSES
        );

        Long selectedProblemId = selectRandomProblemId(
                totalWrongProblems,
                randomIndex -> problemRepository.findWrongProblemIdsByUserIdAndChapterId(
                        userId,
                        chapterId,
                        WRONG_STATUSES,
                        PageRequest.of(randomIndex, 1)
                )
        );

        Problem problem = findById(selectedProblemId);
        Integer correctRate = statsService.calculateCorrectRate(problem.getId());

        return ProblemResponse.of(problem, correctRate);
    }

    public Problem findById(Long problemId) {
        return problemRepository.findById(problemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROBLEM_NOT_FOUND));
    }

    public Problem findByIdInChapter(Long problemId, Long chapterId) {
        return problemRepository.findByIdAndChapter_Id(problemId, chapterId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROBLEM_NOT_FOUND));
    }

    public Problem findByIdWithAnswers(Long problemId) {
        return problemRepository.findByIdWithAnswers(problemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROBLEM_NOT_FOUND));
    }

    private Long selectRandomProblemId(long totalCount, java.util.function.IntFunction<List<Long>> pageFetcher) {
        if (totalCount == 0) {
            throw new BusinessException(ErrorCode.NO_MORE_PROBLEMS);
        }

        int randomIndex = Math.toIntExact(ThreadLocalRandom.current().nextLong(totalCount));

        return pageFetcher.apply(randomIndex).stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.NO_MORE_PROBLEMS));
    }
}
