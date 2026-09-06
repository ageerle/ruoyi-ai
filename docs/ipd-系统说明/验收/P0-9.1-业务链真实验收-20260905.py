#!/usr/bin/env python3
# P0-9.1 P0 真实登录、删除、审计业务链验收（真 HTTP + 真库双侧，非 Mock）
# AC 来源：看板 P0-9.1「真实DB/HTTP从登录到改密、发起删除、审核归档、审计验链；
#          包含越权和失败回滚；记录请求与DB前后差异」+ P0验收§4「不能只 health UP」
# 覆盖源 AC：AC-DEL-01（无直删入口）/AC-DEL-02（终审通过→归档区+审计链完整保留）
#            AC-AUTH-07 族（改密后旧 token 即时撤销）/AC-AUD-03（防篡改链自洽，DEF-4 修复后）
# 目标实例：16045（ruoyi-admin-p091b.jar）内嵌件指纹逐个 javap 复验：
#   ruoyi-ipd@16:18（含 5b95a9d0 DEF-4 四层根因修复）+ 单类外科替换 DeletionArchiveService@16:50（DEF-8）
#   ruoyi-system@12:21（含 056640ca 的 UserActionListener.isBaselineLoginType 守卫）
# 为何用外科替换而非整模块重建：工作树有兄弟 35 个在途未提交主源码改动（序列化重构），
#   整模块打包会把未发布代码混进验证 jar → 证据无法绑定到确定提交。
# 部署链教训（DEF-7 假红）：首跑用的 ruoyi-admin-def4.jar 内嵌 ~/.m2 陈旧的 ruoyi-system@05:29，
#   缺该守卫 → 改密后 revokeAll 触发 SaJwtException「jwt loginType 无效」→ 业务已成功却返回
#   500/90001。结论：验证用 fat jar 必须核查**所有**内嵌模块新鲜度，不止自己改的那个。
#
# 安全设计（并发多会话共享库，必须零残留）：
#   改密靶子 = 孙研发（must_change_pwd=1 且 last_login_at=NULL，从未登录 → 兄弟会话不可能在用）；
#   改密前捕获 password_hash，脚本 finally 无条件用 root 还原原哈希 + must_change_pwd=1，
#   并复登验证还原生效 → 环境无痕。
#   删除靶子 = 本脚本自建的 throwaway cert_template（不触碰任何真实业务行）。
#   登录限流：同 IP 同账号 60s 最多 5 次（IpdAuthController @RateLimiter）→ 每账号 ≤4 次并加 sleep。
import json, os, subprocess, time, urllib.parse, urllib.request, urllib.error

BASE = "http://localhost:16045"
MYSQL = "/Users/mac/Documents/ruoyi-ai/.codex/ipd-dev/software/mysql-8.0.46-macos15-arm64/bin/mysql"
CNF = "/Users/mac/Documents/ruoyi-ai/.codex/ipd-dev/config/mysql-app.cnf"        # ipd_app（应用身份）
ROOTCNF = "/Users/mac/Documents/ruoyi-ai/.codex/ipd-dev/config/mysql-client.cnf"  # root（还原用）
REPO = "/Users/mac/Documents/ruoyi-ai"
TS = time.strftime("%Y%m%d-%H%M%S")

# 改密靶子（从未登录的种子账号）
PWD_USER, PWD_OLD = "孙研发", "Ipd@123456"
PWD_NEW = "Qa09-P091-x7Km2vLp"

HEAD = subprocess.run(["git", "-C", REPO, "rev-parse", "--short", "HEAD"],
                      capture_output=True, text=True).stdout.strip()
JAR = REPO + "/.codex/ipd-dev/runtime/ruoyi-admin-p091b.jar"
JAR_MTIME = time.strftime("%H:%M:%S", time.localtime(os.path.getmtime(JAR))) if os.path.exists(JAR) else "?"


def sql(q, cnf=CNF):
    r = subprocess.run([MYSQL, "--defaults-file=" + cnf, "ipd_dev", "-N", "-e", q],
                       capture_output=True, text=True)
    return r.stdout.strip(), r.returncode, r.stderr.strip()


def q1(query, cnf=CNF):
    return sql(query, cnf)[0]


def req(method, path, token=None, body=None):
    r = urllib.request.Request(BASE + path, method=method)
    if token:
        r.add_header("Authorization", "Bearer " + token)
    data = json.dumps(body).encode() if body is not None else None
    if data:
        r.add_header("Content-Type", "application/json")
    try:
        with urllib.request.urlopen(r, data=data, timeout=25) as resp:
            return resp.status, json.loads(resp.read().decode())
    except urllib.error.HTTPError as e:
        try:
            return e.code, json.loads(e.read().decode())
        except Exception:
            return e.code, {}
    except Exception as ex:
        return -1, {"err": str(ex)}


def login(u, p):
    s, b = req("POST", "/api/v1/auth/login", body={"username": u, "password": p})
    d = b.get("data") or {}
    return s, b.get("code"), d.get("token"), d.get("mustChangePwd"), d.get("scope")


results, evidence = [], {}


def check(name, actual, expect, note=""):
    ok = (actual == expect)
    results.append({"项": name, "期望": expect, "实际": actual, "结论": "PASS" if ok else "FAIL", "备注": note})
    print(("  PASS  " if ok else "  FAIL  ") + name + "  期望=%s 实际=%s %s" % (expect, actual, ("← " + note) if note and not ok else ""))
    return ok


def verify_state(tok):
    s, b = req("GET", "/api/v1/audit-logs/verify", token=tok)
    d = b.get("data") or {}
    return s, d.get("chain"), len(d.get("broken") or []), [int(x) for x in (d.get("broken") or [])]


def attribute_broken(seqs):
    """DEF-6 归因探针：断裂行是否 100% 携带 JSON 载荷（before_data/after_data 非空）。

    MySQL json 列会「规范化渲染」：键按（字节长度 → 字典序）重排、成员间插 \", \"/\": \" 空格、
    1e3 → 1000.0（实测 jprobe 临时表，2026-09-05）；而写入侧哈希用 Jackson 紧凑串 →
    带载荷的审计行写完立即被判断裂。返回（断裂总数，其中带 JSON 载荷数）。"""
    if not seqs:
        return 0, 0
    lst = ",".join(str(int(x)) for x in seqs)
    tot = int(q1("SELECT COUNT(*) FROM audit_logs WHERE seq IN (%s)" % lst) or 0)
    withjson = int(q1("SELECT COUNT(*) FROM audit_logs WHERE seq IN (%s) "
                      "AND (before_data IS NOT NULL OR after_data IS NOT NULL)" % lst) or 0)
    return tot, withjson


def chain_checks(tag, tok):
    """审计链断言 + DEF-6 归因：链断裂时区分「DEF-4 四层修复失效」与「DEF-6 JSON 载荷」。"""
    vs, chain, nb, seqs = verify_state(tok)
    check("%s 审计链 chain=OK" % tag, chain, "OK", "broken=%s" % seqs[:8])
    check("%s 断裂数=0" % tag, nb, 0, "broken=%s" % seqs[:8])
    tot, wj = attribute_broken(seqs)
    if tot:
        check("%s 断裂 100%% 归因 DEF-6（JSON 载荷行，非 DEF-4 回归）" % tag, wj, tot,
              "断裂%d行/带载荷%d行" % (tot, wj))
        evidence["DEF-6断裂行@" + tag] = q1(
            "SELECT GROUP_CONCAT(CONCAT(seq,':',action)) FROM audit_logs WHERE seq IN (%s)"
            % ",".join(str(int(x)) for x in seqs))
    return vs, chain, nb, seqs


def ok(b):
    return b.get("code") == 0


print("=" * 78)
print("P0-9.1 业务链真实验收  HEAD=%s  jar=%s(@%s)  实例=%s  开始=%s"
      % (HEAD, os.path.basename(JAR), JAR_MTIME, BASE, time.strftime("%F %T")))
print("=" * 78)

# ---------------- L0 基线 ----------------
print("\n=== L0 基线：审计链态 + 库快照 ===")
rows0 = int(q1("SELECT COUNT(*) FROM audit_logs") or 0)
seq0 = int(q1("SELECT MAX(seq) FROM audit_logs") or 0)
dr0 = int(q1("SELECT COUNT(*) FROM deletion_requests") or 0)
ct0 = int(q1("SELECT COUNT(*) FROM cert_templates") or 0)

# ---------------- L1 登录腿 ----------------
print("\n=== L1 真实登录（4 主账号 + 首登强制改密门 + 未登录拒绝）===")
TOK = {}
for role, u, p in (("ADMIN", "ipd-admin", "SZviX9kmMo7Tg92kjBax6i9MVsfBV-eh"),
                   ("LEADER", "ipd-leader", "i7fVdG7aIgwG8l37-4f7tqNVEQygfWHJ"),
                   ("MARKET", "陈市场", "Qa03-Mkt-7pLx9wVz"),
                   ("RD", "ipd-rd", "iQc9xozo-KelvRPdIQvknkxMZIiij39F")):
    s, c, t, mcp, scope = login(u, p)
    TOK[role] = t
    check("L1 %s 登录 HTTP200/code0" % role, (s, c), (200, 0), "scope=%s" % scope)
    check("L1 %s 拿到 token" % role, bool(t), True)
    check("L1 %s 非首登态 mustChangePwd=false" % role, mcp, False)
    time.sleep(0.4)

s, c, pwd_tok, pwd_mcp, pwd_scope = login(PWD_USER, PWD_OLD)
check("L1 改密靶子 %s 登录成功" % PWD_USER, (s, c), (200, 0))
check("L1 首登强制改密门 mustChangePwd=true（AC：未改密不得放行）", pwd_mcp, True)
evidence["改密靶子scope"] = pwd_scope

s, b = req("GET", "/api/v1/auth/me")
check("L1 未登录访问 /me HTTP401", s, 401)
check("L1 未登录 code=20001", b.get("code"), 20001)

# 基线验链（DEF-4 修复后应为 OK/0）
vs, chain0, n0, seqs0 = chain_checks("L1 基线（DEF-4 修复存续）", TOK["ADMIN"])
evidence["基线"] = {"rows": rows0, "maxSeq": seq0, "deletion_requests": dr0, "cert_templates": ct0,
                    "chain": chain0, "broken": n0}

# ---------------- L2/L3 改密腿（含失败留痕 + 可逆还原） ----------------
h0 = q1("SELECT password_hash FROM persons WHERE username='%s'" % PWD_USER)
mcp0 = q1("SELECT must_change_pwd FROM persons WHERE username='%s'" % PWD_USER)
print("\n=== L2 改密失败腿：原密码错误 → 拒绝 + 独立事务留痕 + 零写 ===")
try:
    s, b = req("POST", "/api/v1/auth/change-password", token=pwd_tok,
               body={"currentPassword": "WrongPwd-9x", "newPassword": PWD_NEW})
    check("L2 原密码错误被拒（code!=0）", ok(b), False, "HTTP=%s code=%s msg=%s" % (s, b.get("code"), b.get("msg")))
    evidence["L2失败腿"] = {"HTTP": s, "code": b.get("code"), "msg": b.get("msg")}
    h_after_fail = q1("SELECT password_hash FROM persons WHERE username='%s'" % PWD_USER)
    check("L2 失败后 password_hash 零变更（回滚/未写）", h_after_fail == h0, True)
    time.sleep(1.0)
    # 真实契约：AuditAttemptService 把 Outcome 枚举写进 action 列（'FAILURE'），
    # attemptedAction 落在 after_data JSON 内 → 按此断言，不按臆想的 action 名（首跑因此假红）
    rej = int(q1("SELECT COUNT(*) FROM audit_logs WHERE action='FAILURE' "
                 "AND after_data LIKE '%PASSWORD_CHANGE_REJECTED%'") or 0)
    check("L2 失败仍留痕：action=FAILURE 且 after_data 含 PASSWORD_CHANGE_REJECTED（REQUIRES_NEW 不被业务回滚）",
          rej >= 1, True, "实际=%d" % rej)
    evidence["L2失败审计行"] = rej

    print("\n=== L3 改密成功腿：真改密 + 旧 token 即时撤销 + DB 前后差异 ===")
    s, b = req("POST", "/api/v1/auth/change-password", token=pwd_tok,
               body={"currentPassword": PWD_OLD, "newPassword": PWD_NEW})
    check("L3 改密 HTTP200/code0", (s, b.get("code")), (200, 0), "msg=%s" % b.get("msg"))

    s2, b2 = req("GET", "/api/v1/auth/me", token=pwd_tok)
    check("L3 改密后旧 token 立即失效 HTTP401（revokeAll，AC-AUTH-07 族）", s2, 401)

    h1 = q1("SELECT password_hash FROM persons WHERE username='%s'" % PWD_USER)
    mcp1 = q1("SELECT must_change_pwd FROM persons WHERE username='%s'" % PWD_USER)
    check("L3 DB 差异①：password_hash 已变更", h1 != h0, True)
    check("L3 DB 差异②：must_change_pwd 1→0（首登标记清除）", (mcp0, mcp1), ("1", "0"))
    evidence["L3改密DB差异"] = {"hash前": h0[:16] + "...", "hash后": h1[:16] + "...",
                                "must_change_pwd": [mcp0, mcp1]}

    time.sleep(12)  # 避开同账号 60s/5 次限流窗口
    s3, c3, t3, mcp3, _ = login(PWD_USER, PWD_NEW)
    check("L3 新密码可登录 code0", c3, 0)
    check("L3 新密码登录后 mustChangePwd=false", mcp3, False)
    s4, c4, t4, _, _ = login(PWD_USER, PWD_OLD)
    check("L3 旧密码已失效（登录不成功）", c4 == 0, False, "code=%s" % c4)
    pc = int(q1("SELECT COUNT(*) FROM audit_logs WHERE action='PASSWORD_CHANGE'") or 0)
    check("L3 PASSWORD_CHANGE 成功审计行>=1", pc >= 1, True, "实际=%d" % pc)
    evidence["L3成功审计行"] = pc
finally:
    # 无条件还原：共享库零残留（含脚本中途异常）
    print("\n=== L4 环境还原（root 直写原哈希 + must_change_pwd=1）===")
    _, rc, err = sql("UPDATE persons SET password_hash='%s', must_change_pwd='%s' WHERE username='%s';"
                     % (h0, mcp0, PWD_USER), ROOTCNF)
    check("L4 还原 UPDATE 成功", rc, 0, err[:120])
    h2 = q1("SELECT password_hash FROM persons WHERE username='%s'" % PWD_USER)
    check("L4 password_hash 已还原为原值", h2 == h0, True)
    time.sleep(12)
    s5, c5, t5, mcp5, _ = login(PWD_USER, PWD_OLD)
    check("L4 还原后原密码复登成功（环境无痕）", c5, 0)
    check("L4 还原后 mustChangePwd 恢复 true", mcp5, True)

# ---------------- L5 删除申请业务链 ----------------
print("\n=== L5 删除申请链：无直删入口 → 提交 → 越权矩阵 → 跳级拒绝 → 初审 → 终审软删 → 归档 ===")
s, b = req("POST", "/api/v1/cert-templates", token=TOK["ADMIN"],
           body={"countryCode": "Q9", "countryName": "QA-P091临时国",
                 "certName": "QA-P091-throwaway-" + TS, "certAuthority": "QA",
                 "requirementDesc": "P0-9.1 业务链验收靶子，可安全软删", "isMandatory": "0"})
ct_id = ((b.get("data") or {}).get("id")) if ok(b) else None
check("L5 超管建 throwaway cert_template（靶子）", (s, ok(b), bool(ct_id)), (200, True, True), "msg=%s" % b.get("msg"))
evidence["靶子cert_template_id"] = ct_id

if ct_id:
    ct_flag0 = q1("SELECT del_flag FROM cert_templates WHERE id=%s" % ct_id)
    # AC-DEL-01：直删入口已关闭
    s, b = req("POST", "/api/v1/cert-templates/%s/remove" % ct_id, token=TOK["ADMIN"])
    check("L5 AC-DEL-01 超管直删被拒（无直删入口，须走审核）", ok(b), False, "code=%s msg=%s" % (b.get("code"), b.get("msg")))
    evidence["L5直删拒绝msg"] = b.get("msg")
    check("L5 直删被拒后 del_flag 仍=0（业务零写）", q1("SELECT del_flag FROM cert_templates WHERE id=%s" % ct_id), ct_flag0)

    # 提交（RD_PM 有 SUBMIT 权限）
    s, b = req("POST", "/api/v1/deletion-requests", token=TOK["RD"],
               body={"entityType": "cert_templates", "entityId": int(ct_id),
                     "reason": "P0-9.1 验收：认证模板下线", "snapshot": "{\"certName\":\"QA-P091\"}"})
    dr_id = ((b.get("data") or {}).get("id")) if ok(b) else None
    check("L5 RD 提交删除申请成功", (s, ok(b), bool(dr_id)), (200, True, True), "msg=%s" % b.get("msg"))
    st = q1("SELECT status FROM deletion_requests WHERE id=%s" % dr_id) if dr_id else "?"
    check("L5 提交后状态=LEADER_REVIEW（进组长初审）", st, "LEADER_REVIEW")
    check("L5 DB 差异：deletion_requests 行 +1",
          int(q1("SELECT COUNT(*) FROM deletion_requests") or 0), dr0 + 1)
    evidence["删除申请id"] = dr_id

    # 越权矩阵
    print("  -- 越权矩阵（deletion-requests）--")
    s, b = req("POST", "/api/v1/deletion-requests", body={"entityType": "cert_templates",
                                                          "entityId": int(ct_id), "reason": "x", "snapshot": "{}"})
    check("L5 NOAUTH 提交 HTTP401", s, 401)
    check("L5 NOAUTH 提交 code=20001", b.get("code"), 20001)
    for role, path, exp in (("MARKET", "/api/v1/deletion-requests/%s/leader-decision?approve=true" % dr_id, 403),
                            ("RD", "/api/v1/deletion-requests/%s/admin-decision?approve=true" % dr_id, 403),
                            ("RD", "/api/v1/deletion-requests/archive", 403)):
        m = "GET" if path.endswith("archive") else "POST"
        s, b = req(m, path, token=TOK[role])
        check("L5 %s 越权 %s HTTP%d" % (role, path.split("/")[-1].split("?")[0], exp), s, exp)
        check("L5 %s 越权 code=30001" % role, b.get("code"), 30001)
    check("L5 越权后状态未被推进", q1("SELECT status FROM deletion_requests WHERE id=%s" % dr_id), "LEADER_REVIEW")
    check("L5 越权后靶子 del_flag 仍=0", q1("SELECT del_flag FROM cert_templates WHERE id=%s" % ct_id), "0")

    # 跳级：未初审直接终审 → 状态机拒绝
    s, b = req("POST", "/api/v1/deletion-requests/%s/admin-decision?approve=true" % dr_id, token=TOK["ADMIN"])
    check("L5 跳级终审被拒（状态机不匹配）", ok(b), False, "code=%s msg=%s" % (b.get("code"), b.get("msg")))
    check("L5 跳级被拒后状态仍=LEADER_REVIEW", q1("SELECT status FROM deletion_requests WHERE id=%s" % dr_id), "LEADER_REVIEW")
    check("L5 跳级被拒后靶子未软删", q1("SELECT del_flag FROM cert_templates WHERE id=%s" % ct_id), "0")
    evidence["L5跳级拒绝msg"] = b.get("msg")

    # 组长初审通过
    # 查询串必须 URL 编码：中文 opinion 直拼会让 urllib 抛 UnicodeEncodeError（首跑因此假红 HTTP=-1）
    s, b = req("POST", "/api/v1/deletion-requests/%s/leader-decision?%s"
               % (dr_id, urllib.parse.urlencode({"approve": "true", "opinion": "P091初审同意"})),
               token=TOK["LEADER"])
    check("L5 组长初审通过 code0", (s, ok(b)), (200, True), "msg=%s" % b.get("msg"))
    row = q1("SELECT CONCAT(status,'|',IFNULL(leader_decision,'-')) FROM deletion_requests WHERE id=%s" % dr_id)
    check("L5 初审后状态=ADMIN_REVIEW|APPROVE", row, "ADMIN_REVIEW|APPROVE")
    check("L5 初审后靶子仍未软删（须终审）", q1("SELECT del_flag FROM cert_templates WHERE id=%s" % ct_id), "0")

    # 超管终审通过 → 原子软删（AC-DEL-02）
    s, b = req("POST", "/api/v1/deletion-requests/%s/admin-decision?%s"
               % (dr_id, urllib.parse.urlencode({"approve": "true", "opinion": "P091终审同意"})),
               token=TOK["ADMIN"])
    check("L5 AC-DEL-02 超管终审通过 code0", (s, ok(b)), (200, True), "msg=%s" % b.get("msg"))
    row = q1("SELECT CONCAT(status,'|',IFNULL(admin_decision,'-')) FROM deletion_requests WHERE id=%s" % dr_id)
    check("L5 终审后状态=DELETED|APPROVE", row, "DELETED|APPROVE")
    check("L5 AC-DEL-02 靶子移入归档区 del_flag=1", q1("SELECT del_flag FROM cert_templates WHERE id=%s" % ct_id), "1")
    ex = q1("SELECT executed_at IS NOT NULL FROM deletion_requests WHERE id=%s" % dr_id)
    check("L5 executed_at 已置位", ex, "1")

    s, b = req("GET", "/api/v1/deletion-requests/archive", token=TOK["ADMIN"])
    ids = [str(x.get("id")) for x in (b.get("data") or [])]
    check("L5 归档区列表可见该申请（AC-DEL-02 归档区）", str(dr_id) in ids, True, "archive=%s" % ids[:8])

    # AC-DEL-02：审计链完整保留 + 动作齐全
    # 审计行按 reason='deletion_request:<id>' 绑定申请（entity_id 是靶子实体，非申请 ID）
    acts = q1("SELECT GROUP_CONCAT(DISTINCT action) FROM audit_logs "
              "WHERE reason='deletion_request:%s' AND action LIKE 'DELETE%%'" % dr_id)
    for a in ("DELETE_REQUEST_SUBMIT", "DELETE_LEADER_APPROVE", "DELETE_EXECUTE"):
        check("L5 审计动作覆盖 %s" % a, a in (acts or ""), True, "实际=%s" % acts)
    chain_checks("L5 AC-DEL-02 删除链后", TOK["ADMIN"])

# ---------------- L6 失败回滚腿 ----------------
print("\n=== L6 失败回滚腿：不支持的 entity_type → 执行失败不得显示 DELETED ===")
s, b = req("POST", "/api/v1/deletion-requests", token=TOK["RD"],
           body={"entityType": "not_a_real_table", "entityId": 999999999,
                 "reason": "P0-9.1 失败回滚探针", "snapshot": "{}"})
bad_id = ((b.get("data") or {}).get("id")) if ok(b) else None
check("L6 提交 unsupported 申请（受理，执行期才校验）", bool(bad_id), True, "msg=%s" % b.get("msg"))
if bad_id:
    req("POST", "/api/v1/deletion-requests/%s/leader-decision?approve=true&opinion=x" % bad_id, token=TOK["LEADER"])
    s, b = req("POST", "/api/v1/deletion-requests/%s/admin-decision?approve=true&opinion=x" % bad_id, token=TOK["ADMIN"])
    check("L6 终审执行失败被拒（不支持的 entity_type）", ok(b), False, "code=%s msg=%s" % (b.get("code"), b.get("msg")))
    evidence["L6失败msg"] = b.get("msg")
    st6 = q1("SELECT status FROM deletion_requests WHERE id=%s" % bad_id)
    check("L6 关键契约：失败后状态≠DELETED（仍 ADMIN_REVIEW）", st6, "ADMIN_REVIEW")
    ex6 = q1("SELECT executed_at IS NULL FROM deletion_requests WHERE id=%s" % bad_id)
    check("L6 executed_at 未置位", ex6, "1")
    de = int(q1("SELECT COUNT(*) FROM audit_logs WHERE action='DELETE_EXECUTE' AND entity_id=%s" % bad_id) or 0)
    check("L6 无 DELETE_EXECUTE 审计（失败不落成功痕）", de, 0)
    chain_checks("L6 失败回滚后", TOK["ADMIN"])

# ---------------- L7 审计验链终态 ----------------
print("\n=== L7 审计验链终态（AC-AUD-03 防篡改 + DEF-4 修复在全业务链下存续）===")
vs, chain, nb, seqs = verify_state(TOK["ADMIN"])
check("L7 终态 verify HTTP200", vs, 200)
check("L7 终态 chain=OK", chain, "OK", "broken=%s" % seqs[:8])
check("L7 终态断裂数=0", nb, 0, "broken=%s" % seqs[:8])
tot7, wj7 = attribute_broken(seqs)
if tot7:
    check("L7 断裂 100% 归因 DEF-6（JSON 载荷行，非 DEF-4 回归）", wj7, tot7,
          "断裂%d行/带载荷%d行" % (tot7, wj7))
    det = q1("SELECT GROUP_CONCAT(CONCAT(seq,':',action)) FROM audit_logs WHERE seq IN (%s)"
             % ",".join(str(int(x)) for x in seqs))
    evidence["DEF-6断裂行明细"] = det
    print("     DEF-6 断裂行明细: %s" % det)
rows1 = int(q1("SELECT COUNT(*) FROM audit_logs") or 0)
seq1 = int(q1("SELECT MAX(seq) FROM audit_logs") or 0)
check("L7 全链新增审计行>=8（本轮业务动作均留痕）", rows1 - rows0 >= 8, True, "增量=%d" % (rows1 - rows0))
gap = q1("SELECT COUNT(*) FROM (SELECT seq, LAG(seq) OVER (ORDER BY seq) p FROM audit_logs) t WHERE p IS NOT NULL AND seq<>p+1")
dup = q1("SELECT COUNT(*) FROM (SELECT seq FROM audit_logs GROUP BY seq HAVING COUNT(*)>1) t")
check("L7 seq 零跳号", gap, "0")
check("L7 seq 零重复", dup, "0")
s, b = req("GET", "/api/v1/audit-logs/export/scope", token=TOK["RD"])
check("L7 RD 受限导出可用（P0-5.4 scoped）", s, 200)

# ---------------- 汇总 ----------------
p = sum(1 for r in results if r["结论"] == "PASS")
f = len(results) - p
evidence["终态"] = {"rows": rows1, "maxSeq": seq1, "chain": chain, "broken": nb,
                    "rows增量": rows1 - rows0, "seq增量": seq1 - seq0}
summary = {"卡": "P0-9.1", "HEAD": HEAD, "jar": os.path.basename(JAR) + "@" + JAR_MTIME,
           "实例": BASE, "TS": time.strftime("%F %T"),
           "总项": len(results), "PASS": p, "FAIL": f,
           "结论": "ALL PASS" if f == 0 else "HAS FAIL",
           "证据": evidence, "明细": results}
out = "/tmp/p091_result.json"
with open(out, "w") as fp:
    json.dump(summary, fp, ensure_ascii=False, indent=2)
print("\n" + "=" * 78)
print("==== P0-9.1: %d/%d PASS  结论=%s ====" % (p, len(results), summary["结论"]))
if f:
    print("---- FAIL 清单 ----")
    for r in results:
        if r["结论"] == "FAIL":
            print("  * %s 期望=%s 实际=%s %s" % (r["项"], r["期望"], r["实际"], r["备注"]))
print("结果已写 %s" % out)
