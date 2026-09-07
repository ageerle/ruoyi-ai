-- W4-Defence-AC-INC-16b 销售回款台账 DDL 补齐
-- 卡面：B-FIX-PACK-1 / AC-INC-16b（回款口径验证）阻塞 AC 之一
-- 落点：对应 org.ruoyi.ipd.domain.ReceiptLedger（P3-4.1 实装体）
-- 业务规则：
--   AC-INC-16b：达成率口径必须是 RECEIPT（回款），不是 SHIPMENT/开票
--   AC-INC-16c：月度录入金额 + 凭证附件 URL + SHA256
--   AC-INC-16d：窗口外数据不计入达成率
--   AC-INC-31b：窗口内退款当期冲减（refund_amount）
--   AC-INC-32：6自然月窗口以上市日期为起算点（window_start / window_end）
-- 幂等策略：
--   information_schema 显式存在性检查 + IF NOT EXISTS 双保险
--   重复执行安全（同 2026-09-06-ipd-qa05p3-audit-operator-index.sql 模式）
-- 已存在版本说明：
--   docs/script/sql/update/receipt_ledger.sql（Wave2 dbc75862 落盘，无日期前缀）
--   本文件为 B-FIX-PACK-1 卡面显式化补齐，结构与既有 DDL 字段一致，含 IF NOT EXISTS

CREATE TABLE IF NOT EXISTS `receipt_ledger` (
  `id` bigint NOT NULL COMMENT '主键（雪花）',
  `project_id` bigint NOT NULL COMMENT '项目ID（关联 projects.id）',
  `bonus_pool_id` bigint DEFAULT NULL COMMENT '奖金池ID（关联 bonus_pools.id）',
  `receipt_month` varchar(7) COLLATE utf8mb4_general_ci NOT NULL COMMENT '回款月份 YYYY-MM（任务书 period 字段）',
  `receipt_amount` decimal(18,2) NOT NULL COMMENT '回款金额（正数；任务书 amount 字段）',
  `refund_amount` decimal(18,2) DEFAULT '0.00' COMMENT '退款冲减金额（AC-INC-31b 窗口内当期冲减）',
  `net_amount` decimal(18,2) GENERATED ALWAYS AS (`receipt_amount` - `refund_amount`) STORED COMMENT '净回款（生成列）',
  `voucher_url` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '凭证附件URL（AC-INC-16c）',
  `voucher_hash` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '凭证SHA256',
  `source` varchar(16) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'RECEIPT' COMMENT '来源 RECEIPT|SHIPMENT（AC-INC-16b 口径验证）',
  `window_start` date DEFAULT NULL COMMENT '6自然月窗口起算日（AC-INC-32 上市日期）',
  `window_end` date DEFAULT NULL COMMENT '6自然月窗口截止日',
  `in_window` tinyint(1) GENERATED ALWAYS AS (`receipt_month` >= DATE_FORMAT(`window_start`, '%Y-%m') AND `receipt_month` <= DATE_FORMAT(`window_end`, '%Y-%m')) STORED COMMENT '是否在窗口内（AC-INC-16d 窗外不计）',
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='销售回款台账（P3-4.1 / AC-INC-16b）';

-- 执行后校验（期望返回 1 行：receipt_ledger）
SELECT table_name, table_comment
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name = 'receipt_ledger';

-- ROLLBACK（环境异常回滚用）
-- DROP TABLE IF EXISTS `receipt_ledger`;
