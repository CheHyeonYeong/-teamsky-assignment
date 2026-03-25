package com.teamsky.learning.submission;

import com.teamsky.learning.submission.request.SkipRequest;
import com.teamsky.learning.submission.request.SubmitRequest;
import com.teamsky.learning.submission.response.SubmissionDetailResponse;
import com.teamsky.learning.submission.response.SubmissionHistoryResponse;
import com.teamsky.learning.submission.response.SubmitResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Submission", description = "문제 제출 관련 API")
@RestController
@RequestMapping("/api/v1/submissions")
@RequiredArgsConstructor
public class SubmissionController {

    private final SubmissionService submissionService;

    @Operation(summary = "문제 제출", description = "문제에 대한 답안을 제출하고 정답 여부를 확인합니다.")
    @PostMapping
    public ResponseEntity<SubmitResponse> submit(@Valid @RequestBody SubmitRequest request) {
        SubmitResponse response = submissionService.submit(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "문제 넘기기", description = "현재 문제를 건너뜁니다.")
    @PostMapping("/skip")
    public ResponseEntity<Void> skip(@Valid @RequestBody SkipRequest request) {
        submissionService.skipProblem(request);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "풀이 상세 조회", description = "특정 문제에 대한 자신의 풀이 이력을 상세 조회합니다.")
    @GetMapping("/detail")
    public ResponseEntity<SubmissionDetailResponse> getDetail(
            @RequestParam Long userId,
            @RequestParam Long problemId) {
        SubmissionDetailResponse response = submissionService.getSubmissionDetail(userId, problemId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "풀이 이력 목록", description = "단원별 풀이 이력을 페이징하여 조회합니다.")
    @GetMapping("/history")
    public ResponseEntity<Page<SubmissionHistoryResponse>> getHistory(
            @RequestParam Long userId,
            @RequestParam Long chapterId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<SubmissionHistoryResponse> response = submissionService.getSubmissionHistory(userId, chapterId, pageable);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "오답 노트", description = "틀린 문제 목록을 조회합니다.")
    @GetMapping("/wrong")
    public ResponseEntity<Page<SubmissionHistoryResponse>> getWrongSubmissions(
            @RequestParam Long userId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<SubmissionHistoryResponse> response = submissionService.getWrongSubmissions(userId, pageable);
        return ResponseEntity.ok(response);
    }
}
