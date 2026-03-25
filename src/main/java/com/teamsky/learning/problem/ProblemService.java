package com.teamsky.learning.problem;

import com.teamsky.learning.chapter.ChapterService;
import com.teamsky.learning.common.exception.BusinessException;
import com.teamsky.learning.common.exception.ErrorCode;
import com.teamsky.learning.problem.entity.Difficulty;
import com.teamsky.learning.problem.entity.Problem;
import com.teamsky.learning.problem.request.RandomProblemRequest;
import com.teamsky.learning.problem.response.ProblemResponse;
import com.teamsky.learning.stats.StatsService;
import com.teamsky.learning.submission.SubmissionRepository;
import com.teamsky.learning.submission.SkippedProblemRepository;
import com.teamsky.learning.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProblemService {

    private final ProblemRepository problemRepository;
    private final SubmissionRepository submissionRepository;
    private final SkippedProblemRepository skippedProblemRepository;
    private final UserService userService;
    private final ChapterService chapterService;
    private final StatsService statsService;

    private final Random random = new Random();

    public ProblemResponse getRandomProblem(RandomProblemRequest request) {
        userService.validateUserExists(request.userId());
        chapterService.validateChapterExists(request.chapterId());

        List<Long> excludedIds = getExcludedProblemIds(request.userId(), request.chapterId());

        List<Problem> availableProblems = findAvailableProblems(
                request.chapterId(),
                request.difficulty(),
                excludedIds
        );

        if (availableProblems.isEmpty()) {
            throw new BusinessException(ErrorCode.NO_MORE_PROBLEMS);
        }

        Problem selectedProblem = availableProblems.get(random.nextInt(availableProblems.size()));
        Integer correctRate = statsService.calculateCorrectRate(selectedProblem.getId());

        return ProblemResponse.of(selectedProblem, correctRate);
    }

    public ProblemResponse getRandomWrongProblem(Long userId, Long chapterId) {
        userService.validateUserExists(userId);
        chapterService.validateChapterExists(chapterId);

        List<Long> wrongProblemIds = submissionRepository.findWrongProblemIdsByUserIdAndChapterId(userId, chapterId);

        if (wrongProblemIds.isEmpty()) {
            throw new BusinessException(ErrorCode.NO_MORE_PROBLEMS);
        }

        Long selectedProblemId = wrongProblemIds.get(random.nextInt(wrongProblemIds.size()));
        Problem problem = findById(selectedProblemId);
        Integer correctRate = statsService.calculateCorrectRate(problem.getId());

        return ProblemResponse.of(problem, correctRate);
    }

    public Problem findById(Long problemId) {
        return problemRepository.findById(problemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROBLEM_NOT_FOUND));
    }

    private List<Long> getExcludedProblemIds(Long userId, Long chapterId) {
        List<Long> solvedIds = submissionRepository.findProblemIdsByUserId(userId);
        Long lastSkippedId = skippedProblemRepository
                .findLastSkippedProblemId(userId, chapterId)
                .orElse(null);

        List<Long> excludedIds = new ArrayList<>(solvedIds);
        if (lastSkippedId != null) {
            excludedIds.add(lastSkippedId);
        }

        return excludedIds.isEmpty() ? List.of(-1L) : excludedIds;
    }

    private List<Problem> findAvailableProblems(Long chapterId, Difficulty difficulty, List<Long> excludedIds) {
        if (difficulty != null) {
            return problemRepository.findByChapterIdAndDifficultyExcluding(chapterId, difficulty, excludedIds);
        }
        return problemRepository.findByChapterIdExcluding(chapterId, excludedIds);
    }
}
