package com.teamsky.learning.stats;

import com.teamsky.learning.stats.entity.ProblemStats;
import com.teamsky.learning.submission.SubmissionRepository;
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
@DisplayName("StatsService 테스트")
class StatsServiceTest {

    @InjectMocks
    private StatsService statsService;

    @Mock
    private ProblemStatsRepository problemStatsRepository;

    @Mock
    private SubmissionRepository submissionRepository;

    @Nested
    @DisplayName("정답률 계산")
    class CalculateCorrectRate {

        @Test
        @DisplayName("30명 이상 풀면 정답률 반환 (소수점 첫째자리 반올림)")
        void shouldReturnCorrectRateWhenOver30Submissions() {
            // given
            ProblemStats stats = new TestProblemStats(35L, 23L); // 23/35 = 65.71... -> 66%
            given(problemStatsRepository.findByProblemId(1L)).willReturn(Optional.of(stats));

            // when
            Integer correctRate = statsService.calculateCorrectRate(1L);

            // then
            assertThat(correctRate).isEqualTo(66);
        }

        @Test
        @DisplayName("30명 미만이면 null 반환")
        void shouldReturnNullWhenUnder30Submissions() {
            // given
            ProblemStats stats = new TestProblemStats(25L, 20L);
            given(problemStatsRepository.findByProblemId(1L)).willReturn(Optional.of(stats));

            // when
            Integer correctRate = statsService.calculateCorrectRate(1L);

            // then
            assertThat(correctRate).isNull();
        }

        @Test
        @DisplayName("통계가 없으면 null 반환")
        void shouldReturnNullWhenNoStats() {
            // given
            given(problemStatsRepository.findByProblemId(1L)).willReturn(Optional.empty());

            // when
            Integer correctRate = statsService.calculateCorrectRate(1L);

            // then
            assertThat(correctRate).isNull();
        }

        @Test
        @DisplayName("66.7% -> 67% 반올림")
        void shouldRoundUp667To67() {
            // given
            ProblemStats stats = new TestProblemStats(30L, 20L); // 20/30 = 66.67... -> 67%
            given(problemStatsRepository.findByProblemId(1L)).willReturn(Optional.of(stats));

            // when
            Integer correctRate = statsService.calculateCorrectRate(1L);

            // then
            assertThat(correctRate).isEqualTo(67);
        }

        @Test
        @DisplayName("66.4% -> 66% 버림")
        void shouldRoundDown664To66() {
            // given
            ProblemStats stats = new TestProblemStats(500L, 332L); // 332/500 = 66.4% -> 66%
            given(problemStatsRepository.findByProblemId(1L)).willReturn(Optional.of(stats));

            // when
            Integer correctRate = statsService.calculateCorrectRate(1L);

            // then
            assertThat(correctRate).isEqualTo(66);
        }
    }

    @Nested
    @DisplayName("통계 누적")
    class UpdateStats {

        @Test
        @DisplayName("정답 제출이면 total과 correct를 모두 증가")
        void shouldIncrementBothCountsWhenCorrect() {
            // given
            given(problemStatsRepository.incrementTotalCount(1L)).willReturn(1);

            // when
            statsService.updateStats(1L, true);

            // then
            verify(problemStatsRepository).incrementTotalCount(1L);
            verify(problemStatsRepository).incrementCorrectCount(1L);
        }

        @Test
        @DisplayName("오답 제출이면 total만 증가")
        void shouldIncrementOnlyTotalCountWhenWrong() {
            // given
            given(problemStatsRepository.incrementTotalCount(1L)).willReturn(1);

            // when
            statsService.updateStats(1L, false);

            // then
            verify(problemStatsRepository).incrementTotalCount(1L);
            verify(problemStatsRepository, never()).incrementCorrectCount(1L);
        }

        @Test
        @DisplayName("통계 행이 없으면 추가 증가 없이 종료")
        void shouldReturnWhenStatsRowDoesNotExist() {
            // given
            given(problemStatsRepository.incrementTotalCount(1L)).willReturn(0);

            // when
            statsService.updateStats(1L, true);

            // then
            verify(problemStatsRepository).incrementTotalCount(1L);
            verify(problemStatsRepository, never()).incrementCorrectCount(1L);
        }
    }

    @Nested
    @DisplayName("사용자 통계")
    class UserStats {

        @Test
        @DisplayName("제출 수와 정답 수로 사용자 통계를 계산")
        void shouldCalculateUserStats() {
            // given
            given(submissionRepository.countByUserId(1L)).willReturn(8L);
            given(submissionRepository.countCorrectByUserId(1L)).willReturn(6L);

            // when
            var response = statsService.getUserStats(1L);

            // then
            assertThat(response.totalSubmissions()).isEqualTo(8L);
            assertThat(response.correctSubmissions()).isEqualTo(6L);
            assertThat(response.correctRate()).isEqualTo(75);
        }
    }

    @Nested
    @DisplayName("단원 통계")
    class ChapterStats {

        @Test
        @DisplayName("단원 기준으로만 정답률을 계산")
        void shouldCalculateChapterStatsWithinChapterBoundary() {
            // given
            given(submissionRepository.countByUserIdAndChapterId(1L, 2L)).willReturn(5L);
            given(submissionRepository.countCorrectByUserIdAndChapterId(1L, 2L)).willReturn(3L);

            // when
            var response = statsService.getChapterStats(1L, 2L);

            // then
            assertThat(response.totalSubmissions()).isEqualTo(5L);
            assertThat(response.correctSubmissions()).isEqualTo(3L);
            assertThat(response.correctRate()).isEqualTo(60);
        }
    }

    // 테스트용 ProblemStats 클래스
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
