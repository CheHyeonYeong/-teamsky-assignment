-- Rerunnable migration for existing MySQL databases.
-- Run this before starting the default profile (`ddl-auto=validate`).

CREATE TABLE IF NOT EXISTS user_problem_state (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    problem_id BIGINT NOT NULL,
    last_submission_id BIGINT NOT NULL,
    last_answer_status VARCHAR(20) NOT NULL,
    solved BOOLEAN NOT NULL DEFAULT FALSE,
    attempt_count BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (problem_id) REFERENCES problems(id),
    FOREIGN KEY (last_submission_id) REFERENCES submissions(id),
    UNIQUE KEY uk_user_problem_state_user_problem (user_id, problem_id),
    INDEX idx_user_problem_state_user_problem (user_id, problem_id),
    INDEX idx_user_problem_state_user_status (user_id, last_answer_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_chapter_state (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    chapter_id BIGINT NOT NULL,
    last_skipped_problem_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (chapter_id) REFERENCES chapters(id),
    FOREIGN KEY (last_skipped_problem_id) REFERENCES problems(id),
    UNIQUE KEY uk_user_chapter_state_user_chapter (user_id, chapter_id),
    INDEX idx_user_chapter_state_user_chapter (user_id, chapter_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_submission_stats (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    total_submissions BIGINT NOT NULL DEFAULT 0,
    correct_submissions BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id),
    UNIQUE KEY uk_user_submission_stats_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_chapter_submission_stats (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    chapter_id BIGINT NOT NULL,
    total_submissions BIGINT NOT NULL DEFAULT 0,
    correct_submissions BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (chapter_id) REFERENCES chapters(id),
    UNIQUE KEY uk_user_chapter_submission_stats_user_chapter (user_id, chapter_id),
    INDEX idx_user_chapter_submission_stats_user_chapter (user_id, chapter_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO problem_stats (
    problem_id,
    total_count,
    correct_count
)
SELECT
    p.id,
    COALESCE(first_attempts.total_users, 0) AS total_count,
    COALESCE(first_attempts.correct_users, 0) AS correct_count
FROM problems p
LEFT JOIN (
    SELECT
        first_submission.problem_id,
        COUNT(*) AS total_users,
        SUM(CASE WHEN first_submission.answer_status = 'CORRECT' THEN 1 ELSE 0 END) AS correct_users
    FROM submissions first_submission
    JOIN (
        SELECT user_id, problem_id, MIN(id) AS first_submission_id
        FROM submissions
        GROUP BY user_id, problem_id
    ) grouped
        ON grouped.first_submission_id = first_submission.id
    GROUP BY first_submission.problem_id
) first_attempts
    ON first_attempts.problem_id = p.id
ON DUPLICATE KEY UPDATE
    total_count = VALUES(total_count),
    correct_count = VALUES(correct_count);

INSERT INTO user_problem_state (
    user_id,
    problem_id,
    last_submission_id,
    last_answer_status,
    solved,
    attempt_count,
    created_at,
    updated_at
)
SELECT
    latest.user_id,
    latest.problem_id,
    latest.id,
    latest.answer_status,
    latest.answer_status = 'CORRECT',
    grouped.attempt_count,
    grouped.first_submission_at,
    latest.updated_at
FROM submissions latest
JOIN (
    SELECT
        user_id,
        problem_id,
        MAX(id) AS latest_submission_id,
        COUNT(*) AS attempt_count,
        MIN(created_at) AS first_submission_at
    FROM submissions
    GROUP BY user_id, problem_id
) grouped
    ON grouped.user_id = latest.user_id
   AND grouped.problem_id = latest.problem_id
   AND grouped.latest_submission_id = latest.id
ON DUPLICATE KEY UPDATE
    last_submission_id = VALUES(last_submission_id),
    last_answer_status = VALUES(last_answer_status),
    solved = VALUES(solved),
    attempt_count = VALUES(attempt_count),
    created_at = VALUES(created_at),
    updated_at = VALUES(updated_at);

SET @has_skipped_problems := (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'skipped_problems'
);

SET @backfill_user_chapter_state_sql := IF(
    @has_skipped_problems > 0,
    '
    INSERT INTO user_chapter_state (
        user_id,
        chapter_id,
        last_skipped_problem_id,
        created_at,
        updated_at
    )
    SELECT
        latest.user_id,
        latest.chapter_id,
        latest.problem_id,
        NOW(6),
        NOW(6)
    FROM skipped_problems latest
    JOIN (
        SELECT user_id, chapter_id, MAX(id) AS latest_skip_id
        FROM skipped_problems
        GROUP BY user_id, chapter_id
    ) grouped
        ON grouped.latest_skip_id = latest.id
    ON DUPLICATE KEY UPDATE
        last_skipped_problem_id = VALUES(last_skipped_problem_id),
        updated_at = VALUES(updated_at)
    ',
    'SELECT 1'
);

PREPARE backfill_user_chapter_state_stmt FROM @backfill_user_chapter_state_sql;
EXECUTE backfill_user_chapter_state_stmt;
DEALLOCATE PREPARE backfill_user_chapter_state_stmt;

INSERT INTO user_submission_stats (
    user_id,
    total_submissions,
    correct_submissions,
    created_at,
    updated_at
)
SELECT
    s.user_id,
    COUNT(*) AS total_submissions,
    SUM(CASE WHEN s.answer_status = 'CORRECT' THEN 1 ELSE 0 END) AS correct_submissions,
    MIN(s.created_at) AS created_at,
    MAX(s.updated_at) AS updated_at
FROM submissions s
GROUP BY s.user_id
ON DUPLICATE KEY UPDATE
    total_submissions = VALUES(total_submissions),
    correct_submissions = VALUES(correct_submissions),
    created_at = VALUES(created_at),
    updated_at = VALUES(updated_at);

INSERT INTO user_chapter_submission_stats (
    user_id,
    chapter_id,
    total_submissions,
    correct_submissions,
    created_at,
    updated_at
)
SELECT
    s.user_id,
    p.chapter_id,
    COUNT(*) AS total_submissions,
    SUM(CASE WHEN s.answer_status = 'CORRECT' THEN 1 ELSE 0 END) AS correct_submissions,
    MIN(s.created_at) AS created_at,
    MAX(s.updated_at) AS updated_at
FROM submissions s
JOIN problems p ON p.id = s.problem_id
GROUP BY s.user_id, p.chapter_id
ON DUPLICATE KEY UPDATE
    total_submissions = VALUES(total_submissions),
    correct_submissions = VALUES(correct_submissions),
    created_at = VALUES(created_at),
    updated_at = VALUES(updated_at);
