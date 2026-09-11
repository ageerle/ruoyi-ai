# P1 项3 application-prod.yml 四项覆盖 · 交付说明

- 执行会话：主协调（owner 2026-09-05 指令项3）
- 时间窗：2026-09-05 20:50–20:59 PDT
- 交付物：`application-prod-owner-item3-delta.patch`（25 行）+ 守卫 `ProdConfigDeltaGuardTest`（4 例）
- 为什么不直写 `application-prod.yml`：`.claude/helpers/sensitive-field-guard.cjs` 按设计阻断智能体写该文件。owner 指令里写的「需用户/CI 手动」就是这一条，**不绕过**。

## 1. owner 列的 4 项，实测只有 1 项是真缺项

| # | owner 项 | prod 现状（HEAD `cd0a1151`，20:56 实测） | 判定 |
|---|---|---|---|
| ① | `demo.enabled=false` | prod 无 `demo` 段；父 `application.yml:396-399` 已 `enabled: false`（R8-P0-2） | 语义**已成立**（靠继承），本 patch 追加显式 pin |
| ② | `sa-token` 占位符 | prod 无 `jwt-secret-key` 段；父 `application.yml:167` = `${SA_TOKEN_JWT_SECRET_KEY:}` 无默认 | 语义**已成立**，本 patch 显式重申 |
| ③ | springdoc 关闭 | 父 `application.yml:305-308` `api-docs.enabled: true`，**prod 无任何覆盖** | **唯一真缺项** → `/v3/api-docs` 与 swagger-ui 会带着生产配置暴露全部接口面 |
| ④ | actuator 收窄 | prod 无 `management` 段（grep 零命中）；父 `application.yml:365-373` = `include: health,info,metrics` + `show-details: WHEN_AUTHORIZED` | 语义**已成立**，且已有 `ActuatorNarrowTest` 正向守住 → **不进 patch**（加了反而是第二个需要维护的事实源） |

结论：本 patch 净效果 = 关掉 ③ + 把 ①② 从「隐式继承」升级为「显式 pin，防父基线被翻」。

## 2. 与兄弟 Batch-3 的关系（两套「4 项」不是同一组）

兄弟 `182382b3` 交付过 `application-prod-batch3.patch`，内容是 `p6spy` / `connectionTimeout` / `IPD_INITIAL_PWD` / `api-decrypt.privateKey` —— 与 owner 本项的 ①②③④ **零重叠**。该 patch 已由用户授权路径被 `76888bbf`（20:50）消费落地，兄弟收口文档 `Wave3-Batch-3-收口-20260905.md:113` 已自行划掉「执行 patch」一行，**无需我回写**。

复测（20:56:51）：

```
git apply --check docs/ipd-系统说明/验收/application-prod-batch3.patch
  → error: patch failed: ruoyi-admin/src/main/resources/application-prod.yml:47
  → rc=1        ← 正常：内容已落地，这个 patch 现在是「已消费交付物」，不可再 apply
```

⚠️ 因此：**不要再执行 batch3.patch**，也不要按它做二次合并；本目录新增的 delta patch 才是当前唯一待执行项。

## 3. 执行方式（用户或 CI 一条命令）

```bash
git apply --check docs/ipd-系统说明/验收/application-prod-owner-item3-delta.patch && \
git apply         docs/ipd-系统说明/验收/application-prod-owner-item3-delta.patch
```

apply 前实测：`git apply --check` rc=0，`--stat` 显示 `1 file changed, 18 insertions(+)`（20:56:20）。

apply 后请顺手把本 patch 归档（删除或改名加 `.consumed`）——守卫的第 3 条会因「prod 已含 `api-docs.enabled: false` 但 patch 仍躺在交付目录」而变红，这正是为了防止 batch3.patch 那类「已消费但看起来仍可执行」的僵尸交付物重演。

## 4. 红绿双证

```
mvn -o -pl ruoyi-modules/ruoyi-ipd -Dtest='ProdConfigDeltaGuardTest,CredentialLiteralGuardTest,IpdMockDataInitializerTest' test
  绿 20:59:04–20:59:10   Tests run: 13, Failures: 0, Errors: 0（项3 守卫 4 例全过）

红证（对 patch 交付物本身做破坏注入，逐个验证守卫真会红）
  A 20:59:24–26  删掉 delta patch        → prodSpringdocGapMustBeTracked FAIL（:100 缺口失控）
  B 20:59:37–39  把头改回畸形 `aruoyi-admin/…` → deltaPatchIsActuallyApplicable FAIL（:119 头不合法）
  C 20:59:39–41  把 hunk 头改成 @@ -1,3 @@  → 同一用例 FAIL（:161 上下文错位，等价 batch3 的 rc=1）
  还原后 sha256 与原件一致（39f47a67bc3ff5e1），git apply --check 复验 rc=0
```

红证 B 不是假想敌：本 patch 的第一版就是用 `git diff --no-index` + `sed` 改路径时，把 git 前缀 `a/`、`b/` 的斜杠一起吃掉，产出了永远 apply 不上的畸形头。第 4 条断言（纯 Java 复算 `git apply --check` 的上下文判据）就是为了下次不再靠人眼。

## 5. 未覆盖 / 需 owner 决定

- **不泛化「僵尸 patch」规则**：守卫只约束自己的 delta patch。若把规则推广到扫描全部 `application-prod-*.patch`，今天就会因兄弟的 batch3.patch 已消费而红——那属兄弟/owner 的资产处置，不在此越权。
- **prod `spring.boot.admin.client.password` 仍有内联默认值**（`application-prod.yml:13`，弱默认）。因该段 `enabled: false`（L5）故当前不可达；owner 项3 未列它，本批**未改**，登记备查。
- prod 的 websocket 具体 origin（独立部署场景）由兄弟收口文档 L116 挂账，本批不重复。

## 6. 消费登记（R9c，2026-09-05 21:5x）

- owner 授权「立即执行剩余待办」后，本 patch 已由 R9c 会话 `git apply` 落地（application-prod.yml +18 行），并按 `ProdConfigDeltaGuardTest` case 3 的生命周期契约**改名归档**为 `*.applied-20260905`。
- 后续执行方请勿重复 apply（对齐 batch3 前车之鉴）。
