package com.teamsky.learning.stats.application;

import com.teamsky.learning.chapter.application.ChapterService;
import com.teamsky.learning.stats.domain.ProblemStats;
import com.teamsky.learning.stats.domain.UserChapterSubmissionStats;
import com.teamsky.learning.stats.domain.UserSubmissionStats;
import com.teamsky.learning.stats.infrastructure.ProblemStatsRepository;
import com.teamsky.learning.stats.infrastructure.UserChapterSubmissionStatsRepository;
import com.teamsky.learning.stats.infrastructure.UserSubmissionStatsRepository;
import com.teamsky.learning.user.application.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("StatsService tests")
class StatsServiceTest {

    @InjectMocks
    private StatsService statsService;

    @Mock
    private ProblemStatsRepository problemStatsRepository;

    @Mock
    private UserSubmissionStatsRepository userSubmissionStatsRepository;

    @Mock
    private UserChapterSubmissionStatsRepository userChapterSubmissionStatsRepository;

    @Mock
    private UserService userService;

    @Mock
    private ChapterService chapterService;

    @Nested
    @DisplayName("Correct rate")
    class CalculateCorrectRate {

        @Test
        @DisplayName("returns a rounded rate when the submission count is at least 30")
        void shouldReturnCorrectRateWhenOver30Submissions() {
            ProblemStats stats = new TestProblemStats(35L, 23L);
            given(problemStatsRepository.findByProblemId(1L)).willReturn(Optional.of(stats));

            Integer correctRate = statsService.calculateCorrectRate(1L);

            assertThat(correctRate).isEqualTo(66);
        }

        @Test
        @DisplayName("returns null when the submission count is below 30")
        void shouldReturnNullWhenUnder30Submissions() {
            ProblemStats stats = new TestProblemStats(25L, 20L);
            given(problemStatsRepository.findByProblemId(1L)).willReturn(Optional.of(stats));

            Integer correctRate = statsService.calculateCorrectRate(1L);

            assertThat(correctRate).isNull();
        }

        @Test
        @DisplayName("returns null when no stats row exists")
        void shouldReturnNullWhenNoStats() {
            given(problemStatsRepository.findByProblemId(1L)).willReturn(Optional.empty());

            Integer correctRate = statsService.calculateCorrectRate(1L);

            assertThat(correctRate).isNull();
        }
    }

    @Nested
    @DisplayName("Problem stats")
    class UpdateProblemStats {

        @Test
        @DisplayName("increments both total and correct counts for a correct first submission")
        void shouldIncrementBothCountsWhenCorrect() {
            given(problemStatsRepository.incrementTotalCount(1L)).willReturn(1);

            statsService.updateProblemStats(1L, true, true);

            verify(problemStatsRepository).incrementTotalCount(1L);
            verify(problemStatsRepository).incrementCorrectCount(1L);
        }

        @Test
        @DisplayName("increments only total count for a wrong first submission")
        void shouldIncrementOnlyTotalCountWhenWrong() {
            given(problemStatsRepository.incrementTotalCount(1L)).willReturn(1);

            statsService.updateProblemStats(1L, false, true);

            verify(problemStatsRepository).incrementTotalCount(1L);
            verify(problemStatsRepository, never()).incrementCorrectCount(1L);
        }

        @Test
        @DisplayName("skips updates for repeat submissions from the same user")
        void shouldSkipRepeatUserSubmissions() {
            statsService.updateProblemStats(1L, true, false);

            verify(problemStatsRepository, never()).incrementTotalCount(1L);
            verify(problemStatsRepository, never()).incrementCorrectCount(1L);
        }

        @Test
        @DisplayName("returns when the problem stats row does not exist")
        void shouldReturnWhenStatsRowDoesNotExist() {
            given(problemStatsRepository.incrementTotalCount(1L)).willReturn(0);

            statsService.updateProblemStats(1L, true, true);

            verify(problemStatsRepository).incrementTotalCount(1L);
            verify(problemStatsRepository, never()).incrementCorrectCount(1L);
        }
    }

    @Test
    @DisplayName("upserts user and chapter submission stats")
    void shouldUpdateSubmissionStats() {
        statsService.updateSubmissionStats(1L, 2L, true);

        verify(userSubmissionStatsRepository).upsertSubmissionStats(1L, 1);
        verify(userChapterSubmissionStatsRepository).upsertSubmissionStats(1L, 2L, 1);
    }

    @Test
    @DisplayName("returns user stats from the aggregate row")
    void shouldCalculateUserStats() {
        UserSubmissionStats stats = UserSubmissionStats.builder()
                .totalSubmissions(8L)
                .correctSubmissions(6L)
                .build();
        given(userSubmissionStatsRepository.findByUser_Id(1L)).willReturn(Optional.of(stats));

        var response = statsService.getUserStats(1L);

        verify(userService).validateUserExists(1L);
        assertThat(response.totalSubmissions()).isEqualTo(8L);
        assertThat(response.correctSubmissions()).isEqualTo(6L);
        assertThat(response.correctRate()).isEqualTo(75);
    }

    @Test
    @DisplayName("returns chapter stats from the aggregate row")
    void shouldCalculateChapterStatsWithinChapterBoundary() {
        UserChapterSubmissionStats stats = UserChapterSubmissionStats.builder()
                .totalSubmissions(5L)
                .correctSubmissions(3L)
                .build();
        given(userChapterSubmissionStatsRepository.findByUser_IdAndChapter_Id(1L, 2L))
                .willReturn(Optional.of(stats));

        var response = statsService.getChapterStats(1L, 2L);

        verify(userService).validateUserExists(1L);
        verify(chapterService).validateChapterExists(2L);
        assertThat(response.totalSubmissions()).isEqualTo(5L);
        assertThat(response.correctSubmissions()).isEqualTo(3L);
        assertThat(response.correctRate()).isEqualTo(60);
    }

    @Test
    @DisplayName("returns zeroed user stats when no aggregate row exists")
    void shouldReturnEmptyUserStatsWhenNoRowExists() {
        given(userSubmissionStatsRepository.findByUser_Id(1L)).willReturn(Optional.empty());

        var response = statsService.getUserStats(1L);

        verify(userService).validateUserExists(1L);
        assertThat(response.totalSubmissions()).isZero();
        assertThat(response.correctSubmissions()).isZero();
        assertThat(response.correctRate()).isNull();
    }

    private static class TestProblemStats extends ProblemStats {
        private final Long totalCount;
        private final Long correctCount;

        TestProblemStats(Long totalCount, Long correctCount) {
            this.totalCount = totalCount;
            this.correctCount = correctCount;
        }

        @Override
        public Integer calculateCorrectRate() {
            if (totalCount < 30) {
                return null;
            }
            double rate = (double) correctCount / totalCount * 100;
            return (int) Math.round(rate);
        }
    }
}

