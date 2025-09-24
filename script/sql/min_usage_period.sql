-- 最低使用年限表
CREATE TABLE `min_usage_period` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `category` varchar(100) NOT NULL COMMENT '固定资产类别',
  `content` varchar(200) NOT NULL COMMENT '内容',
  `min_years` int(11) NOT NULL COMMENT '最低使用年限（年）',
  `gb_code` varchar(20) NOT NULL COMMENT '国标代码',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_gb_code` (`gb_code`),
  KEY `idx_category` (`category`),
  KEY `idx_content` (`content`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='最低使用年限表';