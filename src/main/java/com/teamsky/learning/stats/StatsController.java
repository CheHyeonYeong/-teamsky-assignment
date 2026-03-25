package com.teamsky.learning.stats;

import com.teamsky.learning.stats.response.ChapterStatsResponse;
import com.teamsky.learning.stats.response.UserStatsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Stats", description = "통계 관련 API")
@RestController
@RequestMapping("/api/v1/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    @Operation(summary = "단원별 통계", description = "특정 단원에 대한 사용자의 풀이 통계를 조회합니다.")
    @GetMapping("/chapters/{chapterId}")
    public ResponseEntity<ChapterStatsResponse> getChapterStats(
            @PathVariable Long chapterId,
            @RequestParam Long userId) {
        ChapterStatsResponse response = statsService.getChapterStats(userId, chapterId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "사용자 전체 통계", description = "사용자의 전체 풀이 통계를 조회합니다.")
    @GetMapping("/users/{userId}")
    public ResponseEntity<UserStatsResponse> getUserStats(@PathVariable Long userId) {
        UserStatsResponse response = statsService.getUserStats(userId);
        return ResponseEntity.ok(response);
    }
}
