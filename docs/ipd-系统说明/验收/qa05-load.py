#!/usr/bin/env python3
# QA-05 性能基准压测脚本：N 并发 × 5 场景 × 3 轮，记录 p50/p90/p95/p99/max/tps/errors
# 前置：16040 实例指向隔离库 ipd_perf（500 persons/2000 projects/138000 stage_actions/500000 audit_logs）
# 判定：列表 P95 < 1.5s；写 P95 < 3s
# 并发数可覆盖：QA05_CONC=20 python3 qa05-load.py（容量拐点复测）
# 场景可选：QA05_ONLY=a_login,e_transit_write python3 qa05-load.py
# 登录明文复用 QA-03 已入仓的 mock 测试密码（哈希复用其主库行）
import json
import os
import random
import threading
import time
import urllib.error
import urllib.request
from concurrent.futures import ThreadPoolExecutor

BASE = "http://localhost:16040"
CONCURRENCY = int(os.environ.get("QA05_CONC", "100"))
REQS_PER_ROUND = int(os.environ.get("QA05_REQS", "500"))
ROUNDS = 3
ONLY = set(filter(None, os.environ.get("QA05_ONLY", "").split(",")))
OUT = os.environ.get("QA05_OUT",
                     "/Users/mac/Documents/ruoyi-ai/docs/ipd-系统说明/验收/QA-05-load-result-20260905.json")

PWD = {
    "MARKET_PM": "Qa03-Mkt-7pLx9wVz",
    "RD_PM": "iQc9xozo-KelvRPdIQvknkxMZIiij39F",
    "GROUP_LEADER": "i7fVdG7aIgwG8l37-4f7tqNVEQygfWHJ",
    "SUPER_ADMIN": "SZviX9kmMo7Tg92kjBax6i9MVsfBV-eh",
}
ABASE = 7150000000000000000
PRJBASE = 7120000000000000000
PROJ_START = int(os.environ.get("QA05_PROJ_START", "2"))  # 写场景起始项目号（避免重跑命中已流转动作）
# 每项目 C 前缀 MARKET_PM/BOTH 动作下标（owner_role 与 MARKET 压测账号匹配）
MKT_ACTION_IDX = [0, 1, 2, 3, 5, 6, 7, 8, 10, 11]

# 用户名池（与 qa05-seed.py 分布一致）
USERS = ([f"perf_mkt{i:03d}" for i in range(1, 201)] +
         [f"perf_rd{i:03d}" for i in range(1, 201)] +
         [f"perf_gl{i:03d}" for i in range(1, 81)] +
         [f"perf_sa{i:03d}" for i in range(1, 21)])
ROLE_OF = (["MARKET_PM"] * 200 + ["RD_PM"] * 200 + ["GROUP_LEADER"] * 80 + ["SUPER_ADMIN"] * 20)

action_cursor = 0          # 写压测动作池游标（顺序分配防重复）
cursor_lock = threading.Lock()
print_lock = threading.Lock()


def log(msg: str) -> None:
    with print_lock:
        print(msg, flush=True)


def req(method: str, path: str, token: str = None, body: dict = None, timeout: int = 30):
    r = urllib.request.Request(BASE + path, method=method)
    if token:
        r.add_header("Authorization", "Bearer " + token)
    data = json.dumps(body).encode() if body is not None else None
    if data:
        r.add_header("Content-Type", "application/json")
    try:
        with urllib.request.urlopen(r, data=data, timeout=timeout) as resp:
            return resp.status, json.loads(resp.read().decode())
    except urllib.error.HTTPError as e:
        try:
            return e.code, json.loads(e.read().decode())
        except Exception:
            return e.code, {}
    except Exception as e:
        return -1, {"error": str(e)[:120]}


def login(username: str, password: str) -> str:
    s, b = req("POST", "/api/v1/auth/login", body={"username": username, "password": password})
    return b["data"]["token"] if s == 200 and b.get("code") == 0 else None


def pct(sorted_lat: list, p: float) -> float:
    if not sorted_lat:
        return 0.0
    k = min(len(sorted_lat) - 1, max(0, int(round(p / 100.0 * (len(sorted_lat) + 1))) - 1))
    return sorted_lat[k]


def run_round(name: str, fn) -> dict:
    """单轮：500 请求经 100 线程池，返回分位数统计"""
    lat, errs = [], []
    t0 = time.time()
    with ThreadPoolExecutor(max_workers=CONCURRENCY) as ex:
        results = list(ex.map(lambda i: fn(i), range(REQS_PER_ROUND)))
    for ms, ok, note in results:
        lat.append(ms)
        if not ok:
            errs.append(note)
    lat.sort()
    dur = time.time() - t0
    stat = {
        "round": name, "requests": len(lat),
        "p50": round(pct(lat, 50), 1), "p90": round(pct(lat, 90), 1),
        "p95": round(pct(lat, 95), 1), "p99": round(pct(lat, 99), 1),
        "max": round(lat[-1] if lat else 0, 1),
        "tps": round(len(lat) / dur, 1), "errors": len(errs),
        "duration_s": round(dur, 2),
        "raw_ms": lat,
        "error_samples": errs[:10],
    }
    log(f"  {name}: p50={stat['p50']} p95={stat['p95']} p99={stat['p99']} "
        f"max={stat['max']} tps={stat['tps']} err={stat['errors']}")
    return stat


def main() -> None:
    random.seed(20260905)
    global action_cursor
    scenario = {}

    # ---- 预热 50 token（按角色定向：10 SA + 25 MARKET + 10 GL + 5 RD）
    warmup = []
    warm_plan = (["SUPER_ADMIN"] * 10 + ["MARKET_PM"] * 25 + ["GROUP_LEADER"] * 10 + ["RD_PM"] * 5)
    seen = {"MARKET_PM": 0, "RD_PM": 0, "GROUP_LEADER": 0, "SUPER_ADMIN": 0}
    user_by_role = {r: [u for u, rr in zip(USERS, ROLE_OF) if rr == r] for r in PWD}
    for role in warm_plan:
        u = user_by_role[role][seen[role] % len(user_by_role[role])]
        seen[role] += 1
        tok = login(u, PWD[role])
        if tok:
            warmup.append((u, role, tok))
    log(f"预热 token: {len(warmup)}/50")
    admin_toks = [t for _, r, t in warmup if r == "SUPER_ADMIN"]
    mkt_toks = [t for _, r, t in warmup if r == "MARKET_PM"]
    all_toks = [t for _, _, t in warmup]

    # 场景 a：登录（端到端，随机 500 人）
    def sc_login(i):
        u = USERS[random.randrange(500)]
        role = ROLE_OF[USERS.index(u)]
        t1 = time.time()
        s, b = req("POST", "/api/v1/auth/login", body={"username": u, "password": PWD[role]})
        ms = (time.time() - t1) * 1000
        ok = s == 200 and b.get("code") == 0 and b.get("data", {}).get("token")
        return ms, ok, f"{s}:{b.get('message','')[:40]}"

    # 场景 b：项目全量列表（超管，2000 行）
    def sc_projects(i):
        tok = admin_toks[i % len(admin_toks)]
        t1 = time.time()
        s, b = req("GET", "/api/v1/projects", tok)
        ms = (time.time() - t1) * 1000
        ok = s == 200 and b.get("code") == 0 and isinstance(b.get("data"), list) and len(b["data"]) == 2000
        return ms, ok, f"{s}:{b.get('message','')[:40]}"

    # 场景 c：单项目 69 动作
    def sc_stage_actions(i):
        tok = all_toks[i % len(all_toks)]
        pid = PRJBASE + 1 + (i * 13 % 2000)
        t1 = time.time()
        s, b = req("GET", f"/api/v1/stage-actions?projectId={pid}", tok)
        ms = (time.time() - t1) * 1000
        ok = s == 200 and b.get("code") == 0
        return ms, ok, f"{s}:{b.get('message','')[:40]}"

    # 场景 d：审计分页（超管，pageSize=200，50 万表 orderByDesc(seq)）
    def sc_audit(i):
        tok = admin_toks[i % len(admin_toks)]
        page = 1 + (i % 5)
        t1 = time.time()
        s, b = req("GET", f"/api/v1/audit-logs?pageNo={page}&pageSize=200", tok)
        ms = (time.time() - t1) * 1000
        ok = s == 200 and b.get("code") == 0
        return ms, ok, f"{s}:{b.get('message','')[:40]}"

    # 场景 e：写路径 transit（MARKET 写 C 前缀 MARKET_PM 动作 NOT_STARTED→IN_PROGRESS）
    def sc_transit(i):
        global action_cursor
        with cursor_lock:
            slot = action_cursor
            action_cursor += 1
        proj_no = PROJ_START + slot // len(MKT_ACTION_IDX)   # 每项目 10 个可用 MARKET/BOTH C 动作
        aid = ABASE + (proj_no - 1) * 69 + MKT_ACTION_IDX[slot % len(MKT_ACTION_IDX)]
        tok = mkt_toks[i % len(mkt_toks)]
        t1 = time.time()
        s, b = req("POST", f"/api/v1/stage-actions/{aid}/transit?target=IN_PROGRESS", tok)
        ms = (time.time() - t1) * 1000
        ok = s == 200 and b.get("code") == 0
        if not ok:  # 失败回读：确认动作当前状态（403/超时响应 data 可能为 None）
            s2, b2 = req("GET", f"/api/v1/stage-actions?projectId={PRJBASE + proj_no}", tok)
            rows = b2.get("data") if isinstance(b2.get("data"), list) else []
            st = next((x.get("status") for x in rows if str(x.get("id")) == str(aid)), "?")
            return ms, ok, f"transit={s}:{b.get('message','')[:40]} readback={s2}:status={st}"
        return ms, ok, f"{s}:{b.get('message','')[:40]}"

    plan = [("a_login", sc_login), ("b_projects_full_2000", sc_projects),
            ("c_stage_actions_69", sc_stage_actions), ("d_audit_logs_p200", sc_audit),
            ("e_transit_write", sc_transit)]
    for name, fn in plan:
        if ONLY and name not in ONLY:
            continue
        log(f"场景 {name}")
        rounds = []
        for r in range(ROUNDS):
            rounds.append(run_round(f"{name}_r{r + 1}", fn))
            time.sleep(2)
        p95s = [x["p95"] for x in rounds]
        agg = {k: round(sum(x[k] for x in rounds) / len(rounds), 1)
               for k in ("p50", "p90", "p95", "p99", "max", "tps")}
        agg["errors_total"] = sum(x["errors"] for x in rounds)
        agg["p95_max_round"] = max(p95s)
        scenario[name] = {"aggregate": agg, "rounds": rounds}
        log(f"  ==> {name} 3轮均值: {agg}")

    result = {
        "meta": {
            "date": "2026-09-05", "base": BASE, "concurrency": CONCURRENCY,
            "requests_per_round": REQS_PER_ROUND, "rounds": ROUNDS,
            "db": "ipd_perf", "jvm_xmx": "2g",
            "note": "蜂群并发环境下测得，分位数含邻居负载噪声（同机另有 QA agent 并发任务），此为本机共享开发环境首版基线",
        },
        "scenarios": scenario,
    }
    with open(OUT, "w") as f:
        json.dump(result, f, ensure_ascii=False, indent=1)
    log(f"归仓: {OUT}")


if __name__ == "__main__":
    main()
