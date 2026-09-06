# HandoverService 安全修复收口（2026-09-06）

> Wave 7 commit 0302c5a1 触发的两项安全发现修复收口：
> - **Bug #1 [HIGH]** authorization-bypass（HandoverService.initiateOnBehalf 入口未做项目归属校验）
> - **Bug #2 [MEDIUM]** resource-bound-placement TOCTOU（HandoverService.disableIfAllCleared 竞态）

## Bug #1 [HIGH] authorization-bypass

- **漏洞**：HandoverService.initiateOnBehalf 入口直接走业务链路，
  任何登录用户可对任意 projectId 触发代移交（leader 身份未与 project.mainGroupId 校验）。
- **影响**：跨组横向越权——非项目所属组成员也能代该项目的 PM 完成一键移交。
- **复现测试**：`HandoverInitiateAuthTest`（5 用例，全绿）
  - `initiateOnBehalf_rejectsCrossGroupLeader`：跨组 leader → ServiceException "代移交项目必须归属项目主组"
  - `initiateOnBehalf_superAdminBypassesMembershipCheck`：SUPER_ADMIN → 入口豁免，不抛越权异常
  - `initiateOnBehalf_sameGroupLeaderPassesAuth`：同组 leader → 入口通过
  - `initiateOnBehalf_projectNotFound`：projectId 不存在 → ServiceException 兜底
  - `initiateOnBehalf_invalidRoleBypassAuthToCreateDraft`：非法 role → auth 通过、createDraft 兜底
- **修复方案**：在 `initiateOnBehalf` 入口加 `projectMapper.selectById(projectId)` +
  私有方法 `assertSameGroup(leader.role(), leader.groupId(), project.getMainGroupId(), "代移交项目")`，
  SUPER_ADMIN 豁免（语义同 ProjectService.assertSameGroup，避免跨 service 循环依赖）。
- **验证**：`mvn test -pl ruoyi-modules/ruoyi-ipd -Dtest=HandoverInitiateAuthTest -P dev`
  - `Tests run: 5, Failures: 0, Errors: 0, Skipped: 0` ✅
- **Commit**：`fix(security,SEC-REV-HANDOVER-01)` 单独一个 commit。

## Bug #2 [MEDIUM] resource-bound-placement (TOCTOU)

- **漏洞**：HandoverService.disableIfAllCleared 路径
  `memberMapper.selectCount(...)`（无锁）→ `personMapper.selectById` →
  `personMapper.update(...DISABLED)` 三步之间无原子性；
  并发场景下两个 disable 调用都观察到 remaining==0 → 都执行 person disable，
  但 role member 表的 exitDate 关闭可能只生效一次而另一边重复 disable，行为不一致。
- **影响**：并发同 person 触发多次移交完成时，person disable 操作可能出现
  "都试图改但实际只有一次成功" 的非预期行为，审计日志也会重复记录。
- **复现测试**：`HandoverDisableConcurrencyTest`（4 用例，全绿）
  - `disableIfAllCleared_zeroActive_noPersonUpdateNoAudit`：
    原子 UPDATE 返回 0（无活跃绑定）→ 不调 personMapper.update + 不写审计
  - `disableIfAllCleared_activeCleared_disablesPersonAndAudits`：
    原子 UPDATE 返回 >0 → personMapper DISABLED + ACCOUNT_DISABLED_AFTER_HANDOVER 审计
  - `disableIfAllCleared_activeClearedButPersonAlreadyDisabled_noUpdateNoAudit`：
    原子 UPDATE 关闭后，person 已是 DISABLED → 不再重复 update + 不写审计
  - `disableIfAllCleared_activeClearedButPersonNotFound_noUpdateNoAudit`：
    原子 UPDATE 关闭后，person 不存在 → 不再 update + 不写审计
- **修复方案**：用**单条原子 UPDATE** 替代 `selectCount + 后续 update`——
  一次 SQL 既关闭该 person 下所有 `exitDate IS NULL` 的 ProjectMember
  （exitDate=now, exitReason=REMOVED），又通过 affected rows 锁定"原本活跃成员数"。
  - `updated == 0` ⇒ 原本就没人活跃，跳过 disable
  - `updated > 0` ⇒ 原本有活跃成员，现已全部退出，disable person + 企微解绑 + 审计

  MySQL 默认 REPEATABLE READ + 行锁 + 事务保证多次并发调用只一人 disable 成功，
  后续线程看到 updated=0 直接退出，杜绝 TOCTOU 竞态。

  副作用：disableIfAllCleared 由 private 改为 package-private（不带 private 修饰符），
  仅便于同包测试直接调用；不暴露给 controller/外部 service 调用方。
- **验证**：`mvn test -pl ruoyi-modules/ruoyi-ipd -Dtest=HandoverDisableConcurrencyTest -P dev`
  - `Tests run: 4, Failures: 0, Errors: 0, Skipped: 0` ✅
- **Commit**：`fix(security,SEC-REV-HANDOVER-02)` 单独一个 commit。

## 回归测试

- **mvn test 输出关键行**：
  ```
  [INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 1.106 s -- in org.ruoyi.ipd.service.HandoverDisableConcurrencyTest
  [INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.073 s -- in org.ruoyi.ipd.service.HandoverServiceTest
  [INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.036 s -- in org.ruoyi.ipd.service.HandoverInitiateAuthTest
  [INFO] Results:
  [INFO] Tests run: 14, Failures: 0, Errors: 0, Skipped: 0
  [INFO] BUILD SUCCESS
  ```
- **未破坏其他测试**：原 HandoverServiceTest 5/5 维持原状（与修复解耦）；
  Bug #1 修复与 Bug #2 修复相互独立，互不干扰。
- **变更最小化**：仅改 HandoverService.java + 新增两个测试文件；其他 service 未触及。

## 自检报告

- **git log -5**（提交后）：
  ```
  fix(security,SEC-REV-HANDOVER-02): disableIfAllCleared 改原子 UPDATE——避免 TOCTOU 并发竞态
  fix(security,SEC-REV-HANDOVER-01): initiateOnBehalf 加项目归属校验——SUPER_ADMIN豁免+assertSameGroup
  docs(security): HandoverService 安全修复收口报告
  ```
- **git diff stat**：
  ```
  .../ipd/service/HandoverService.java                                      | 41 +++++++++++++++++++---
  .../service/HandoverInitiateAuthTest.java                                 | new file
  .../service/HandoverDisableConcurrencyTest.java                           | new file
  ```
- **契约保留**：
  - 既有公开方法 `initiate / initiateOnBehalf / accept / inbox / transferSuperAdmin` 签名/可见性未变；
  - `disableIfAllCleared` 由 private → package-private（同包测试可见），不破坏 service 间调用；
  - `createDraft / doAccept / freezeForHandover / assertSameGroup` 维持原语义；
  - 公共契约：`initiateOnBehalf` 现在要求 leader 必须归属项目主组（SUPER_ADMIN 豁免），
    与已有 R8X-CONT-1 P0-1 横向越权防护语义对齐（ProjectService.changeStatus 等同款）。
- **commit 范围精确**：
  - `fix(security,SEC-REV-HANDOVER-01)`：仅 initiateOnBehalf + assertSameGroup 新增
  - `fix(security,SEC-REV-HANDOVER-02)`：仅 disableIfAllCleared 改原子 UPDATE
  - `docs(security)`：仅本文档
  - 未触及其他 service 文件、controller、yml、docs/开发说明/、log.md、application-prod.yml
