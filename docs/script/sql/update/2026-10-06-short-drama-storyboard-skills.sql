-- Add ordered multi-selection without rewriting existing project bindings.
SET @storyboard_skills_sql = (SELECT IF(COUNT(*) = 0,
 'ALTER TABLE short_drama_project ADD COLUMN storyboard_skill_names TEXT NULL COMMENT ''Ordered storyboard skill catalog names as JSON''',
 'SELECT 1') FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'short_drama_project' AND column_name = 'storyboard_skill_names');
PREPARE storyboard_skills_stmt FROM @storyboard_skills_sql;
EXECUTE storyboard_skills_stmt;
DEALLOCATE PREPARE storyboard_skills_stmt;
