package com.teamsky.learning.problem;

import com.teamsky.learning.problem.ProblemService;
import com.teamsky.learning.problem.request.RandomProblemRequest;
import com.teamsky.learning.problem.response.ProblemResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Problem", description = "Problem APIs")
@RestController
@RequestMapping("/api/v1/problems")
@RequiredArgsConstructor
public class ProblemController {

    private final ProblemService problemService;

    @Operation(summary = "Get random problem", description = "Return one random unsolved problem from the selected chapter.")
    @GetMapping("/random")
    public ResponseEntity<ProblemResponse> getRandomProblem(@Valid @ModelAttribute RandomProblemRequest request) {
        ProblemResponse response = problemService.getRandomProblem(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get random wrong problem", description = "Return one random currently wrong problem from the selected chapter.")
    @GetMapping("/random/wrong")
    public ResponseEntity<ProblemResponse> getRandomWrongProblem(
            @RequestParam Long userId,
            @RequestParam Long chapterId) {
        ProblemResponse response = problemService.getRandomWrongProblem(userId, chapterId);
        return ResponseEntity.ok(response);
    }
}