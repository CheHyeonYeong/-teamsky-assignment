package com.teamsky.learning.stats;

import com.teamsky.learning.problem.entity.Problem;
import com.teamsky.learning.stats.entity.ProblemStats;
import com.teamsky.learning.stats.entity.UserChapterSubmissionStats;
import com.teamsky.learning.stats.entity.UserSubmissionStats;
import com.teamsky.learning.stats.response.ChapterStatsResponse;
import com.teamsky.learning.stats.response.UserStatsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatsService {

    private final ProblemStatsRepository problemStatsRepository;
    private final UserSubmissionStatsRepository userSubmissionStatsRepository;
    private final UserChapterSubmissionStatsRepository userChapterSubmissionStatsRepository;

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

    @Transactional
    public void updateSubmissionStats(Long userId, Long chapterId, boolean isCorrect) {
        int correctIncrement = isCorrect ? 1 : 0;
        userSubmissionStatsRepository.upsertSubmissionStats(userId, correctIncrement);
        userChapterSubmissionStatsRepository.upsertSubmissionStats(userId, chapterId, correctIncrement);
    }

    public ChapterStatsResponse getChapterStats(Long userId, Long chapterId) {
        UserChapterSubmissionStats stats = userChapterSubmissionStatsRepository
                .findByUser_IdAndChapter_Id(userId, chapterId)
                .orElse(null);

        long totalSubmissions = stats != null ? stats.getTotalSubmissions() : 0L;
        long correctSubmissions = stats != null ? stats.getCorrectSubmissions() : 0L;

        Integer correctRate = totalSubmissions > 0
                ? (int) Math.round((double) correctSubmissions / totalSubmissions * 100)
                : null;

        return new ChapterStatsResponse(chapterId, totalSubmissions, correctSubmissions, correctRate);
    }

    public UserStatsResponse getUserStats(Long userId) {
        UserSubmissionStats stats = userSubmissionStatsRepository.findByUser_Id(userId).orElse(null);

        long totalSubmissions = stats != null ? stats.getTotalSubmissions() : 0L;
        long correctSubmissions = stats != null ? stats.getCorrectSubmissions() : 0L;

        Integer correctRate = totalSubmissions > 0
                ? (int) Math.round((double) correctSubmissions / totalSubmissions * 100)
                : null;

        return new UserStatsResponse(userId, totalSubmissions, correctSubmissions, correctRate);
    }
}
