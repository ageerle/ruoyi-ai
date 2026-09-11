-- P3-4.1 销售回款台账（AC-INC-16b/16c/16d/31/31b/32）
-- 执行前确认：SHOW TABLES LIKE 'receipt_ledger' → 空
CREATE TABLE IF NOT EXISTS `receipt_ledger` (
  `id` bigint NOT NULL COMMENT '主键（雪花）',
  `project_id` bigint NOT NULL COMMENT '项目ID（关联 projects.id）',
  `bonus_pool_id` bigint DEFAULT NULL COMMENT '奖金池ID（关联 bonus_pools.id）',
  `receipt_month` varchar(7) COLLATE utf8mb4_general_ci NOT NULL COMMENT '回款月份 YYYY-MM',
  `receipt_amount` decimal(18,2) NOT NULL COMMENT '回款金额（正数）',
  `refund_amount` decimal(18,2) DEFAULT '0.00' COMMENT '退款冲减金额（AC-INC-31b 窗口内当期冲减）',
  `net_amount` decimal(18,2) GENERATED ALWAYS AS (`receipt_amount` - `refund_amount`) STORED COMMENT '净回款（生成列）',
  `voucher_url` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '凭证附件URL（AC-INC-16c）',
  `voucher_hash` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '凭证SHA256',
  `source` varchar(16) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'RECEIPT' COMMENT '来源 RECEIPT|SHIPMENT（AC-INC-16b）',
  `window_start` date DEFAULT NULL COMMENT '6自然月窗口起算日（AC-INC-32 上市日期）',
  `window_end` date DEFAULT NULL COMMENT '6自然月窗口截止日',
  `in_window` tinyint(1) GENERATED ALWAYS AS (`receipt_month` >= DATE_FORMAT(`window_start`, '%Y-%m') AND `receipt_month` <= DATE_FORMAT(`window_end`, '%Y-%m')) STORED COMMENT '是否在窗口内（AC-INC-16d）',
  `create_dept` bigint DEFAULT NULL,
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `tenant_id` varchar(20) COLLATE utf8mb4_general_ci DEFAULT '000000',
  `del_flag` char(1) COLLATE utf8mb4_general_ci DEFAULT '0',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_receipt_project_month` (`project_id`, `receipt_month`, `tenant_id`, `del_flag`),
  KEY `idx_receipt_bonus_pool` (`bonus_pool_id`),
  KEY `idx_receipt_window` (`window_start`, `window_end`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='销售回款台账（P3-4.1）';
