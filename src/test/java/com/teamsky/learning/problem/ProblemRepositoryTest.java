package com.teamsky.learning.problem;

import com.teamsky.learning.chapter.entity.Chapter;
import com.teamsky.learning.config.JpaConfig;
import com.teamsky.learning.problem.entity.Difficulty;
import com.teamsky.learning.problem.entity.Problem;
import com.teamsky.learning.problem.entity.ProblemType;
import com.teamsky.learning.submission.entity.AnswerStatus;
import com.teamsky.learning.submission.entity.Submission;
import com.teamsky.learning.submission.entity.UserProblemState;
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
@DisplayName("ProblemRepository MySQL tests")
class ProblemRepositoryTest {

    @Autowired
    private ProblemRepository problemRepository;

    @Autowired
    private TestEntityManager entityManager;

    private int nextOrderNum = 1;

    @Test
    @DisplayName("available problem query excludes attempted and last skipped problems")
    void shouldReturnOnlyAvailableProblemIds() {
        User user = persistUser("hong");
        Chapter chapter = persistChapter("Chapter 1");
        Problem candidate = persistProblem(chapter, "Candidate", Difficulty.LOW);
        Problem solved = persistProblem(chapter, "Solved", Difficulty.LOW);
        Problem skipped = persistProblem(chapter, "Skipped", Difficulty.LOW);
        persistProblem(chapter, "Different difficulty", Difficulty.MEDIUM);

        Submission solvedSubmission = persistSubmission(user, solved, AnswerStatus.CORRECT, "answer");
        persistUserProblemState(user, solved, solvedSubmission, AnswerStatus.CORRECT, 1L);
        entityManager.flush();
        entityManager.clear();

        long count = problemRepository.countAvailableProblems(
                chapter.getId(),
                user.getId(),
                Difficulty.LOW,
                skipped.getId()
        );

        List<Long> result = problemRepository.findAvailableProblemIds(
                chapter.getId(),
                user.getId(),
                Difficulty.LOW,
                skipped.getId(),
                PageRequest.of(0, 10)
        );

        assertThat(count).isEqualTo(1L);
        assertThat(result).containsExactly(candidate.getId());
    }

    @Test
    @DisplayName("wrong problem query uses only the latest state")
    void shouldReturnWrongProblemIdsWithinChapter() {
        User user = persistUser("kim");
        Chapter chapter = persistChapter("Chapter 2");
        Chapter otherChapter = persistChapter("Chapter 3");
        Problem wrong = persistProblem(chapter, "Wrong", Difficulty.LOW);
        Problem partial = persistProblem(chapter, "Partial", Difficulty.MEDIUM);
        Problem correct = persistProblem(chapter, "Correct", Difficulty.HIGH);
        Problem correctedLater = persistProblem(chapter, "Corrected later", Difficulty.LOW);
        Problem otherChapterWrong = persistProblem(otherChapter, "Other chapter wrong", Difficulty.LOW);

        Submission wrongSubmission = persistSubmission(user, wrong, AnswerStatus.WRONG, "wrong");
        Submission partialSubmission = persistSubmission(user, partial, AnswerStatus.PARTIAL, "partial");
        Submission correctSubmission = persistSubmission(user, correct, AnswerStatus.CORRECT, "correct");
        persistSubmission(user, correctedLater, AnswerStatus.WRONG, "wrong");
        Submission correctedCorrectSubmission = persistSubmission(user, correctedLater, AnswerStatus.CORRECT, "correct");
        Submission otherChapterSubmission = persistSubmission(user, otherChapterWrong, AnswerStatus.WRONG, "wrong");

        persistUserProblemState(user, wrong, wrongSubmission, AnswerStatus.WRONG, 1L);
        persistUserProblemState(user, partial, partialSubmission, AnswerStatus.PARTIAL, 1L);
        persistUserProblemState(user, correct, correctSubmission, AnswerStatus.CORRECT, 1L);
        persistUserProblemState(user, correctedLater, correctedCorrectSubmission, AnswerStatus.CORRECT, 2L);
        persistUserProblemState(user, otherChapterWrong, otherChapterSubmission, AnswerStatus.WRONG, 1L);
        entityManager.flush();
        entityManager.clear();

        long count = problemRepository.countWrongProblemIdsByUserIdAndChapterId(
                user.getId(),
                chapter.getId(),
                List.of(AnswerStatus.WRONG, AnswerStatus.PARTIAL)
        );

        List<Long> result = problemRepository.findWrongProblemIdsByUserIdAndChapterId(
                user.getId(),
                chapter.getId(),
                List.of(AnswerStatus.WRONG, AnswerStatus.PARTIAL),
                PageRequest.of(0, 10)
        );

        assertThat(count).isEqualTo(2L);
        assertThat(result)
                .containsExactlyInAnyOrder(wrong.getId(), partial.getId())
                .doesNotContain(correct.getId(), correctedLater.getId(), otherChapterWrong.getId());
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
                .description(name + " description")
                .orderNum(nextOrderNum++)
                .build());
    }

    private Problem persistProblem(Chapter chapter, String content, Difficulty difficulty) {
        return entityManager.persistAndFlush(Problem.builder()
                .chapter(chapter)
                .content(content)
                .problemType(ProblemType.SUBJECTIVE)
                .difficulty(difficulty)
                .explanation(content + " explanation")
                .hint(content + " hint")
                .build());
    }

    private Submission persistSubmission(User user, Problem problem, AnswerStatus status, String userAnswer) {
        return entityManager.persistAndFlush(Submission.builder()
                .user(user)
                .problem(problem)
                .answerStatus(status)
                .userAnswer(userAnswer)
                .timeSpentSeconds(10L)
                .hintUsed(false)
                .build());
    }

    private UserProblemState persistUserProblemState(User user, Problem problem, Submission lastSubmission,
                                                     AnswerStatus status, long attemptCount) {
        return entityManager.persistAndFlush(UserProblemState.builder()
                .user(user)
                .problem(problem)
                .lastSubmission(lastSubmission)
                .lastAnswerStatus(status)
                .solved(status == AnswerStatus.CORRECT)
                .attemptCount(attemptCount)
                .build());
    }
}

