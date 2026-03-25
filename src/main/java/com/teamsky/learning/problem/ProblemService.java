package com.teamsky.learning.problem;

import com.teamsky.learning.chapter.ChapterService;
import com.teamsky.learning.common.exception.BusinessException;
import com.teamsky.learning.common.exception.ErrorCode;
import com.teamsky.learning.problem.entity.Difficulty;
import com.teamsky.learning.problem.entity.Problem;
import com.teamsky.learning.problem.request.RandomProblemRequest;
import com.teamsky.learning.problem.response.ProblemResponse;
import com.teamsky.learning.stats.StatsService;
import com.teamsky.learning.submission.SkippedProblemRepository;
import com.teamsky.learning.submission.entity.AnswerStatus;
import com.teamsky.learning.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProblemService {

    private final ProblemRepository problemRepository;
    private final SkippedProblemRepository skippedProblemRepository;
    private final UserService userService;
    private final ChapterService chapterService;
    private final StatsService statsService;

    private static final List<AnswerStatus> WRONG_STATUSES = List.of(AnswerStatus.WRONG, AnswerStatus.PARTIAL);

    public ProblemResponse getRandomProblem(RandomProblemRequest request) {
        userService.validateUserExists(request.userId());
        chapterService.validateChapterExists(request.chapterId());

        Long lastSkippedProblemId = skippedProblemRepository
                .findLastSkippedProblemId(request.userId(), request.chapterId())
                .orElse(null);

        Long selectedProblemId = problemRepository.findRandomAvailableProblemIds(
                request.chapterId(),
                request.userId(),
                request.difficulty(),
                lastSkippedProblemId,
                PageRequest.of(0, 1)
        ).stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.NO_MORE_PROBLEMS));

        Problem selectedProblem = findById(selectedProblemId);
        Integer correctRate = statsService.calculateCorrectRate(selectedProblem.getId());

        return ProblemResponse.of(selectedProblem, correctRate);
    }

    public ProblemResponse getRandomWrongProblem(Long userId, Long chapterId) {
        userService.validateUserExists(userId);
        chapterService.validateChapterExists(chapterId);

        Long selectedProblemId = problemRepository.findRandomWrongProblemIdsByUserIdAndChapterId(
                userId,
                chapterId,
                WRONG_STATUSES,
                PageRequest.of(0, 1)
        ).stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.NO_MORE_PROBLEMS));

        Problem problem = findById(selectedProblemId);
        Integer correctRate = statsService.calculateCorrectRate(problem.getId());

        return ProblemResponse.of(problem, correctRate);
    }

    public Problem findById(Long problemId) {
        return problemRepository.findById(problemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROBLEM_NOT_FOUND));
    }
}
