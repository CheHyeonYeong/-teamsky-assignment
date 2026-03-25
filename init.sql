-- TeamSky Learning Platform Database Schema

CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(255),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS chapters (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description VARCHAR(500),
    order_num INT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS problems (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    chapter_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    problem_type VARCHAR(20) NOT NULL,
    difficulty VARCHAR(10) NOT NULL,
    explanation TEXT,
    hint TEXT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    FOREIGN KEY (chapter_id) REFERENCES chapters(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS choices (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    problem_id BIGINT NOT NULL,
    choice_number INT NOT NULL,
    content TEXT NOT NULL,
    FOREIGN KEY (problem_id) REFERENCES problems(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS answers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    problem_id BIGINT NOT NULL,
    answer_value VARCHAR(500) NOT NULL,
    FOREIGN KEY (problem_id) REFERENCES problems(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS submissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    problem_id BIGINT NOT NULL,
    answer_status VARCHAR(20) NOT NULL,
    user_answer TEXT NOT NULL,
    time_spent_seconds BIGINT,
    hint_used BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (problem_id) REFERENCES problems(id),
    INDEX idx_submission_user_problem (user_id, problem_id),
    INDEX idx_submission_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

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

CREATE TABLE IF NOT EXISTS bookmarks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    problem_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (problem_id) REFERENCES problems(id),
    UNIQUE KEY uk_bookmark_user_problem (user_id, problem_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS problem_stats (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    problem_id BIGINT NOT NULL UNIQUE,
    total_count BIGINT NOT NULL DEFAULT 0,
    correct_count BIGINT NOT NULL DEFAULT 0,
    FOREIGN KEY (problem_id) REFERENCES problems(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Sample Data for Testing

INSERT INTO users (name, email, created_at, updated_at) VALUES
('홍길동', 'hong@example.com', NOW(), NOW()),
('김철수', 'kim@example.com', NOW(), NOW()),
('이영희', 'lee@example.com', NOW(), NOW());

INSERT INTO chapters (name, description, order_num, created_at, updated_at) VALUES
('1장. 기초 수학', '기초적인 수학 개념을 학습합니다.', 1, NOW(), NOW()),
('2장. 대수학', '대수학의 기본 개념을 학습합니다.', 2, NOW(), NOW()),
('3장. 기하학', '기하학의 기본 개념을 학습합니다.', 3, NOW(), NOW());

INSERT INTO problems (chapter_id, content, problem_type, difficulty, explanation, hint, created_at, updated_at) VALUES
(1, '다음 중 소수를 모두 고르시오.', 'MULTIPLE_CHOICE', 'LOW', '2, 3, 5, 7은 소수입니다. 소수는 1과 자기 자신만으로 나누어지는 수입니다.', '소수는 약수가 2개인 수입니다.', NOW(), NOW()),
(1, '10 + 5 * 2의 결과는?', 'SUBJECTIVE', 'LOW', '연산 순서에 따라 곱셈을 먼저 계산합니다. 5 * 2 = 10, 10 + 10 = 20', '연산 순서를 생각해보세요.', NOW(), NOW()),
(1, '다음 중 짝수를 모두 고르시오.', 'MULTIPLE_CHOICE', 'LOW', '2, 4는 짝수입니다.', '2로 나누어 떨어지는 수입니다.', NOW(), NOW()),
(2, '일차방정식 2x + 3 = 7의 해는?', 'SUBJECTIVE', 'MEDIUM', 'x = 2입니다. 2x = 4, x = 2', '양변에서 3을 빼세요.', NOW(), NOW()),
(2, '다음 중 이차방정식의 근의 공식에 해당하는 것을 고르시오.', 'MULTIPLE_CHOICE', 'HIGH', '근의 공식은 (-b ± √(b²-4ac)) / 2a 입니다.', '판별식을 기억하세요.', NOW(), NOW());

INSERT INTO choices (problem_id, choice_number, content) VALUES
(1, 1, '2'),
(1, 2, '3'),
(1, 3, '4'),
(1, 4, '5'),
(1, 5, '6'),
(3, 1, '1'),
(3, 2, '2'),
(3, 3, '3'),
(3, 4, '4'),
(3, 5, '5'),
(5, 1, 'x = -b / 2a'),
(5, 2, 'x = (-b ± √(b²-4ac)) / 2a'),
(5, 3, 'x = b² - 4ac'),
(5, 4, 'x = a + b + c'),
(5, 5, 'x = -b ± √(b²+4ac)');

INSERT INTO answers (problem_id, answer_value) VALUES
(1, '1'),
(1, '2'),
(1, '4'),
(2, '20'),
(3, '2'),
(3, '4'),
(4, '2'),
(5, '2');

INSERT INTO problem_stats (problem_id, total_count, correct_count) VALUES
(1, 50, 35),
(2, 45, 40),
(3, 30, 25),
(4, 25, 15),
(5, 20, 8);
