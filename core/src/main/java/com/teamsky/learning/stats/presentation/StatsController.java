package com.teamsky.learning.stats.presentation;

import com.teamsky.learning.stats.application.StatsService;
import com.teamsky.learning.stats.presentation.response.ChapterStatsResponse;
import com.teamsky.learning.stats.presentation.response.UserStatsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Stats", description = "Stats APIs")
@RestController
@RequestMapping("/api/v1/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    @Operation(summary = "Get chapter stats", description = "Get user stats for a chapter.")
    @GetMapping("/chapters/{chapterId}")
    public ResponseEntity<ChapterStatsResponse> getChapterStats(
            @PathVariable Long chapterId,
            @RequestParam Long userId) {
        ChapterStatsResponse response = statsService.getChapterStats(userId, chapterId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get user stats", description = "Get overall user stats.")
    @GetMapping("/users/{userId}")
    public ResponseEntity<UserStatsResponse> getUserStats(@PathVariable Long userId) {
        UserStatsResponse response = statsService.getUserStats(userId);
        return ResponseEntity.ok(response);
    }
}