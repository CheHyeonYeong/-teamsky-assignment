package com.teamsky.learning.stats;

import com.teamsky.learning.stats.entity.ProblemStats;
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

@ExtendWith(MockitoExtension.class)
@DisplayName("StatsService 테스트")
class StatsServiceTest {

    @InjectMocks
    private StatsService statsService;

    @Mock
    private ProblemStatsRepository problemStatsRepository;

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
