package com.teamsky.learning.submission.event;

import com.teamsky.learning.stats.StatsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubmissionStatsUpdateEventHandler {

    private final StatsService statsService;

    @Async("submissionStatsExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(SubmissionStatsUpdateRequestedEvent event) {
        try {
            statsService.updateSubmissionStats(event.userId(), event.chapterId(), event.correct());
            statsService.updateProblemStats(event.problemId(), event.correct(), event.firstProblemAttempt());
        } catch (Exception exception) {
            log.error(
                    "Failed to update submission stats asynchronously. userId={}, chapterId={}, problemId={}",
                    event.userId(),
                    event.chapterId(),
                    event.problemId(),
                    exception
            );
        }
    }
}
