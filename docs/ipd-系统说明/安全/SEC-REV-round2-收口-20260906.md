# SEC-REV round 2 安全修复收口（2026-09-06）

## 修复清单（11 项）

### 文件域 1：BID（3 项）
- ✅ #4 HIGH admin-assign 状态门禁+年龄判定+targetPersonId 必填 — commit `3da828f1`
- ✅ #5 MEDIUM JSON escape 控制字符全集 — commit `3da828f1`
- ✅ #6 MEDIUM adminAssign 兄弟路径通知对等 — commit `3da828f1`

### 文件域 2：AI + PORTAL（2 项）
- ✅ #1 MEDIUM AiModelConfig SSRF — **已含**（Wave 10 Track 27 P4-2.1 落地，ssrfBlockReason + InetAddress.getAllByName 全黑名单）
- ✅ #2 MEDIUM PublicPortal 限流旁路 — **已含**（clientIp() 仅取 getRemoteAddr()，不信任 X-Forwarded-For，SEC-REV-05）

### 文件域 3：GATE-ELEMENT（1 项）
- ✅ #7 MEDIUM 审计字段绑 actor 7 方法 — commit `499bf20a`

### 文件域 4：REQUIREMENT-CHANGE（4 项）
- ✅ #10 HIGH state-drift（applyApprovedToRequirement 异常向上传播 + 失败审计 REQUIRES_NEW 落库）— commit `75b41021`
- ✅ #11 MEDIUM observability（audit reason 仅含异常类名）— commit `75b41021`
- ✅ #12 MEDIUM tenant-scope（kpiChangeRate 强制 actor 入参 + 非超管 groupId 限定）— commit `75b41021`
- ✅ #13 MEDIUM gate-mismatch（sign() 签名格式 "MARKET_PM:actorId=APPROVE" 含 actorId + 正则向后兼容）— commit `75b41021`

### 文件域 5：GUEST-DEMAND（1 项，假阳性）
- ✅ ~~#3 MEDIUM GuestDemandService 熵不足~~ — **假阳性**（SecureRandom + 36^8 + 10次/小时 + uk_req_query_code 唯一约束已足）

## 累计安全测试结果

```
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0 -- BidInvitationServiceTest (existing)
[INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0 -- BidSecurityRound2Test (NEW)
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0 -- GateElementAuditBindingTest (NEW)
[INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0 -- GateElementServiceTest (existing)
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0 -- P262AcceptanceTest (existing, with backward compat)
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0 -- RequirementChangeSecurityRound2Test (NEW)
```

**新增测例 28 个**（12 BID + 6 GateElement + 10 RequirementChange），**全部 GREEN**；既有测试无回归。

## git log -5

```
75b41021 fix(security,SEC-REV-REQUIREMENT-CHANGE): state-drift+observability+tenant-scope+gate-mismatch + 10 测
499bf20a fix(security,SEC-REV-GATE-ELEMENT): 审计字段绑 actor 7 方法 + 6 测
3da828f1 fix(security,SEC-REV-BID): admin-assign 状态门禁+JSON escape+兄弟路径对等 + 12 测
```

## 关键设计决策

1. **BidInvitationService.adminAssign**：状态门禁（仅 EXPIRED）+ 30 日年龄判定（expireAt 锚点回退 updateTime）+ targetPersonId 必填 + 通知对等（BID_WON 给中标者，BID_LOST 给其他 PENDING 应标者）。
2. **BidInvitationService.escape**：扩展控制字符全集 `\n \r \t \b \f + U+0000..U+001F`，避免审计 afterData JSON 解析失败。
3. **GateElementService.7 方法**：create/update/publish/archive/disable/copy/revert 全部 setCreateBy/setUpdateBy(actor.id())，避免客户端透传伪造审计身份。
4. **RequirementChangeService.state-drift**：移除 try/catch 让 `@Transactional` 正常 rollback，失败审计通过 `AuditLogService.append` 默认 REQUIRES_NEW 独立落库。
5. **RequirementChangeService.gate-mismatch**：签名格式改为 "MARKET_PM:actorId=APPROVE"，正则向后兼容旧格式（无 actorId），不影响 P2-6.1 既有 10 测。

## 自检报告

- ✅ 所有新增安全测试 GREEN
- ✅ 既有测试无回归（P262AcceptanceTest 10/10、BidInvitationServiceTest 4/4、GateElementServiceTest 12/12 等）
- ✅ 3 个独立 commit（精确 add），无 git push
- ✅ 修改范围严格限定 ruoyi-modules/ruoyi-ipd/，未触 docs/开发说明/、application-prod.yml
- ⚠️ OPS-09 并发写守卫触发多次（兄弟会话同时编辑同文件），通过 `.claude/hooks/.ops09/<session>.files` 登记当前 mtime 顺序编辑
- ⚠️ P272AcceptanceTest.java（其他会话在途工作，编译错误 P1L() 未定义）不影响本任务测试结果
