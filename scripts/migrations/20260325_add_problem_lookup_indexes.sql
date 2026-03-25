-- Add problem lookup indexes for random chapter queries.
-- Safe to rerun on existing MySQL databases.

SET @has_problem_chapter_id_index := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'problems'
      AND index_name = 'idx_problem_chapter_id'
);

SET @add_problem_chapter_id_index_sql := IF(
    @has_problem_chapter_id_index = 0,
    'ALTER TABLE problems ADD INDEX idx_problem_chapter_id (chapter_id, id)',
    'SELECT 1'
);

PREPARE add_problem_chapter_id_index_stmt FROM @add_problem_chapter_id_index_sql;
EXECUTE add_problem_chapter_id_index_stmt;
DEALLOCATE PREPARE add_problem_chapter_id_index_stmt;

SET @has_problem_chapter_difficulty_id_index := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'problems'
      AND index_name = 'idx_problem_chapter_difficulty_id'
);

SET @add_problem_chapter_difficulty_id_index_sql := IF(
    @has_problem_chapter_difficulty_id_index = 0,
    'ALTER TABLE problems ADD INDEX idx_problem_chapter_difficulty_id (chapter_id, difficulty, id)',
    'SELECT 1'
);

PREPARE add_problem_chapter_difficulty_id_index_stmt FROM @add_problem_chapter_difficulty_id_index_sql;
EXECUTE add_problem_chapter_difficulty_id_index_stmt;
DEALLOCATE PREPARE add_problem_chapter_difficulty_id_index_stmt;
