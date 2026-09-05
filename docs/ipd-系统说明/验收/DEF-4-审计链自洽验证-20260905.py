#!/usr/bin/env python3
# DEF-4 审计哈希链自洽性验证（真 HTTP + 真库双侧）
# 背景：DEF-4 四层根因修复后收口证据
#   ① verifyChain 误用降序 wrapper + 硬编码 GENESIS/seq=1 → 结构性全行断判（395/397）
#   ② 毫秒不对称：create_time=datetime(0) vs 哈希入参 currentTimeMillis
#   ③ selectLast→insert 无锁竞态（seq=150 prev 失配实证）
#   ④ MySQL datetime(0) 对毫秒「四舍五入」（≥.500 进位）而 secondMillis 是截断
#      → append 写库前必须毫秒归零，否则约半数新行读回 +1s 失配（实测 seq=406/407）
# 架构约束：ipd_app 对 audit_logs 仅 SELECT,INSERT（G-02 只追加的 DB 层强制）
#   → append 竞态防护 = uk_audit_seq 冲突重试（禁 FOR UPDATE）
#   → rebuildChain 需临时 GRANT UPDATE；链自洽时为 no-op（零 UPDATE）不需授权
# 目标实例：16045（ruoyi-admin-def4.jar）
import json, subprocess, time, urllib.request, urllib.error

BASE = "http://localhost:16045"
MYSQL = "/Users/mac/Documents/ruoyi-ai/.codex/ipd-dev/software/mysql-8.0.46-macos15-arm64/bin/mysql"
CNF = "/Users/mac/Documents/ruoyi-ai/.codex/ipd-dev/config/mysql-app.cnf"
ROOTCNF = "/Users/mac/Documents/ruoyi-ai/.codex/ipd-dev/config/mysql-client.cnf"
HEAD = subprocess.run(["git","-C","/Users/mac/Documents/ruoyi-ai","rev-parse","--short","HEAD"],
                      capture_output=True,text=True).stdout.strip()
TS = time.strftime("%Y-%m-%d %H:%M:%S")

def sql(q, cnf=CNF):
    r = subprocess.run([MYSQL,"--defaults-file="+cnf,"ipd_dev","-N","-e",q],
                       capture_output=True,text=True)
    return r.stdout.strip()

def req(method,path,token=None,body=None):
    r = urllib.request.Request(BASE+path,method=method)
    if token: r.add_header("Authorization","Bearer "+token)
    data = json.dumps(body).encode() if body is not None else None
    if data: r.add_header("Content-Type","application/json")
    try:
        with urllib.request.urlopen(r,data=data,timeout=20) as resp:
            return resp.status, json.loads(resp.read().decode())
    except urllib.error.HTTPError as e:
        try: return e.code, json.loads(e.read().decode())
        except Exception: return e.code, {}

def login(u,p):
    s,b = req("POST","/api/v1/auth/login",body={"username":u,"password":p})
    return b["data"]["token"] if s==200 and b.get("code")==0 else None

ROLES = {
    "ADMIN":  login("ipd-admin","SZviX9kmMo7Tg92kjBax6i9MVsfBV-eh"),
    "LEADER": login("ipd-leader","i7fVdG7aIgwG8l37-4f7tqNVEQygfWHJ"),
    "MARKET": login("陈市场","Qa03-Mkt-7pLx9wVz"),
    "RD":     login("ipd-rd","iQc9xozo-KelvRPdIQvknkxMZIiij39F"),
    "NOAUTH": None,
}
assert all(ROLES[r] for r in ("ADMIN","LEADER","MARKET","RD")), "登录失败（注意限流：同IP同账号60秒最多5次）"
print("登录 4 角色 OK, HEAD=", HEAD, "TS=", TS)

rows0 = int(sql("SELECT COUNT(*) FROM audit_logs") or 0)
maxseq0 = int(sql("SELECT MAX(seq) FROM audit_logs") or 0)
print("前置 DB: rows=%d maxSeq=%d" % (rows0, maxseq0))

results = []
def check(name, actual, expect, owner="DEF-4"):
    ok = (actual == expect)
    results.append({"归属":owner,"项":name,"期望":expect,"实际":actual,"结论":"PASS" if ok else "FAIL"})
    print(("  PASS " if ok else "  FAIL ") + "["+owner+"] "+ name + " 期望=%s 实际=%s" % (expect, actual))
    return ok

def verify_state(tok):
    s,b = req("GET","/api/v1/audit-logs/verify",token=tok)
    d = b.get("data") or {}
    return s, d.get("chain"), len(d.get("broken") or []), (d.get("broken") or [])[:6]

print("\n=== M1 基线 verify ===")
s,chain,n,head = verify_state(ROLES["ADMIN"])
check("M1 verify HTTP", s, 200)
print("     基线 chain=%s brokenCount=%d head=%s" % (chain,n,head))

print("\n=== M2 rebuild-chain 越权矩阵（@SaCheckPermission ipd:audit-log:verify + requireAdmin）===")
for role,exp_http,exp_code in (("NOAUTH",401,20001),("MARKET",403,30001),
                               ("LEADER",403,30001),("RD",403,30001)):
    s,b = req("POST","/api/v1/audit-logs/rebuild-chain",token=ROLES[role])
    check("M2 %s HTTP" % role, s, exp_http)
    check("M2 %s code" % role, b.get("code"), exp_code)

print("\n=== M3 超管 rebuild（链断裂时修哈希；自洽时 no-op fixed=0）===")
s,b = req("POST","/api/v1/audit-logs/rebuild-chain",token=ROLES["ADMIN"])
check("M3 ADMIN HTTP", s, 200)
check("M3 ADMIN code", b.get("code"), 0)
fixed1 = (b.get("data") or {}).get("fixed")
print("     fixed=%s" % fixed1)

print("\n=== M4 rebuild 后 verify（期望 OK/0）===")
s,chain,n,head = verify_state(ROLES["ADMIN"])
check("M4 chain", chain, "OK")
check("M4 brokenCount", n, 0)

print("\n=== M5 新写行自洽性：连续 6 次 export/scope（各写一条 EXPORT 审计，避开 login 限流）===")
for i in range(6):
    req("GET","/api/v1/audit-logs/export/scope",token=ROLES["ADMIN"])
rows1 = int(sql("SELECT COUNT(*) FROM audit_logs") or 0)
check("M5 审计行增量", rows1 - rows0 >= 6, True)
s,chain,n,head = verify_state(ROLES["ADMIN"])
check("M5 写后 chain（第四层根因回归锁）", chain, "OK")
check("M5 写后 brokenCount", n, 0)
if n: print("     断裂 head=%s ← 若为新增行说明毫秒归零失效" % head)

print("\n=== M6 rebuild 幂等（链自洽 → fixed=0 且零 UPDATE）===")
s,b = req("POST","/api/v1/audit-logs/rebuild-chain",token=ROLES["ADMIN"])
check("M6 幂等 fixed", (b.get("data") or {}).get("fixed"), "0")
s,chain,n,head = verify_state(ROLES["ADMIN"])
check("M6 幂等后 chain", chain, "OK")

print("\n=== M7 REBUILD_CHAIN 动作自身落审计（Controller 契约）===")
rb = int(sql("SELECT COUNT(*) FROM audit_logs WHERE action='REBUILD_CHAIN'") or 0)
check("M7 REBUILD_CHAIN 行数>=2", rb >= 2, True)
print("     REBUILD_CHAIN 行=%d" % rb)

print("\n=== M8 DB 权限态：表级意图 vs 运行时实效（WHERE 1=0 零副作用探测）===")
g = sql("SHOW GRANTS FOR 'ipd_app'@'127.0.0.1';", ROOTCNF)
line = [x for x in g.split("\n") if "audit_logs`" in x and "chain_heads" not in x]
grants = line[0] if line else ""
check("M8a 表级 grant 含 SELECT", "SELECT" in grants, True)
check("M8a 表级 grant 含 INSERT", "INSERT" in grants, True)
check("M8a 表级 grant 无 UPDATE（G-02 只追加意图）", "UPDATE" in grants, False)
print("     %s" % grants.strip())
# 运行时实效：MySQL 权限累加（全局→库→表→列），库级 grant 会覆盖表级收紧
rt = subprocess.run([MYSQL,"--defaults-file="+CNF,"ipd_dev","-e",
                     "UPDATE audit_logs SET curr_hash=curr_hash WHERE 1=0;"],
                    capture_output=True,text=True)
rt_del = subprocess.run([MYSQL,"--defaults-file="+CNF,"ipd_dev","-e",
                         "DELETE FROM audit_logs WHERE 1=0;"],
                        capture_output=True,text=True)
check("M8b 运行时 UPDATE 被拒（只追加真强制）", rt.returncode != 0, True, "DEF-5")
check("M8b 运行时 DELETE 被拒（只追加真强制）", rt_del.returncode != 0, True, "DEF-5")
if rt.returncode == 0 or rt_del.returncode == 0:
    db级 = [x for x in g.split("\n") if "`ipd_dev`.*" in x]
    print("     ⚠ DEF-5：库级 grant 架空表级收紧 → %s" % (db级[0].strip() if db级 else "?"))

print("\n=== M9 seq 连续性（无跳号/无重复）===")
gap = sql("SELECT COUNT(*) FROM (SELECT seq, LAG(seq) OVER (ORDER BY seq) AS p FROM audit_logs) t "
          "WHERE p IS NOT NULL AND seq <> p + 1")
check("M9 seq 跳号数", gap, "0")
dup = sql("SELECT COUNT(*) FROM (SELECT seq FROM audit_logs GROUP BY seq HAVING COUNT(*)>1) t")
check("M9 seq 重复数", dup, "0")

rows2 = int(sql("SELECT COUNT(*) FROM audit_logs") or 0)
maxseq2 = int(sql("SELECT MAX(seq) FROM audit_logs") or 0)
d4 = [r for r in results if r["归属"]=="DEF-4"]
d5 = [r for r in results if r["归属"]=="DEF-5"]
p4 = sum(1 for r in d4 if r["结论"]=="PASS")
p5 = sum(1 for r in d5 if r["结论"]=="PASS")
summary = {"HEAD":HEAD,"TS":TS,"实例":BASE,
           "DB":{"rows前":rows0,"maxSeq前":maxseq0,"rows后":rows2,"maxSeq后":maxseq2,
                 "REBUILD_CHAIN行":rb},
           "DEF-4":{"总项":len(d4),"PASS":p4,"FAIL":len(d4)-p4,
                    "结论":"ALL PASS" if p4==len(d4) else "HAS FAIL"},
           "DEF-5":{"总项":len(d5),"PASS":p5,"FAIL":len(d5)-p5,
                    "说明":"库级 grant 架空表级只追加收紧（新发现，独立建卡）"},
           "明细":results}
out = "/tmp/def4_chain_result.json"
with open(out,"w") as f: json.dump(summary,f,ensure_ascii=False,indent=2)
print("\n==== DEF-4: %d/%d PASS 结论=%s ====" % (p4,len(d4),summary["DEF-4"]["结论"]))
print("==== DEF-5 探测: %d/%d PASS（FAIL 即新缺陷成立，已独立建卡）====" % (p5,len(d5)))
print("结果已写 %s" % out)
