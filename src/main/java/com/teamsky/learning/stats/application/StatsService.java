package com.teamsky.learning.stats.application;

import com.teamsky.learning.chapter.application.ChapterService;
import com.teamsky.learning.problem.domain.Problem;
import com.teamsky.learning.stats.domain.ProblemStats;
import com.teamsky.learning.stats.domain.UserChapterSubmissionStats;
import com.teamsky.learning.stats.domain.UserSubmissionStats;
import com.teamsky.learning.stats.infrastructure.ProblemStatsRepository;
import com.teamsky.learning.stats.infrastructure.UserChapterSubmissionStatsRepository;
import com.teamsky.learning.stats.infrastructure.UserSubmissionStatsRepository;
import com.teamsky.learning.stats.presentation.response.ChapterStatsResponse;
import com.teamsky.learning.stats.presentation.response.UserStatsResponse;
import com.teamsky.learning.user.application.UserService;
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
    private final UserService userService;
    private final ChapterService chapterService;

    public Integer calculateCorrectRate(Long problemId) {
        return problemStatsRepository.findByProblemId(problemId)
                .map(ProblemStats::calculateCorrectRate)
                .orElse(null);
    }

    @Transactional
    public void updateProblemStats(Long problemId, boolean isCorrect, boolean isFirstProblemAttempt) {
        if (!isFirstProblemAttempt) {
            return;
        }

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
        userService.validateUserExists(userId);
        chapterService.validateChapterExists(chapterId);

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
        userService.validateUserExists(userId);

        UserSubmissionStats stats = userSubmissionStatsRepository.findByUser_Id(userId).orElse(null);

        long totalSubmissions = stats != null ? stats.getTotalSubmissions() : 0L;
        long correctSubmissions = stats != null ? stats.getCorrectSubmissions() : 0L;

        Integer correctRate = totalSubmissions > 0
                ? (int) Math.round((double) correctSubmissions / totalSubmissions * 100)
                : null;

        return new UserStatsResponse(userId, totalSubmissions, correctSubmissions, correctRate);
    }
}

