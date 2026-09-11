-- DEF-6：audit_logs 载荷列 json → longtext（owner 2026-09-05 选定方案 A + 护栏配套；本脚本落地 @18:33）
--
-- 根因：before_data/after_data 为 MySQL json 列，读回时被服务端「规范化渲染」
--       （键排序按 UTF-8 字节长度→字典序、成员间插 ", "、键后 ": "、1e3→1000.0），
--       与写入侧用于算 curr_hash 的 Jackson 紧凑串（AuditEventData.json = 无空格 + 插入序）
--       永不相等 → 带载荷的审计行写完立即被 verifyChain 判断裂（AC-AUD-03 对 6 个写入点失效）。
--       实证：断裂 seq 466/485/502 全为 action=FAILURE；归因探针 attribute_broken 3/3 PASS
--       （断裂行 100% 携带载荷，非载荷行全自洽、seq 零跳号零重复）。
--
-- 方案 A 的取舍：改 longtext 后字节精确往返、零 Java 映射改动（AuditLog.beforeData/afterData
--       本就是 String）、不动已冻结哈希协议 v1；但 **json 列类型原本同时承担 DEF-1 的 DB 层
--       fail-fast 护栏**（DEF-1：GateElementService 纯文本直写 after_data → MysqlDataTruncation
--       「Invalid JSON text」→ 审计与业务同 @Transactional 整体回滚 → 接口 500 + DB 零写入，
--       修复 b74f46bf）。改 longtext 后 MySQL 不再校验 JSON 合法性，畸形载荷将由 fail-fast
--       退化为 fail-late（静默入库、直到读取/审计导出/前端解析时才炸），且脏载荷进入 hash 链。
--       ⇒ 本脚本必须与「应用层护栏」同批上线：AuditLogService.append 入口校验 before/afterData
--         合法性（非法即抛 DataIntegrityViolationException，复刻原 DB 层异常族与回滚语义），
--         回归锁 AuditPayloadJsonGuardTest + 既有 GateElementAuditJsonTest 4/4 绿。
--         缺护栏不得执行本脚本。
--
-- 执行前置：
--   1) 停写窗口。ALTER 需重建表（json→longtext 非 in-place），会取 MDL；执行前确认
--      SELECT COUNT(*) FROM information_schema.INNODB_TRX = 0
--      且 performance_schema.metadata_locks 上无 audit_logs 持锁，否则 ALTER 会排队并阻塞其后全部写入。
--   2) 应用侧护栏 jar 已就绪（先上护栏再改列型，避免出现「无护栏窗口」）。
--
-- 存量数据：转换后既有载荷行的 after_data 变为 MySQL 的规范化文本，其 curr_hash 仍按旧紧凑串
--       算出 → 依然断裂。须在**全部实例切到含 DEF-4 5b95a9d0（毫秒归零）与本护栏的 jar 之后**
--       调用超管端点 POST /api/v1/audit-logs/rebuild-chain 重算全链，再 GET /api/v1/audit-logs/verify
--       终验 chain=OK / broken=0。顺序颠倒会被旧 jar 实例再次污染。
--
-- 幂等：重复执行安全（MODIFY 到同一列型为无操作）；执行前后可用文末校验查询对照。

-- 执行前现态留证（期望 json / json）
select column_name, column_type
from information_schema.columns
where table_schema = database()
  and table_name = 'audit_logs'
  and column_name in ('before_data', 'after_data');

alter table audit_logs
    modify before_data longtext null comment '变更前载荷（DEF-6：原 json 列，改 longtext 保字节精确往返以自洽 hash 链；JSON 合法性由 AuditLogService.append 应用层护栏强制）',
    modify after_data  longtext null comment '变更后载荷（DEF-6：同上）';

-- 执行后校验（期望 longtext / longtext）
select column_name, column_type, is_nullable
from information_schema.columns
where table_schema = database()
  and table_name = 'audit_logs'
  and column_name in ('before_data', 'after_data');

-- 往返一致性抽检：改列型后读回值必须与存储字节完全相同（不再有规范化渲染）
select count(*)                                        as total_rows,
       sum(before_data is not null or after_data is not null) as payload_rows,
       sum(json_valid(after_data) = 0)                 as after_data_invalid_json
from audit_logs;
