# SEC-AUD MED-5 Rebuttal（2026-09-07）

> 本文档归档 SEC-AUD-2026-09-05 跨 commit 安全审查中 MED-5（api-decrypt.privateKey 明文）
> 「字节级 claim」已被 R9a 等 4 个 commit 清零的事实，避免后续审计被旧文本误导。

## 一、原 audit claim（2026-09-05）

`docs/ipd-系统说明/验收/SEC-AUD-2026-09-05-跨commit安全审查.md` § MED-5：
> api-decrypt.privateKey 是真实 RSA 私钥（PEM 编码字符串），公钥也在配置中

该描述**对审查当刻的字节状态完全准确**——`b61c7f35` 时 `application.yml:254-258` 存在明文 PEM 字面量。

## 二、已落地修正 commit 时间线

| commit | 时间 (PDT) | 改动 |
|---|---|---|
| `92dddb11` | 2026-09-05 19:29 | **R8-P0-4**：privateKey `MIIBVA...PEM` → `${API_DECRYPT_PRIVATE_KEY:}` |
| `cd0a1151` | 2026-09-05 20:55 | **RSA-ROTATE**：publicKey 512 位弱密钥换 2048 位新字面量，私钥进 `.codex/ipd-dev/config/` 不进 git |
| `76888bbf` | 2026-09-05 后续 | **Batch-3#23**：application-prod.yml `privateKey: ${API_DECRYPT_PRIVATE_KEY:}` 显式 prod 覆盖 |
| `05fd3f65` | 2026-09-05 21:15 | **R9a**：publicKey `MIIBIjAN...PEM` → `${API_DECRYPT_PUBLIC_KEY:}`（公钥也彻底 env-化）|

## 三、当前字节级状态（HEAD = a712bbb9）

- `ruoyi-admin/src/main/resources/application.yml:353/356`
  - `publicKey: ${API_DECRYPT_PUBLIC_KEY:}`
  - `privateKey: ${API_DECRYPT_PRIVATE_KEY:}`
  - `enabled: false`（默认关闭）
- `ruoyi-admin/src/main/resources/application-prod.yml:288-291`
  - `privateKey: ${API_DECRYPT_PRIVATE_KEY:}`（prod 显式占位符）
- 全仓 `grep -rn "BEGIN.*PRIVATE KEY"` 仅命中 alipay-public-key 脱敏占位符（MIIB****DAQAB，非 api-decrypt）

## 四、PEM 字面量扫描证据

```bash
grep -rn "MIIBVA\|MIICC\|BEGIN PRIVATE KEY\|BEGIN RSA PRIVATE KEY\|MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBA" \
  ruoyi-*/src/main/resources/ ruoyi-common/ 2>/dev/null
# 仅命中 alipay-public-key: MIIB**************DAQAB（脱敏占位符）

grep -rn "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAyoQR7Z9ga7EXmJDfpzpjoAP2Y"
# 0 命中（cd0a1151 写过的 2048 位公钥字面量已被 R9a 替换）
```

## 五、测试覆盖

- `ApiDecryptTest` 不存在（`find ruoyi-ai -name "*ApiDecrypt*"` 0 命中）
- `enabled: false` 默认值下加密链路不启动，运行时单测覆盖非必需
- 历史相关测试 `88bee703 SEC-NEW-MED-4 RSA 私钥 PEM 字面量反向用例` 已落库（清理 yml 注释中残留 PEM字符串）

## 六、结论

| 维度 | 状态 |
|---|---|
| privateKey 形态 | `${API_DECRYPT_PRIVATE_KEY:}` 占位符（PASS） |
| publicKey 形态 | `${API_DECRYPT_PUBLIC_KEY:}` 占位符（PASS，超 MED-5 范围） |
| 全仓 PEM 字面量扫描 | 0 命中 api-decrypt 相关字面量（PASS） |
| 加密开关 | 默认 `enabled: false`（PASS） |
| Prod override | 已显式 env 占位符（PASS） |
| 关联 commit 落 main | 92dddb11 / cd0a1151 / 76888bbf / 05fd3f65 全部 PASS |
| Kanban 卡 f38081fb 状态 | done（与字节一致） |

## 七、衍生动作

- [x] Kanban 卡 f38081fb 状态 done 复核一致
- [ ] 后续审计文档引用 SEC-AUD-2026-09-05 时**必须**附加本 rebuttal，避免被陈旧文本误导
- [ ] 若需 ApiDecrypt 单元测试可后续单独派单（与 MED-5 闭环解耦）

## 八、关联文档

- `docs/ipd-系统说明/验收/SEC-AUD-2026-09-05-跨commit安全审查.md`（原 audit，MED-5 章节）
- `docs/ipd-系统说明/log.md:28/:36`（R8-P0-4 闭环登记）
- `docs/ipd-系统说明/治理轮/零漂移-看板纳管材料-32卡-2026-09-05.md`（R8-P0-4 行）