-- Store the exact creation input separately from generated project descriptions.
-- Existing rows stay NULL unless their original request can be recovered.
SET @original_idea_sql = (SELECT IF(COUNT(*) = 0,
 'ALTER TABLE short_drama_project ADD COLUMN original_idea LONGTEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT ''Original story creation input'' AFTER description',
 'SELECT 1') FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'short_drama_project' AND column_name = 'original_idea');
PREPARE original_idea_stmt FROM @original_idea_sql;
EXECUTE original_idea_stmt;
DEALLOCATE PREPARE original_idea_stmt;
