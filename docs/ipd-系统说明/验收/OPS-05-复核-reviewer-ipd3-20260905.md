# OPS-05 站内通知与可执行待办事件接口 — 独立复核报告（reviewer-ipd3，2026-09-05）

## 判定：✅ 复核通过

交付 commit `e1cc6ae1`（复核以 commit 态为准；磁盘在途差异见 §5）。本卡范围内五条验收口径全部达标，证据可复现，无致命缺陷。

## 1. 测试独立重跑（不信交付者 XML，自跑自查）

```bash
export PATH="$HOME/tools/maven/bin:$PATH"; export JAVA_HOME="$HOME/tools/jdk-17/Contents/Home"
mvn -o -pl ruoyi-modules/ruoyi-ipd -Dtest=OPS05AcceptanceTest,P033AcceptanceTest,P1101AcceptanceTest \
    -Dsurefire.failIfNoSpecifiedTests=false test
# 仓库根执行；不带 -am 不带 clean；启动 23:09:17 PDT
```

- 结果：**BUILD SUCCESS（exit 0）**，Finished at 2026-09-05T23:09:32-07:00
- 本卡 XML（`target/surefire-reports/TEST-org.ruoyi.ipd.service.OPS05AcceptanceTest.xml`，mtime 23:09:32 = 本次自跑产物）经 ElementTree 独立解析：
  **tests=15 failures=0 errors=0 skipped=0** —— 无 @Tag 假绿形态（tests>0 证明在 dev profile 真实执行）。

## 2. 真库探针（只读，socket 凭证 `.codex/ipd-dev/config/mysql-client.cnf`，pymysql）

- `notification_events`：**24 列**；关键列全在——`receiver_id / event_type / kind / source_type / source_id / dedup_key / channel / retry_count / read_flag / read_at`
- 索引：`PRIMARY` + `uk_notify_dedup`(**UNIQUE**，去重键库级兜底) + `idx_notify_inbox`(3 列复合，收件箱查询) + `idx_notify_dispatch`(2 列，投递扫描)
- 行数 0（未起实例未接线业务，与交付声明一致）
- 父 `ruoyi-admin/src/main/resources/application.yml` 的 `tenant.excludes` 含 `notification_events`（键名核对 ✅）

## 3. 卡面验收口径逐条代码对照（commit e1cc6ae1 态）

| 验收口径 | 对照结论 |
|---|---|
| 统一接收者/业务来源/去重键/已读 | ✅ `publish()` dedupKey=`sourceType:eventType:sourceId:receiverId`；`DuplicateKeyException`→selectOne 返回既有行（同事件重复发布不重发）+ `uk_notify_dedup` 库级兜底；多接收者各自成行（key 含 receiver 维度）；`inbox` 恒带 receiver_id 条件 |
| 已读机制 | ✅ `markRead` 他人 ID 视为 NOT_FOUND（防探测）+ 已读幂等 + 条件 UPDATE（id+receiverId 双条件）；`markAllRead` 仅本人未读行 |
| 事件可重试（失败退避+死信） | ✅ `dispatchPending`：退避 `5·2^(n-1)` 分钟；`MAX_RETRIES=3`（第 1/2 次失败→FAILED+退避，第 3 次失败→DEAD 死信不再被扫中）；扫描谓词 `(PENDING|FAILED) AND (next_retry_at IS NULL OR ≤ now)` 仅取到期行 |
| 跨组知会与审批行动分开 | ✅ `kind` 硬校验 `Set.of("FYI","ACTION")`，非法值拒绝；FYI/ACTION 各自成行 |
| 一期 Mock 渠道标识清楚 | ✅ `MockNotificationChannel.CODE="MOCK"` 落 `channel` 列；类 javadoc 明示「SENT+MOCK ≠ 用户已在站外收到」契约；真实渠道后续新增实现不改接口 |
| （加固项）并发双消费守卫 | ✅ 投递成功走条件 UPDATE `status IN (PENDING,FAILED)→SENT`，0 行=并发已处理→skipped |
| （加固项）防越权伪造 | ✅ Controller 端点面仅 `GET inbox / GET unread-count / POST {id}/read / POST read-all / POST dispatch-pending`——**无 HTTP 发布入口**，receiver 恒从会话推导 |

## 4. HTTP 层说明

16045 活体（`ruoyi-admin-ch.jar`，22:04 PDT 起，早于本卡交付 22:38）为旧构建：老端点 `/api/v1/products` 返回 401 认证墙=路由存在，而 `/api/v1/notifications/inbox` 返回 `No endpoint`=实例不含本卡代码。**该实例不可作本卡 HTTP 证据；本卡亦未宣称 HTTP 验收**（未起实例、接线属下游卡），故不构成缺口。范围外声明（业务发布接线属下游卡/调度轮询待 OPS-04 scheduler 合入/真实外部渠道）与镜像登记一致，判定有效。

## 5. 磁盘态与 LOW 备注（不阻塞）

- `NotificationService.java` 工作树有兄弟会话在途 +22 行（疑似业务发布接线）；本次复跑 15/15 在**含该改动的磁盘态**通过——交付态与追加态均绿。
- LOW（纯文档措辞）：`MAX_RETRIES` 字段 javadoc 写「第 1/2/3 次失败仍 FAILED，此后转 DEAD」，实现与测试锁定的是**第 3 次失败即 DEAD**（newCount≥3）。行为以测试 `dispatch_failure_backoffThenDead` 为准，建议后续顺手修正注释。

## 6. 结论

本卡按「Mockito 单测 + 真库结构落位」口径交付完整、诚实（自报 ◇ 不标 done 的理由成立）。**✅ 复核通过**。done 收口建议由协调会话视 HTTP 端到端补验（需含本卡代码的新实例）与 OPS-04 调度接线进度统一裁决。
