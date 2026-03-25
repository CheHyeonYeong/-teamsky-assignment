package com.teamsky.learning.stats;

import com.teamsky.learning.problem.entity.Problem;
import com.teamsky.learning.stats.entity.ProblemStats;
import com.teamsky.learning.stats.response.ChapterStatsResponse;
import com.teamsky.learning.stats.response.UserStatsResponse;
import com.teamsky.learning.submission.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        int updatedRows = problemStatsRepository.incrementTotalCount(problemId);
        if (updatedRows == 0) {
            return;
        }

        if (isCorrect) {
            problemStatsRepository.incrementCorrectCount(problemId);
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
        long totalSubmissions = submissionRepository.countByUserIdAndChapterId(userId, chapterId);
        long correctSubmissions = submissionRepository.countCorrectByUserIdAndChapterId(userId, chapterId);

        Integer correctRate = totalSubmissions > 0
                ? (int) Math.round((double) correctSubmissions / totalSubmissions * 100)
                : null;

        return new ChapterStatsResponse(chapterId, totalSubmissions, correctSubmissions, correctRate);
    }

    public UserStatsResponse getUserStats(Long userId) {
        long totalSubmissions = submissionRepository.countByUserId(userId);
        long correctSubmissions = submissionRepository.countCorrectByUserId(userId);

        Integer correctRate = totalSubmissions > 0
                ? (int) Math.round((double) correctSubmissions / totalSubmissions * 100)
                : null;

        return new UserStatsResponse(userId, totalSubmissions, correctSubmissions, correctRate);
    }
}
