package com.teamsky.learning.submission;

import com.teamsky.learning.stats.StatsService;
import com.teamsky.learning.submission.event.SubmissionStatsUpdateEventHandler;
import com.teamsky.learning.submission.event.SubmissionStatsUpdateRequestedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubmissionStatsUpdateEventHandler tests")
class SubmissionStatsUpdateEventHandlerTest {

    @InjectMocks
    private SubmissionStatsUpdateEventHandler handler;

    @Mock
    private StatsService statsService;

    @Test
    @DisplayName("updates submission and problem stats from the event payload")
    void shouldUpdateStatsFromEvent() {
        SubmissionStatsUpdateRequestedEvent event =
                new SubmissionStatsUpdateRequestedEvent(1L, 2L, 3L, true, false);

        handler.handle(event);

        verify(statsService).updateSubmissionStats(1L, 2L, true);
        verify(statsService).updateProblemStats(3L, true, false);
    }
}
