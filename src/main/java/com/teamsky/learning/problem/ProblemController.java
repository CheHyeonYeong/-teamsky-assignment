package com.teamsky.learning.problem;

import com.teamsky.learning.problem.request.RandomProblemRequest;
import com.teamsky.learning.problem.response.ProblemResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Problem", description = "문제 관련 API")
@RestController
@RequestMapping("/api/v1/problems")
@RequiredArgsConstructor
public class ProblemController {

    private final ProblemService problemService;

    @Operation(summary = "랜덤 문제 조회", description = "해당 단원에서 풀지 않은 문제 중 랜덤으로 1개를 조회합니다.")
    @GetMapping("/random")
    public ResponseEntity<ProblemResponse> getRandomProblem(@Valid @ModelAttribute RandomProblemRequest request) {
        ProblemResponse response = problemService.getRandomProblem(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "오답 문제 재풀이", description = "해당 단원에서 틀린 문제 중 랜덤으로 1개를 조회합니다.")
    @GetMapping("/random/wrong")
    public ResponseEntity<ProblemResponse> getRandomWrongProblem(
            @RequestParam Long userId,
            @RequestParam Long chapterId) {
        ProblemResponse response = problemService.getRandomWrongProblem(userId, chapterId);
        return ResponseEntity.ok(response);
    }
}
