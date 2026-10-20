-- Full image prompt histories can exceed TEXT's 65535 byte capacity after a few revisions.
-- Enlarge both the history and undo columns while retaining all earlier candidates.
ALTER TABLE short_drama_character_appearance
    MODIFY COLUMN image_descriptions MEDIUMTEXT NULL COMMENT '每张图片对应的提示词（JSON数组）',
    MODIFY COLUMN previous_descriptions MEDIUMTEXT NULL COMMENT '上一轮提示词列表（撤销用，JSON数组）';
ALTER TABLE short_drama_location
    MODIFY COLUMN image_descriptions MEDIUMTEXT NULL COMMENT '每张图片对应的提示词（JSON数组）',
    MODIFY COLUMN previous_descriptions MEDIUMTEXT NULL COMMENT '上一轮提示词列表（撤销用，JSON数组）';
