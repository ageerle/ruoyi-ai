-- 高等学校固定资产分类与代码表
CREATE TABLE `asset_classification` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `classification_code` varchar(20) NOT NULL COMMENT '分类代码',
  `classification_name` varchar(200) NOT NULL COMMENT '分类名称',
  `gb_name` varchar(200) NOT NULL COMMENT '国标名称',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_classification_code` (`classification_code`),
  KEY `idx_classification_name` (`classification_name`),
  KEY `idx_gb_name` (`gb_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='高等学校固定资产分类与代码表';
