package com.teamsky.learning.submission.infrastructure;

import com.teamsky.learning.chapter.domain.Chapter;
import com.teamsky.learning.shared.config.JpaConfig;
import com.teamsky.learning.problem.domain.Difficulty;
import com.teamsky.learning.problem.domain.Problem;
import com.teamsky.learning.problem.domain.ProblemType;
import com.teamsky.learning.submission.domain.AnswerStatus;
import com.teamsky.learning.submission.domain.Submission;
import com.teamsky.learning.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@DisplayName("SubmissionRepository MySQL tests")
class SubmissionRepositoryTest {

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("returns the latest submission for a user and problem")
    void shouldReturnLatestSubmissionByUserAndProblem() {
        User user = entityManager.persistAndFlush(User.builder()
                .name("repo-user")
                .email("repo-user@example.com")
                .build());

        Chapter chapter = entityManager.persistAndFlush(Chapter.builder()
                .name("Repository chapter")
                .description("Repository chapter description")
                .orderNum(1)
                .build());

        Problem problem = entityManager.persistAndFlush(Problem.builder()
                .chapter(chapter)
                .content("Repository problem")
                .problemType(ProblemType.SUBJECTIVE)
                .difficulty(Difficulty.LOW)
                .explanation("Repository explanation")
                .hint("Repository hint")
                .build());

        entityManager.persist(Submission.builder()
                .user(user)
                .problem(problem)
                .answerStatus(AnswerStatus.WRONG)
                .userAnswer("wrong")
                .timeSpentSeconds(5L)
                .hintUsed(false)
                .build());

        Submission latestSubmission = entityManager.persist(Submission.builder()
                .user(user)
                .problem(problem)
                .answerStatus(AnswerStatus.CORRECT)
                .userAnswer("correct")
                .timeSpentSeconds(8L)
                .hintUsed(false)
                .build());

        entityManager.flush();
        entityManager.clear();

        Submission result = submissionRepository
                .findFirstByUser_IdAndProblem_IdOrderByCreatedAtDescIdDesc(user.getId(), problem.getId())
                .orElseThrow();

        assertThat(result.getId()).isEqualTo(latestSubmission.getId());
        assertThat(result.getAnswerStatus()).isEqualTo(AnswerStatus.CORRECT);
    }
}

