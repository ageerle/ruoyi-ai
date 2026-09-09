-- ----------------------------
-- MCP 市场工具租户与审计列补齐
-- 2026-08-30
-- ----------------------------
-- 本脚本只迁移结构和归属信息，不删除无法关联到市场的孤儿记录。
-- 孤儿记录的 tenant_id 保持 NULL，因此租户归属查询会 fail closed。
-- 这是一次性迁移脚本：两个 ALTER TABLE 都会隐式提交。若执行中断或仅完成部分 DDL，
-- 必须先核对实际表结构后逐句续跑，禁止直接重跑整个文件。
-- 本机受控执行入口为 scripts/apply-live-migrations.ps1；先使用 -PreflightOnly，
-- 再以同一个显式 DPAPI 备份路径执行正式迁移。生产环境仍需按自身发布流程审计后执行。

-- 首次 DDL 前预检；上线前应确认结果为 0，避免新增列后留下无法可信回填的归属。
SELECT COUNT(*) AS `preflight_invalid_market_owner_count`
FROM `mcp_market_tool` AS `tool`
LEFT JOIN `mcp_market_info` AS `market` ON `market`.`id` = `tool`.`market_id`
WHERE `market`.`id` IS NULL
   OR `market`.`tenant_id` IS NULL;

ALTER TABLE `mcp_market_tool`
    ADD COLUMN `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '租户编号' AFTER `local_tool_id`,
    ADD COLUMN `create_dept` bigint NULL DEFAULT NULL COMMENT '创建部门' AFTER `tenant_id`,
    ADD COLUMN `create_by` bigint NULL DEFAULT NULL COMMENT '创建者' AFTER `create_dept`,
    ADD COLUMN `update_by` bigint NULL DEFAULT NULL COMMENT '更新者' AFTER `create_time`,
    ADD COLUMN `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间' AFTER `update_by`;

UPDATE `mcp_market_tool` AS `tool`
INNER JOIN `mcp_market_info` AS `market` ON `market`.`id` = `tool`.`market_id`
SET `tool`.`tenant_id` = `market`.`tenant_id`;

ALTER TABLE `mcp_market_tool`
    MODIFY COLUMN `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '000000' COMMENT '租户编号',
    ADD INDEX `idx_tenant_market` (`tenant_id`, `market_id`);

-- 上线前应确认结果为 0；统计覆盖父市场缺失、子表租户为空、父子租户为空或不一致。
SELECT COUNT(*) AS `invalid_market_tool_tenant_count`
FROM `mcp_market_tool` AS `tool`
LEFT JOIN `mcp_market_info` AS `market` ON `market`.`id` = `tool`.`market_id`
WHERE `market`.`id` IS NULL
   OR `tool`.`tenant_id` IS NULL
   OR `market`.`tenant_id` IS NULL
   OR NOT (BINARY `tool`.`tenant_id` <=> BINARY `market`.`tenant_id`);
