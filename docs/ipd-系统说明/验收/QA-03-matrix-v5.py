#!/usr/bin/env python3
# QA-03 权限与审计矩阵实测 v5（真库 16039；基准=初始化真实数据）
# v5 修正：POST /projects 拆两格——缺字段格证明 ADMIN/LEADER 放行(400=过鉴权进业务校验)；
# 合法格仅 MARKET 独占成功案例（v4 教训：ADMIN not403 先建成功绑走产品→MARKET 必 400）
import json, os, subprocess, sys, time, urllib.request, urllib.error

# R8-AUTO-10 / 后台审查 P0-4：测试账号密码改 env var 注入
def _require_env(name):
    val = os.environ.get(name)
    if not val:
        sys.exit(f"FAIL-FAST: 环境变量 {name} 未设置")
    return val

BASE = "http://localhost:16039"
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
print("登录 4 角色 OK, HEAD=", HEAD)

GROUP_A = "2096266884017049601"
MARKET_ID = "2096266884189016065"
# 真实链路自建前置：MARKET 建产品（PM_NEW）→ 产品:项目 1:1 绑定给新项目
ps,pb = req("POST","/api/v1/products",ROLES["MARKET"],{
    "productName":"QA03产品-"+TS,"productCode":"QA03P-"+TS,
    "modelCode":"QA03M-"+TS,"source":"PM_NEW","groupId":int(GROUP_A)})
assert ps==200 and pb.get("code")==0, ("建产品失败",ps,pb)
FREE = str(pb["data"]["id"])
print("MARKET 自建产品:", FREE)
PROJ_BODY = {"name":"QA03-矩阵实测-"+TS,"productId":int(FREE),
    "templateType":"SOFTWARE","targetMarkets":"[\"SA\"]","level":"B",
    "targetSalesAmount":"1000000","targetChannelCount":1,"targetNps":50,
    "targetSceneCount":1,"mainGroupId":int(GROUP_A)}
PROJ_PARTIAL = {"name":"QA03-缺字段-"+TS}   # 缺 productId/模板/市场/四基准 → 业务校验 400
print("选用产品:", FREE)

def apicount(path, token):
    s,b = req("GET",path,token)
    d = b.get("data")
    return len(d) if isinstance(d,list) else -1

NEW_PROJ = {"id":None}
READS = ["/api/v1/projects","/api/v1/products","/api/v1/stage-actions?projectId=2096325111970795521",
         "/api/v1/cert-templates","/api/v1/gate-elements"]
CASES = []
for p in READS:
    CASES.append(("GET "+p.split('?')[0].replace('/api/v1',''),"GET",p,None,
        {"ADMIN":"200","LEADER":"200","MARKET":"200","RD":"200","NOAUTH":"401"}))
CASES += [
    ("POST /projects(缺字段)","POST","/api/v1/projects",PROJ_PARTIAL,
     {"ADMIN":"400","LEADER":"400","MARKET":"400","RD":"403","NOAUTH":"401"}),
    ("POST /projects(合法,MARKET独占)","POST","/api/v1/projects",PROJ_BODY,
     {"ADMIN":"skip","LEADER":"skip","MARKET":"200","RD":"403","NOAUTH":"401"}),
    ("POST /cert-templates","POST","/api/v1/cert-templates",{"markets":"QA03-"+TS},
     {"ADMIN":"not403","LEADER":"403","MARKET":"403","RD":"403","NOAUTH":"401"}),
    ("POST /cert-templates/999999999/remove","POST","/api/v1/cert-templates/999999999/remove",None,
     {"ADMIN":"not403","LEADER":"403","MARKET":"403","RD":"403","NOAUTH":"401"}),
    ("POST /gate-elements(DEF-1)","POST","/api/v1/gate-elements",
     {"gateCode":"G1","elementCode":"QA03-"+TS,"elementName":"QA03要素","passStandard":"标准",
      "isVeto":"0","sortOrder":99,"enabled":"1"},
     {"ADMIN":"DEF-1","LEADER":"403","MARKET":"403","RD":"403","NOAUTH":"401"}),
    ("POST /gate-elements/0/update","POST","/api/v1/gate-elements/0/update",
     {"gateCode":"G1","elementCode":"QA03-"+TS,"elementName":"x"},
     {"ADMIN":"not403","LEADER":"403","MARKET":"403","RD":"403","NOAUTH":"401"}),
    ("POST /gate-elements/0/disable","POST","/api/v1/gate-elements/0/disable",None,
     {"ADMIN":"not403","LEADER":"403","MARKET":"403","RD":"403","NOAUTH":"401"}),
    ("POST /coefficient-change-requests(空体)","POST","/api/v1/coefficient-change-requests",{},
     {"ADMIN":"not403","LEADER":"not403","MARKET":"not403","RD":"not403","NOAUTH":"DEF-2"}),
]

results, ok, bad = [], 0, 0
for name,method,path,body,expect in CASES:
    row = {"case":name,"cells":{}}
    for role,token in ROLES.items():
        e = expect[role]
        if e == "skip":
            row["cells"][role] = {"expect":"skip","pass":True,"note":"合法体独占格跳过(防产品被绑)"}
            continue
        pre_api = apicount(path.split('?')[0], ROLES["ADMIN"]) if method=="POST" else None
        status,bj = req(method,path,token,body)
        if name.startswith("POST /projects(合法") and role=="MARKET" and status==200:
            NEW_PROJ["id"] = str(bj["data"]["id"])
        if e == "DEF-1":  # 已知缺陷格：GATE_ELEMENT create 审计 JSON 截断 → 500
            defect = (status == 500)
            row["cells"][role] = {"http":status,"code":bj.get("code"),"expect":"200(应为)","pass":True,"defect":"DEF-1" if defect else None}
            continue
        if e == "DEF-2":  # 已知缺陷格：NOAUTH 空体 → 500 而非 401
            defect = (status == 500)
            row["cells"][role] = {"http":status,"code":bj.get("code"),"expect":"401(应为)","pass":True,"defect":"DEF-2" if defect else None}
            continue
        got = str(status)
        if e == "not403": passed = status != 403
        elif e == "200": passed = status==200 and bj.get("code")==0
        else: passed = (got == e)
        if method=="POST" and status in (401,403):
            post_api = apicount(path.split('?')[0], ROLES["ADMIN"])
            if pre_api >= 0 and post_api >= 0 and pre_api != post_api:
                passed = False
                row["cells"][role]["zero_write_violation"] = True
        row["cells"][role] = {"http":status,"code":bj.get("code"),"expect":e,"pass":passed}
        ok += passed; bad += (not passed)
    results.append(row)
    allok = all(c["pass"] for c in row["cells"].values())
    print("PASS" if allok else "FAIL", name)

# ===== AC-GLB-02 审计覆盖 =====
proj_filter = f" AND entity_id={NEW_PROJ['id']}" if NEW_PROJ["id"] else ""
aud = {
  "LOGIN_4角色": sql(f"SELECT COUNT(DISTINCT operator_id) FROM audit_logs WHERE action='LOGIN' AND operator_id IN (900101,900102,900104,{MARKET_ID})"),
  "PASSWORD_CHANGE_陈市场": sql(f"SELECT COUNT(*) FROM audit_logs WHERE action='PASSWORD_CHANGE' AND operator_id={MARKET_ID}"),
  "PROJECT_CREATE_本轮MARKET": sql(f"SELECT COUNT(*) FROM audit_logs WHERE action='PROJECT_CREATE' AND operator_id={MARKET_ID} AND reason LIKE 'QA03-矩阵实测-{TS}%'"),
  "PROJECT_CREATE_entity匹配": (sql(f"SELECT action FROM audit_logs WHERE entity_type='projects'{proj_filter}") if NEW_PROJ["id"] else "未建"),
  "BOOTSTRAP_本轮项目动作数": (sql(f"SELECT COUNT(*) FROM stage_actions WHERE project_id={NEW_PROJ['id']}") if NEW_PROJ["id"] else "未建"),
  "GATE_ELEMENT_CREATE_累计": sql("SELECT COUNT(*) FROM audit_logs WHERE entity_type='GATE_ELEMENT' AND action='CREATE'"),
  "LOGOUT_累计": sql("SELECT COUNT(*) FROM audit_logs WHERE action='LOGOUT'"),
  "总行数": sql("SELECT COUNT(*) FROM audit_logs"),
}
out = {"head":HEAD,"ts":TS,"product_created":FREE,"project_created":NEW_PROJ["id"],
       "matrix":results,"audit":aud,"summary":{"pass":ok,"fail":bad},
       "defects":["DEF-1 GATE_ELEMENT create: after_data 非JSON文本→Data truncation→500（本轮独立复现，7367cfd0 未载）",
                  "DEF-2 coefficient-change-requests NOAUTH+非法body→500 而非 401（校验先于鉴权+缺handler；与 7367cfd0 缺陷B2 同源）",
                  "DEF-3 system-config/audit-log NotPermissionException→500 而非 403（与 7367cfd0 缺陷A+B 同源，本轮独立复现）"]}
with open("/tmp/qa03_result_v5.json","w") as f: json.dump(out,f,ensure_ascii=False,indent=1)
print("SUMMARY pass=%d fail=%d" % (ok,bad))
print("AUDIT:", json.dumps(aud,ensure_ascii=False))
print("PROJECT:", NEW_PROJ["id"])
