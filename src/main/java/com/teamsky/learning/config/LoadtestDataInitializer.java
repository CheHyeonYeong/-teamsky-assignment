package com.teamsky.learning.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@Profile("loadtest")
@RequiredArgsConstructor
public class LoadtestDataInitializer implements ApplicationRunner {

    private static final long FIRST_CHAPTER_ID = 901L;
    private static final long SECOND_CHAPTER_ID = 902L;
    private static final long USER_ID_START = 1001L;
    private static final int USER_COUNT = 120;
    private static final int SUBJECTIVE_PROBLEMS_PER_CHAPTER = 20;
    private static final int SEEDED_SUBMISSIONS_PER_CHAPTER = 4;
    private static final long PROBLEM_ID_START = 2001L;
    private static final long ANSWER_ID_START = 3001L;
    private static final long SUBMISSION_ID_START = 4001L;
    private static final long PROBLEM_STATS_TOTAL_COUNT = 60L;
    private static final long PROBLEM_STATS_CORRECT_BASE = 36L;
    private static final LocalDateTime BASE_TIME = LocalDateTime.of(2026, 1, 1, 0, 0);

    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<SeedProblem> problems = buildSeedProblems();

        resetSeedData();
        seedChapters();
        seedUsers();
        seedProblems(problems);
        seedAnswers(problems);
        seedProblemStats(problems);
        seedSubmissionData(problems);
    }

    private void resetSeedData() {
        jdbcTemplate.update(
                "DELETE FROM user_chapter_submission_stats WHERE chapter_id IN (?, ?) OR user_id BETWEEN ? AND ?",
                FIRST_CHAPTER_ID, SECOND_CHAPTER_ID, USER_ID_START, USER_ID_START + USER_COUNT - 1
        );
        jdbcTemplate.update(
                "DELETE FROM user_submission_stats WHERE user_id BETWEEN ? AND ?",
                USER_ID_START, USER_ID_START + USER_COUNT - 1
        );
        jdbcTemplate.update(
                "DELETE FROM user_chapter_state WHERE chapter_id IN (?, ?) OR user_id BETWEEN ? AND ?",
                FIRST_CHAPTER_ID, SECOND_CHAPTER_ID, USER_ID_START, USER_ID_START + USER_COUNT - 1
        );
        jdbcTemplate.update(
                "DELETE FROM user_problem_state WHERE problem_id BETWEEN ? AND ? OR user_id BETWEEN ? AND ?",
                PROBLEM_ID_START,
                PROBLEM_ID_START + (SUBJECTIVE_PROBLEMS_PER_CHAPTER * 2L) - 1,
                USER_ID_START,
                USER_ID_START + USER_COUNT - 1
        );
        jdbcTemplate.update(
                "DELETE FROM submissions WHERE problem_id BETWEEN ? AND ? OR user_id BETWEEN ? AND ?",
                PROBLEM_ID_START,
                PROBLEM_ID_START + (SUBJECTIVE_PROBLEMS_PER_CHAPTER * 2L) - 1,
                USER_ID_START,
                USER_ID_START + USER_COUNT - 1
        );
        jdbcTemplate.update(
                "DELETE FROM problem_stats WHERE problem_id BETWEEN ? AND ?",
                PROBLEM_ID_START,
                PROBLEM_ID_START + (SUBJECTIVE_PROBLEMS_PER_CHAPTER * 2L) - 1
        );
        jdbcTemplate.update(
                "DELETE FROM answers WHERE problem_id BETWEEN ? AND ?",
                PROBLEM_ID_START,
                PROBLEM_ID_START + (SUBJECTIVE_PROBLEMS_PER_CHAPTER * 2L) - 1
        );
        jdbcTemplate.update(
                "DELETE FROM choices WHERE problem_id BETWEEN ? AND ?",
                PROBLEM_ID_START,
                PROBLEM_ID_START + (SUBJECTIVE_PROBLEMS_PER_CHAPTER * 2L) - 1
        );
        jdbcTemplate.update(
                "DELETE FROM problems WHERE id BETWEEN ? AND ?",
                PROBLEM_ID_START,
                PROBLEM_ID_START + (SUBJECTIVE_PROBLEMS_PER_CHAPTER * 2L) - 1
        );
        jdbcTemplate.update(
                "DELETE FROM chapters WHERE id IN (?, ?)",
                FIRST_CHAPTER_ID,
                SECOND_CHAPTER_ID
        );
        jdbcTemplate.update(
                "DELETE FROM users WHERE id BETWEEN ? AND ?",
                USER_ID_START,
                USER_ID_START + USER_COUNT - 1
        );
    }

    private void seedChapters() {
        jdbcTemplate.batchUpdate(
                """
                        INSERT INTO chapters (id, name, description, order_num, created_at, updated_at)
                        VALUES (?, ?, ?, ?, NOW(6), NOW(6))
                        ON DUPLICATE KEY UPDATE
                            name = VALUES(name),
                            description = VALUES(description),
                            order_num = VALUES(order_num),
                            updated_at = NOW(6)
                        """,
                List.of(
                        new Object[]{FIRST_CHAPTER_ID, "Load Test Chapter 1", "Load test seed chapter 1", 1},
                        new Object[]{SECOND_CHAPTER_ID, "Load Test Chapter 2", "Load test seed chapter 2", 2}
                )
        );
    }

    private void seedUsers() {
        List<Object[]> args = new ArrayList<>();
        for (int offset = 0; offset < USER_COUNT; offset++) {
            long userId = USER_ID_START + offset;
            String suffix = String.format("%03d", offset + 1);
            args.add(new Object[]{
                    userId,
                    "load_user_" + suffix,
                    "load_user_" + suffix + "@example.com"
            });
        }

        jdbcTemplate.batchUpdate(
                """
                        INSERT INTO users (id, name, email, created_at, updated_at)
                        VALUES (?, ?, ?, NOW(6), NOW(6))
                        ON DUPLICATE KEY UPDATE
                            name = VALUES(name),
                            email = VALUES(email),
                            updated_at = NOW(6)
                        """,
                args
        );
    }

    private void seedProblems(List<SeedProblem> problems) {
        List<Object[]> args = new ArrayList<>();
        for (SeedProblem problem : problems) {
            args.add(new Object[]{
                    problem.problemId(),
                    problem.chapterId(),
                    problem.content(),
                    "SUBJECTIVE",
                    problem.difficulty(),
                    problem.explanation(),
                    problem.hint()
            });
        }

        jdbcTemplate.batchUpdate(
                """
                        INSERT INTO problems (
                            id, chapter_id, content, problem_type, difficulty, explanation, hint, created_at, updated_at
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, NOW(6), NOW(6))
                        ON DUPLICATE KEY UPDATE
                            chapter_id = VALUES(chapter_id),
                            content = VALUES(content),
                            problem_type = VALUES(problem_type),
                            difficulty = VALUES(difficulty),
                            explanation = VALUES(explanation),
                            hint = VALUES(hint),
                            updated_at = NOW(6)
                        """,
                args
        );
    }

    private void seedAnswers(List<SeedProblem> problems) {
        List<Object[]> args = new ArrayList<>();
        for (SeedProblem problem : problems) {
            args.add(new Object[]{
                    problem.answerId(),
                    problem.problemId(),
                    problem.correctAnswer()
            });
        }

        jdbcTemplate.batchUpdate(
                """
                        INSERT INTO answers (id, problem_id, answer_value)
                        VALUES (?, ?, ?)
                        ON DUPLICATE KEY UPDATE
                            problem_id = VALUES(problem_id),
                            answer_value = VALUES(answer_value)
                        """,
                args
        );
    }

    private void seedProblemStats(List<SeedProblem> problems) {
        List<Object[]> args = new ArrayList<>();
        for (int index = 0; index < problems.size(); index++) {
            SeedProblem problem = problems.get(index);
            args.add(new Object[]{
                    problem.problemId(),
                    PROBLEM_STATS_TOTAL_COUNT,
                    PROBLEM_STATS_CORRECT_BASE + (index % 15)
            });
        }

        jdbcTemplate.batchUpdate(
                """
                        INSERT INTO problem_stats (problem_id, total_count, correct_count)
                        VALUES (?, ?, ?)
                        ON DUPLICATE KEY UPDATE
                            total_count = VALUES(total_count),
                            correct_count = VALUES(correct_count)
                        """,
                args
        );
    }

    private void seedSubmissionData(List<SeedProblem> problems) {
        Map<Long, List<SeedProblem>> problemsByChapter = new LinkedHashMap<>();
        problemsByChapter.put(FIRST_CHAPTER_ID, new ArrayList<>());
        problemsByChapter.put(SECOND_CHAPTER_ID, new ArrayList<>());
        for (SeedProblem problem : problems) {
            problemsByChapter.get(problem.chapterId()).add(problem);
        }

        List<Object[]> submissionArgs = new ArrayList<>();
        List<Object[]> stateArgs = new ArrayList<>();
        Map<Long, SubmissionCounter> userStats = new LinkedHashMap<>();
        Map<UserChapterKey, SubmissionCounter> chapterStats = new LinkedHashMap<>();

        long submissionId = SUBMISSION_ID_START;
        for (int userOffset = 0; userOffset < USER_COUNT; userOffset++) {
            long userId = USER_ID_START + userOffset;
            for (long chapterId : List.of(FIRST_CHAPTER_ID, SECOND_CHAPTER_ID)) {
                List<SeedProblem> chapterProblems = problemsByChapter.get(chapterId);
                for (int attemptIndex = 0; attemptIndex < SEEDED_SUBMISSIONS_PER_CHAPTER; attemptIndex++) {
                    SeedProblem problem = chapterProblems.get((userOffset + attemptIndex) % chapterProblems.size());
                    boolean isCorrect = attemptIndex % 2 == 0;
                    String answerStatus = isCorrect ? "CORRECT" : "WRONG";
                    String userAnswer = isCorrect
                            ? problem.correctAnswer()
                            : "wrong-" + userId + "-" + problem.problemId();
                    Timestamp eventTime = Timestamp.valueOf(BASE_TIME.plusSeconds(submissionId - SUBMISSION_ID_START));

                    submissionArgs.add(new Object[]{
                            submissionId,
                            userId,
                            problem.problemId(),
                            answerStatus,
                            userAnswer,
                            15L + attemptIndex,
                            false,
                            eventTime,
                            eventTime
                    });
                    stateArgs.add(new Object[]{
                            userId,
                            problem.problemId(),
                            submissionId,
                            answerStatus,
                            isCorrect,
                            1L,
                            eventTime,
                            eventTime
                    });

                    userStats.computeIfAbsent(userId, ignored -> new SubmissionCounter()).record(isCorrect);
                    chapterStats.computeIfAbsent(new UserChapterKey(userId, chapterId), ignored -> new SubmissionCounter())
                            .record(isCorrect);

                    submissionId++;
                }
            }
        }

        jdbcTemplate.batchUpdate(
                """
                        INSERT INTO submissions (
                            id, user_id, problem_id, answer_status, user_answer, time_spent_seconds, hint_used, created_at, updated_at
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        ON DUPLICATE KEY UPDATE
                            answer_status = VALUES(answer_status),
                            user_answer = VALUES(user_answer),
                            time_spent_seconds = VALUES(time_spent_seconds),
                            hint_used = VALUES(hint_used),
                            updated_at = VALUES(updated_at)
                        """,
                submissionArgs
        );

        jdbcTemplate.batchUpdate(
                """
                        INSERT INTO user_problem_state (
                            user_id, problem_id, last_submission_id, last_answer_status, solved, attempt_count, created_at, updated_at
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                        ON DUPLICATE KEY UPDATE
                            last_submission_id = VALUES(last_submission_id),
                            last_answer_status = VALUES(last_answer_status),
                            solved = VALUES(solved),
                            attempt_count = VALUES(attempt_count),
                            updated_at = VALUES(updated_at)
                        """,
                stateArgs
        );

        List<Object[]> userStatsArgs = new ArrayList<>();
        for (Map.Entry<Long, SubmissionCounter> entry : userStats.entrySet()) {
            userStatsArgs.add(new Object[]{
                    entry.getKey(),
                    entry.getValue().total(),
                    entry.getValue().correct(),
                    Timestamp.valueOf(BASE_TIME),
                    Timestamp.valueOf(BASE_TIME.plusMinutes(1))
            });
        }
        jdbcTemplate.batchUpdate(
                """
                        INSERT INTO user_submission_stats (
                            user_id, total_submissions, correct_submissions, created_at, updated_at
                        )
                        VALUES (?, ?, ?, ?, ?)
                        ON DUPLICATE KEY UPDATE
                            total_submissions = VALUES(total_submissions),
                            correct_submissions = VALUES(correct_submissions),
                            updated_at = VALUES(updated_at)
                        """,
                userStatsArgs
        );

        List<Object[]> chapterStatsArgs = new ArrayList<>();
        for (Map.Entry<UserChapterKey, SubmissionCounter> entry : chapterStats.entrySet()) {
            chapterStatsArgs.add(new Object[]{
                    entry.getKey().userId(),
                    entry.getKey().chapterId(),
                    entry.getValue().total(),
                    entry.getValue().correct(),
                    Timestamp.valueOf(BASE_TIME),
                    Timestamp.valueOf(BASE_TIME.plusMinutes(1))
            });
        }
        jdbcTemplate.batchUpdate(
                """
                        INSERT INTO user_chapter_submission_stats (
                            user_id, chapter_id, total_submissions, correct_submissions, created_at, updated_at
                        )
                        VALUES (?, ?, ?, ?, ?, ?)
                        ON DUPLICATE KEY UPDATE
                            total_submissions = VALUES(total_submissions),
                            correct_submissions = VALUES(correct_submissions),
                            updated_at = VALUES(updated_at)
                        """,
                chapterStatsArgs
        );
    }

    private List<SeedProblem> buildSeedProblems() {
        List<SeedProblem> problems = new ArrayList<>();

        long problemId = PROBLEM_ID_START;
        long answerId = ANSWER_ID_START;
        for (long chapterId : List.of(FIRST_CHAPTER_ID, SECOND_CHAPTER_ID)) {
            for (int index = 0; index < SUBJECTIVE_PROBLEMS_PER_CHAPTER; index++) {
                String difficulty = switch (index % 3) {
                    case 0 -> "LOW";
                    case 1 -> "MEDIUM";
                    default -> "HIGH";
                };
                problems.add(new SeedProblem(
                        problemId,
                        answerId,
                        chapterId,
                        "Load test subjective problem " + problemId,
                        "Load test explanation " + problemId,
                        "Load test hint " + problemId,
                        "answer-" + problemId,
                        difficulty
                ));
                problemId++;
                answerId++;
            }
        }

        return problems;
    }

    private record SeedProblem(
            long problemId,
            long answerId,
            long chapterId,
            String content,
            String explanation,
            String hint,
            String correctAnswer,
            String difficulty
    ) {
    }

    private record UserChapterKey(long userId, long chapterId) {
    }

    private static final class SubmissionCounter {
        private long total;
        private long correct;

        void record(boolean isCorrect) {
            total++;
            if (isCorrect) {
                correct++;
            }
        }

        long total() {
            return total;
        }

        long correct() {
            return correct;
        }
    }
}
