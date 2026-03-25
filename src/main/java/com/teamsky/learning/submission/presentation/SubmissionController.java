package com.teamsky.learning.submission.presentation;

import com.teamsky.learning.shared.presentation.PageResponse;
import com.teamsky.learning.submission.application.SubmissionService;
import com.teamsky.learning.submission.presentation.request.SkipRequest;
import com.teamsky.learning.submission.presentation.request.SubmitRequest;
import com.teamsky.learning.submission.presentation.response.SubmissionDetailResponse;
import com.teamsky.learning.submission.presentation.response.SubmissionHistoryResponse;
import com.teamsky.learning.submission.presentation.response.SubmitResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Submission", description = "Submission APIs")
@RestController
@RequestMapping("/api/v1/submissions")
@RequiredArgsConstructor
public class SubmissionController {

    private final SubmissionService submissionService;

    @Operation(summary = "Submit answer", description = "Submit an answer and return grading result immediately.")
    @PostMapping
    public ResponseEntity<SubmitResponse> submit(@Valid @RequestBody SubmitRequest request) {
        SubmitResponse response = submissionService.submit(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Skip problem", description = "Skip the current problem.")
    @PostMapping("/skip")
    public ResponseEntity<Void> skip(@Valid @RequestBody SkipRequest request) {
        submissionService.skipProblem(request);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Get submission detail", description = "Get the latest submission detail for a problem.")
    @GetMapping("/detail")
    public ResponseEntity<SubmissionDetailResponse> getDetail(
            @RequestParam Long userId,
            @RequestParam Long problemId) {
        SubmissionDetailResponse response = submissionService.getSubmissionDetail(userId, problemId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get submission history", description = "Get paged submission history for a chapter.")
    @GetMapping("/history")
    public ResponseEntity<PageResponse<SubmissionHistoryResponse>> getHistory(
            @RequestParam Long userId,
            @RequestParam Long chapterId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.of(submissionService.getSubmissionHistory(userId, chapterId, pageable)));
    }

    @Operation(summary = "Get wrong submissions", description = "Get paged wrong submission history.")
    @GetMapping("/wrong")
    public ResponseEntity<PageResponse<SubmissionHistoryResponse>> getWrongSubmissions(
            @RequestParam Long userId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.of(submissionService.getWrongSubmissions(userId, pageable)));
    }
}