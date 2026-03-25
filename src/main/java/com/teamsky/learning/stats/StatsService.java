package com.teamsky.learning.stats;

import com.teamsky.learning.problem.entity.Problem;
import com.teamsky.learning.stats.entity.ProblemStats;
import com.teamsky.learning.stats.response.ChapterStatsResponse;
import com.teamsky.learning.stats.response.UserStatsResponse;
import com.teamsky.learning.submission.SubmissionRepository;
import com.teamsky.learning.submission.entity.AnswerStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatsService {

    private final ProblemStatsRepository problemStatsRepository;
    private final SubmissionRepository submissionRepository;

    private static final int MIN_SUBMISSION_COUNT = 30;

    public Integer calculateCorrectRate(Long problemId) {
        return problemStatsRepository.findByProblemId(problemId)
                .map(ProblemStats::calculateCorrectRate)
                .orElse(null);
    }

    @Transactional
    public void updateStats(Long problemId, boolean isCorrect) {
        ProblemStats stats = problemStatsRepository.findByProblemId(problemId)
                .orElse(null);

        if (stats == null) {
            return;
        }

        stats.incrementTotal();
        if (isCorrect) {
            stats.incrementCorrect();
        }
    }

    @Transactional
    public void initializeStats(Problem problem) {
        ProblemStats stats = ProblemStats.builder()
                .problem(problem)
                .build();
        problemStatsRepository.save(stats);
    }

    public ChapterStatsResponse getChapterStats(Long userId, Long chapterId) {
        long totalSubmissions = submissionRepository.findByUserIdAndChapterId(userId, chapterId, null)
                .getTotalElements();

        List<AnswerStatus> correctStatuses = List.of(AnswerStatus.CORRECT);
        long correctSubmissions = submissionRepository
                .findByUserIdAndAnswerStatusIn(userId, correctStatuses, null)
                .getTotalElements();

        Integer correctRate = totalSubmissions > 0
                ? (int) Math.round((double) correctSubmissions / totalSubmissions * 100)
                : null;

        return new ChapterStatsResponse(chapterId, totalSubmissions, correctSubmissions, correctRate);
    }

    public UserStatsResponse getUserStats(Long userId) {
        return new UserStatsResponse(userId, 0L, 0L, null);
    }
}
