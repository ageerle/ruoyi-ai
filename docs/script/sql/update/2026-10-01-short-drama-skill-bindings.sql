-- MySQL idempotent migration; run in the configured application database.
SET @skill_aesthetic_sql = (SELECT IF(COUNT(*) = 0,
 'ALTER TABLE short_drama_project ADD COLUMN aesthetic_skill_name VARCHAR(64) NULL COMMENT ''Selected aesthetic skill catalog name''',
 'SELECT 1') FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'short_drama_project' AND column_name = 'aesthetic_skill_name');
PREPARE skill_aesthetic_stmt FROM @skill_aesthetic_sql;
EXECUTE skill_aesthetic_stmt;
DEALLOCATE PREPARE skill_aesthetic_stmt;
SET @skill_director_sql = (SELECT IF(COUNT(*) = 0,
 'ALTER TABLE short_drama_project ADD COLUMN director_skill_name VARCHAR(64) NULL COMMENT ''Selected director skill catalog name''',
 'SELECT 1') FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'short_drama_project' AND column_name = 'director_skill_name');
PREPARE skill_director_stmt FROM @skill_director_sql;
EXECUTE skill_director_stmt;
DEALLOCATE PREPARE skill_director_stmt;
