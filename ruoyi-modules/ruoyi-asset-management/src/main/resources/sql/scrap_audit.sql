-- 报废审核表
CREATE TABLE `scrap_audit` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `serial_number` varchar(64) DEFAULT NULL COMMENT '序号',
  `asset_number` varchar(64) NOT NULL COMMENT '资产编号',
  `asset_name` varchar(255) NOT NULL COMMENT '资产名称',
  `original_min_usage_period` int(11) DEFAULT NULL COMMENT '原始最低使用年限（年）',
  `used_time` decimal(10,2) DEFAULT NULL COMMENT '已使用时间（年）',
  `matched_classification_code` varchar(64) DEFAULT NULL COMMENT '匹配的分类代码',
  `matched_classification_name` varchar(255) DEFAULT NULL COMMENT '匹配的分类名称',
  `actual_min_usage_period` int(11) DEFAULT NULL COMMENT '实际最低使用年限（年）',
  `is_scrap_eligible` tinyint(1) DEFAULT NULL COMMENT '是否达到报废标准',
  `match_status` varchar(20) DEFAULT NULL COMMENT '匹配状态：SUCCESS-匹配成功，FAILED-匹配失败',
  `judgment_basis` varchar(500) DEFAULT NULL COMMENT '判断依据',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_asset_number` (`asset_number`),
  KEY `idx_asset_name` (`asset_name`),
  KEY `idx_match_status` (`match_status`),
  KEY `idx_is_scrap_eligible` (`is_scrap_eligible`),
  KEY `idx_serial_number` (`serial_number`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报废审核表';

