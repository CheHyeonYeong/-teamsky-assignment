package com.teamsky.learning.problem;

import com.teamsky.learning.chapter.entity.Chapter;
import com.teamsky.learning.common.config.JpaConfig;
import com.teamsky.learning.problem.entity.Difficulty;
import com.teamsky.learning.problem.entity.Problem;
import com.teamsky.learning.problem.entity.ProblemType;
import com.teamsky.learning.submission.entity.AnswerStatus;
import com.teamsky.learning.submission.entity.Submission;
import com.teamsky.learning.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@DisplayName("ProblemRepository MySQL 통합 테스트")
class ProblemRepositoryTest {

    @Autowired
    private ProblemRepository problemRepository;

    @Autowired
    private TestEntityManager entityManager;

    private int nextOrderNum = 1;

    @Test
    @DisplayName("랜덤 조회는 풀이한 문제와 마지막으로 넘긴 문제를 제외하고 난이도 필터를 적용한다")
    void shouldReturnOnlyAvailableProblemIds() {
        User user = persistUser("hong");
        Chapter chapter = persistChapter("1장");
        Problem candidate = persistProblem(chapter, "대상 문제", Difficulty.LOW);
        Problem solved = persistProblem(chapter, "풀이한 문제", Difficulty.LOW);
        Problem skipped = persistProblem(chapter, "방금 넘긴 문제", Difficulty.LOW);
        persistProblem(chapter, "다른 난이도 문제", Difficulty.MEDIUM);
        persistSubmission(user, solved, AnswerStatus.CORRECT);
        entityManager.flush();
        entityManager.clear();

        List<Long> result = problemRepository.findRandomAvailableProblemIds(
                chapter.getId(),
                user.getId(),
                Difficulty.LOW,
                skipped.getId(),
                PageRequest.of(0, 10)
        );

        assertThat(result).containsExactly(candidate.getId());
    }

    @Test
    @DisplayName("오답 재풀이 조회는 해당 단원의 오답과 부분정답만 반환한다")
    void shouldReturnWrongProblemIdsWithinChapter() {
        User user = persistUser("kim");
        Chapter chapter = persistChapter("2장");
        Chapter otherChapter = persistChapter("3장");
        Problem wrong = persistProblem(chapter, "오답 문제", Difficulty.LOW);
        Problem partial = persistProblem(chapter, "부분정답 문제", Difficulty.MEDIUM);
        Problem correct = persistProblem(chapter, "정답 문제", Difficulty.HIGH);
        Problem otherChapterWrong = persistProblem(otherChapter, "다른 단원 오답 문제", Difficulty.LOW);
        persistSubmission(user, wrong, AnswerStatus.WRONG);
        persistSubmission(user, partial, AnswerStatus.PARTIAL);
        persistSubmission(user, correct, AnswerStatus.CORRECT);
        persistSubmission(user, otherChapterWrong, AnswerStatus.WRONG);
        entityManager.flush();
        entityManager.clear();

        List<Long> result = problemRepository.findRandomWrongProblemIdsByUserIdAndChapterId(
                user.getId(),
                chapter.getId(),
                List.of(AnswerStatus.WRONG, AnswerStatus.PARTIAL),
                PageRequest.of(0, 10)
        );

        assertThat(result).containsExactlyInAnyOrder(wrong.getId(), partial.getId());
    }

    private User persistUser(String name) {
        return entityManager.persistAndFlush(User.builder()
                .name(name)
                .email(name + "@example.com")
                .build());
    }

    private Chapter persistChapter(String name) {
        return entityManager.persistAndFlush(Chapter.builder()
                .name(name)
                .description(name + " 설명")
                .orderNum(nextOrderNum++)
                .build());
    }

    private Problem persistProblem(Chapter chapter, String content, Difficulty difficulty) {
        return entityManager.persistAndFlush(Problem.builder()
                .chapter(chapter)
                .content(content)
                .problemType(ProblemType.SUBJECTIVE)
                .difficulty(difficulty)
                .explanation(content + " 해설")
                .hint(content + " 힌트")
                .build());
    }

    private Submission persistSubmission(User user, Problem problem, AnswerStatus status) {
        return entityManager.persistAndFlush(Submission.builder()
                .user(user)
                .problem(problem)
                .answerStatus(status)
                .userAnswer("답안")
                .timeSpentSeconds(10L)
                .hintUsed(false)
                .build());
    }
}
