# SEC-NEW-MED-3 application-prod.yml 数据源凭证 env 注入 · 交付说明

- 执行会话：Qoder 补救包三波收口会话（owner 经 AskUserQuestion 授权「按 patch 交付」）
- 交付时间：2026-09-05 23:50 PDT
- 交付物：`application-prod-med3-delta.patch`（本目录）+ 守卫 `ProdConfigDeltaGuardTest` 第 5/6 条（6/6 绿@23:49）
- 为什么不直写 `application-prod.yml`：`.claude/helpers/sensitive-field-guard.cjs` 按设计阻断智能体写该文件，**不绕过**（同 owner-item3 / batch3 先例）。

## 1. 改什么

`application-prod.yml` master 数据源两行：

```diff
-          username: root
-          password: root
+          # SEC-NEW-MED-3：prod 凭证只走环境变量（必填无默认，未注入即启动失败，不再裸奔 root/root）；
+          # dev 侧 root/root 仅本地默认（application-dev.yml），与本文件无关
+          username: ${SPRING_DATASOURCE_USERNAME:}
+          password: ${SPRING_DATASOURCE_PASSWORD:}
```

语义：`${SPRING_DATASOURCE_USERNAME:}` 空默认 = 未注入环境变量时用户名为空串，任何真实连接立即失败——**fail-fast，不再静默用 root/root 连库**（CWE-798 关闭）。

## 2. 你要跑的一条命令

```bash
git apply --check docs/ipd-系统说明/验收/application-prod-med3-delta.patch && \
git apply         docs/ipd-系统说明/验收/application-prod-med3-delta.patch
```

apply 前实测（2026-09-05 23:48 PDT）：`git apply --check` rc=0，`--stat` 1 file changed，+4/-2。

## 3. apply 之后

1. 按 `ProdConfigDeltaGuardTest` 第 5 条的双态规矩：**把 patch 改名归档**（如 `application-prod-med3-delta.patch.applied-20260906`）或删除，否则测试转红提醒归档（batch3.patch 未归档误导后人的前车之鉴）。
2. 生产部署时注入 `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD`（建议与 DEF-5 的专用最小权限账号 `ipd_app` 一并落地——两张卡天然一对）。
3. dev 侧 `application-dev.yml` 的 root/root 保留不动（仅本地默认，卡面明示允许）。

## 4. 守卫（已落地，随本仓 mvn test 常绿）

`ProdConfigDeltaGuardTest`（4→6 例）：
- 第 5 条双态：prod 已 env 注入 → patch 必须归档不存在；仍 root/root → patch 必须待命且内容恰为这两行替换
- 第 6 条自校验：patch 头部三件套 + hunk 上下文与目标文件逐行对得上（纯 Java 复算 `git apply --check`，防畸形 patch）

范围边界：只守数据源凭证两行。prod 文件里上游遗留的 MONITOR_PASSWORD 默认值/社交登录 client-secret 占位串属 SEC-AUD 其他处置项，不在本卡越权扩大。
