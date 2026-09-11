#!/usr/bin/env python3
# QA-03 补充矩阵 v7（SEC-02 收口专用）：管理端点（audit-logs/system-configs）× 5 身份
# 背景：缺陷 A-audit（Catalog 缺 ipd:audit-log:* → 全员不可用）修复后真 HTTP 验证；
#       缺陷 B（advice 白名单外 500）已由 6628ab3b 修复，本矩阵做 NOAUTH=401 回归锁。
# 目标实例：16045（ruoyi-admin-sec02a.jar，含 A-audit Catalog 修复 + 缺陷B advice）
#
# R8-AUTO-10 / 后台审查 P0-4：测试账号密码改 env var 注入，避免仓库泄露
# 必须设置环境变量：IPD_TEST_ADMIN_PWD / IPD_TEST_LEADER_PWD / IPD_TEST_MARKET_PWD / IPD_TEST_RD_PWD
# 缺失任一即 fail-fast
import json, os, subprocess, sys, time, urllib.request, urllib.error

def _require_env(name):
    val = os.environ.get(name)
    if not val:
        sys.exit(f"FAIL-FAST: 环境变量 {name} 未设置（参见文件头部 R8-AUTO-10 注释）")
    return val

BASE = "http://localhost:16045"
MYSQL = "/Users/mac/Documents/ruoyi-ai/.codex/ipd-dev/software/mysql-8.0.46-macos15-arm64/bin/mysql"
CNF = "/Users/mac/Documents/ruoyi-ai/.codex/ipd-dev/config/mysql-app.cnf"
HEAD = subprocess.run(["git","-C","/Users/mac/Documents/ruoyi-ai","rev-parse","--short","HEAD"],
                      capture_output=True,text=True).stdout.strip()
TS = str(int(time.time()))

def sql(q):
    r = subprocess.run([MYSQL,"--defaults-file="+CNF,"ipd_dev","-N","-e",q],
                       capture_output=True,text=True)
    return r.stdout.strip()

def req(method,path,token=None,body=None):
    r = urllib.request.Request(BASE+path,method=method)
    if token: r.add_header("Authorization","Bearer "+token)
    data = json.dumps(body).encode() if body is not None else None
    if data: r.add_header("Content-Type","application/json")
    try:
        with urllib.request.urlopen(r,data=data,timeout=15) as resp:
            return resp.status, json.loads(resp.read().decode())
    except urllib.error.HTTPError as e:
        try: return e.code, json.loads(e.read().decode())
        except Exception: return e.code, {}

def login(u,p):
    s,b = req("POST","/api/v1/auth/login",body={"username":u,"password":p})
    return b["data"]["token"] if s==200 and b.get("code")==0 else None

ROLES = {
    "ADMIN":  login("ipd-admin",_require_env("IPD_TEST_ADMIN_PWD")),
    "LEADER": login("ipd-leader",_require_env("IPD_TEST_LEADER_PWD")),
    "MARKET": login("陈市场",_require_env("IPD_TEST_MARKET_PWD")),
    "RD":     login("ipd-rd",_require_env("IPD_TEST_RD_PWD")),
    "NOAUTH": None,
}
assert all(ROLES[r] for r in ("ADMIN","LEADER","MARKET","RD")), "登录失败"
print("登录 4 角色 OK, HEAD=", HEAD, "TS=", TS)

export_before = int(sql("SELECT COUNT(*) FROM audit_logs WHERE action='EXPORT'") or 0)
total_before = int(sql("SELECT COUNT(*) FROM audit_logs") or 0)

results = []
ok = bad = 0

def matrix(name, method, path, expects, extra=None):
    """expects: {role: "200"|"403"|"401"|"not403"|"not500"|"rec"}；extra(role,status,body)->bool|None 附加断言"""
    global ok, bad
    row = {"name":name,"cells":{}}
    for role,exp in expects.items():
        status,bj = req(method,path,ROLES[role], extra_body.get(name) if method=="POST" else None)
        passed = False
        if exp=="rec": passed = status != 500          # 记录型：500 即缺陷
        elif exp=="not403": passed = status != 403
        elif exp=="not500": passed = status != 500
        else: passed = str(status)==exp
        if extra and passed:
            r2 = extra(role,status,bj)
            if r2 is not None: passed = r2
        row["cells"][role] = {"http":status,"code":bj.get("code"),"expect":exp,"pass":passed}
        ok += passed; bad += (not passed)
    allok = all(c["pass"] for c in row["cells"].values())
    print("PASS" if allok else "FAIL", name)
    results.append(row)
    return row

extra_body = {"M9 coefficient NOAUTH/非法body（DEF-2 探针）": {}}

ADMIN_ONLY = {"ADMIN":"200","LEADER":"403","MARKET":"403","RD":"403","NOAUTH":"401"}
ALL_INTERNAL = {"ADMIN":"200","LEADER":"200","MARKET":"200","RD":"200","NOAUTH":"401"}

# ===== 审计端点（缺陷 A-audit 主验证）=====
matrix("M1 audit-logs list 超管专属","GET","/api/v1/audit-logs?pageNo=1&pageSize=5",dict(ADMIN_ONLY))
chain = {}
def _verify_extra(role,status,bj):
    if role=="ADMIN":
        chain.update(bj.get("data") or {})
    return None
matrix("M2 audit-logs verify 链校验","GET","/api/v1/audit-logs/verify",dict(ADMIN_ONLY),extra=_verify_extra)
matrix("M3 audit-logs export 超管专属","GET","/api/v1/audit-logs/export",dict(ADMIN_ONLY))

def _scope_extra(role,status,bj):
    if status!=200: return None
    d = bj.get("data") or {}
    sc = d.get("scope")
    if role=="ADMIN": return sc=="GLOBAL"
    if role=="LEADER": return sc in ("GROUP","OWN")   # 组内仅自己时按实现折为 OWN
    return sc=="OWN"                                   # MARKET/RD 仅本人
matrix("M4 audit-logs scope 角色范围(AC-AUD-04/05/06)","GET","/api/v1/audit-logs/scope?pageNo=1&pageSize=5",dict(ALL_INTERNAL),extra=_scope_extra)
matrix("M5 audit-logs export/scope 全内部角色","GET","/api/v1/audit-logs/export/scope",dict(ALL_INTERNAL))

# ===== 系统参数端点 =====
matrix("M6 system-configs list 超管专属","GET","/api/v1/system-configs",dict(ADMIN_ONLY))

# 动态取一个已存在参数 key
s,b = req("GET","/api/v1/system-configs",ROLES["ADMIN"])
cfg_key, cfg_val = None, None
data = (b.get("data") if isinstance(b,dict) else None) or {}
if isinstance(data,list):
    items = data
else:
    items = data.get("records") or data.get("list") or data.get("items") or []
    if not items:
        for k,v in data.items():
            if isinstance(v,dict) and ("configKey" in v or "key" in v): items=[v]; break
for it in (items or []):
    if isinstance(it,dict):
        cfg_key = it.get("configKey") or it.get("key") or it.get("config_key")
        cfg_val = it.get("configValue", it.get("value", it.get("config_value","")))
        if cfg_key: break
assert cfg_key, f"无法从 list 响应解析参数 key: {str(b)[:200]}"
print("取样参数:", cfg_key, "=", str(cfg_val)[:40])

matrix(f"M7 system-configs read({cfg_key}) 全内部角色","GET",f"/api/v1/system-configs/{cfg_key}",dict(ALL_INTERNAL))
extra_body[f"M8 system-configs update({cfg_key}) 超管专属"] = None
def _put(role):
    return req("PUT",f"/api/v1/system-configs/{cfg_key}",ROLES[role],{"value":str(cfg_val)})
row = {"name":f"M8 system-configs update({cfg_key}) 超管专属(原值回写零副作用)","cells":{}}
for role,exp in ADMIN_ONLY.items():
    status,bj = _put(role)
    passed = (str(status)==exp)
    row["cells"][role] = {"http":status,"code":bj.get("code"),"expect":exp,"pass":passed}
    ok += passed; bad += (not passed)
print("PASS" if all(c["pass"] for c in row["cells"].values()) else "FAIL", row["name"])
results.append(row)

# ===== DEF-2 回归探针（coefficient NOAUTH+非法body；缺陷B2 修复后应非 500）=====
row = {"name":"M9 coefficient-change-requests NOAUTH/非法body（DEF-2 探针，记录型）","cells":{}}
for role in ("NOAUTH","ADMIN","MARKET"):
    status,bj = req("POST","/api/v1/coefficient-change-requests",ROLES[role],{})
    passed = status != 500
    row["cells"][role] = {"http":status,"code":bj.get("code"),"expect":"not500(记录)","pass":passed}
    ok += passed; bad += (not passed)
print("PASS" if all(c["pass"] for c in row["cells"].values()) else "FAIL", row["name"])
results.append(row)

# ===== DB 侧核查 =====
export_after = int(sql("SELECT COUNT(*) FROM audit_logs WHERE action='EXPORT'") or 0)
total_after = int(sql("SELECT COUNT(*) FROM audit_logs") or 0)
aud = {
  "EXPORT_增量(M3+M5四角色应≥5)": export_after-export_before,
  "审计总行数增量": total_after-total_before,
  "verify链状态": chain.get("chain"),
  "verify断裂数": len(chain.get("broken") or []),
  "CONFIG更新审计(本轮)": sql(f"SELECT COUNT(*) FROM audit_logs WHERE entity_type LIKE '%CONFIG%' AND create_time >= NOW() - INTERVAL 5 MINUTE") or "0",
}
out = {"head":HEAD,"ts":TS,"base":BASE,"matrix":results,"audit":aud,
       "summary":{"pass":ok,"fail":bad},
       "scope":"SEC-02 收口补充矩阵：管理端点×5身份（业务62格见 v6 重跑）"}
with open("/tmp/qa03_result_v7.json","w") as f: json.dump(out,f,ensure_ascii=False,indent=1)
print("SUMMARY pass=%d fail=%d" % (ok,bad))
print("AUDIT:", json.dumps(aud,ensure_ascii=False))
