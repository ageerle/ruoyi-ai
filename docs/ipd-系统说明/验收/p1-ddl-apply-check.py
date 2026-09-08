#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
P1 项5（owner 2026-09-05 指令）：DDL 索引在真实库的 apply 状态核验 —— 全程只读 SELECT。

核验三件事（只看 information_schema，绝不 ALTER）：
  1. idx_sa_project_code（SQL 已 commit 8e861719）是否真在 stage_actions 上生效；
     同时报出它的列序与基数，用来判断 R8-AUTO-8 loadByCode 的 .in() 优化是否真能吃到索引。
  2. launch_date_change_requests 的双签约束 apply 状态：
     · uk_ldcr_pending_project（项1a 部分唯一索引等价物）
     · pending_project_id 生成列
     · version 列（项1b 乐观锁）
     三者任一缺失 → 对应 Java 侧防护在活库里是「代码已就位、约束未生效」的半套状态。
  3. launch_date_change_requests 表本身是否存在（该表只登记在 update/ 迁移里，
     基线 2026-09-04-ipd-p0-tables.sql 的 26 表不含它 → 新环境可能整表缺失）。

2026-09-08 D 批次机制 2 扩项（WB-17-1 真活验证两起事故驱动：
gate_arbitrations 漏 GRANT 致 UPDATE 500、kpi_shared_confirms 同构先例）：
  4. 列约束（COLUMN_RULES）：IS_NULLABLE / 列类型前缀与消费方语义匹配——
     「SQL 已 apply」的判定从「表存在」升级为「约束与消费方语义匹配」
     （例：gate_arbitrations.decision 必须 NULLable，NULL=待裁中间态）。
  5. 表级 GRANT（GRANT_RULES）：ipd_app@127.0.0.1 的逐表 CRUD 白名单必须含四权。
     判定语义按本仓授权模型定义为「表级白名单行必须含四权」——因为库级
     SELECT,INSERT 永远存在（mysql.db），按「实际有效权限」判定则 INSERT 永真、
     测不出白名单缺口（INSERT 静默走库级、UPDATE 直接 500 的事故机制）。
  6. 值域抽查（DOMAIN_RULES）：DISTINCT 实值 vs 契约值域的域外值扫描；
     空表记 EMPTY（证据不足）不计 PASS；表无 CHECK 约束，抽查只证存量合规。

--strict 门禁：任一 VIOLATION / MISSING / PARTIAL / OUT_OF_DOMAIN / NULL_VIOLATION → exit 1
（默认仍返回 0/2，向后兼容人工读 JSON 流程）。

库来源：命令行 --dbs 覆盖；默认取 application-dev.yml 实际连接的库 + ipd 隔离库候选。
凭证：沿用 QA-06 纪律，从 .codex/ipd-dev/config/mysql-client.cnf 解析，不出现在命令行与输出。
"""
import argparse
import json
import re
import sys
from datetime import datetime
from pathlib import Path

import pymysql

REPO = Path(__file__).resolve().parents[3]
CNF = REPO / ".codex/ipd-dev/config/mysql-client.cnf"
DEFAULT_DBS = ["ruoyi-ai", "ipd_dev", "ipd_restore"]

# ===== 扩项①：列约束规则（表, 列, 期望 IS_NULLABLE('YES'/'NO'), 期望类型前缀, 规则出处）=====
COLUMN_RULES = [
    ("gate_arbitrations", "decision", "YES", "varchar(16)",
     "WB-17-1: NULL=待裁中间态（openArbitration 预落行），2026-09-08 ALTER 后必须可 NULL"),
    ("gate_arbitrations", "arbitrator_type", "NO", "varchar(16)", "p254 契约：仲裁人类型必填"),
    ("launch_date_change_requests", "version", "NO", "int", "项1b 乐观锁"),
    ("launch_date_change_requests", "pending_project_id", "YES", "bigint",
     "项1a 生成列（生成列核验见 check_column_rules 的 EXTRA 分支）"),
]

# ===== 扩项②：表级 GRANT 规则（库, 表, 账号, host, 期望权限集）=====
_DML4 = frozenset({"SELECT", "INSERT", "UPDATE", "DELETE"})
GRANT_RULES = [
    ("ipd_dev", t, "ipd_app", "127.0.0.1", _DML4)
    for t in (
        # 历史双漏（事故实证），首批必须
        "gate_arbitrations", "kpi_shared_confirms",
        # 2026-09-06 后新建表（仅 3 张建表 SQL 带 GRANT 注释块）
        "contributions", "contribution_versions",
        "sop_template_instances", "post_launch_reviews",
        "switching_acceptance", "project_score_records",
        "project_score_tasks", "receipt_ledger",
        "gate_review_observers", "correction_logs", "kpi_rule_snapshots",
        "ipd_business_config", "ipd_business_config_versions",
        "negative_feedbacks", "launch_date_change_requests",
        "notification_events", "coefficient_change_requests", "project_cert_items",
    )
]

# ===== 扩项③：值域抽查规则（表, 列, 契约值域, 是否允许NULL）=====
DOMAIN_RULES = [
    ("gate_arbitrations", "decision", {"APPROVE", "REJECT"}, True),
    ("gate_arbitrations", "arbitrator_type", {"GROUP_LEADER", "SUPER_ADMIN"}, False),
    ("contributions", "status", {"DRAFT", "SUBMITTED", "CONFIRMED"}, False),
    ("contributions", "leader_decision", {"APPROVE", "REJECT"}, True),
]

# --strict 门禁判定用的坏状态前缀集合
_BAD_PREFIXES = ("VIOLATION", "MISSING", "PARTIAL", "OUT_OF_DOMAIN", "NULL_VIOLATION")


def parse_cnf(path: Path):
    text = path.read_text()
    user = re.search(r"^user\s*=\s*(\S+)", text, re.M).group(1)
    password = re.search(r"^password\s*=\s*(\S+)", text, re.M).group(1)
    sock = re.search(r"^socket\s*=\s*(\S+)", text, re.M).group(1)
    return user, password, sock


def connect(tcp: bool = False):
    user, password, sock = parse_cnf(CNF)
    kw = ({"host": "127.0.0.1", "port": 3306} if tcp else {"unix_socket": sock})
    return pymysql.connect(
        **kw, user=user, password=password, charset="utf8mb4",
        cursorclass=pymysql.cursors.DictCursor, autocommit=True,
    )


def existing_dbs(cur, wanted, all_with_table=False):
    """默认只跟 wanted；--all 时拓出所有含 stage_actions 的非系统库
    （集成测试会建 ipd_*_test_<hex> 临时库，不扫全会漏掉真正跑过验收的那个库）。"""
    cur.execute("SELECT schema_name AS n FROM information_schema.schemata")
    have = {r["n"] for r in cur.fetchall()}
    if all_with_table:
        cur.execute(
            "SELECT DISTINCT table_schema AS n FROM information_schema.tables "
            "WHERE table_name IN ('stage_actions','launch_date_change_requests',"
            "'gate_arbitrations','kpi_shared_confirms','contributions')")
        wanted = [r["n"] for r in cur.fetchall()]
    return [d for d in wanted if d in have], sorted(have)


def table_exists(cur, db, table):
    cur.execute(
        "SELECT COUNT(*) AS c FROM information_schema.tables "
        "WHERE table_schema=%s AND table_name=%s", (db, table))
    return cur.fetchone()["c"] > 0


def index_info(cur, db, table, index):
    cur.execute(
        "SELECT column_name AS col, seq_in_index AS seq, non_unique AS nu "
        "FROM information_schema.statistics "
        "WHERE table_schema=%s AND table_name=%s AND index_name=%s ORDER BY seq_in_index",
        (db, table, index))
    rows = cur.fetchall()
    return None if not rows else {
        "columns": [r["col"] for r in rows],
        "unique": all(int(r["nu"]) == 0 for r in rows),
    }


def column_exists(cur, db, table, column):
    cur.execute(
        "SELECT COUNT(*) AS c FROM information_schema.columns "
        "WHERE table_schema=%s AND table_name=%s AND column_name=%s", (db, table, column))
    return cur.fetchone()["c"] > 0


def check_column_rules(cur, db):
    """扩项①：列约束 vs 消费方语义（IS_NULLABLE 主判 + 类型前缀次级 TYPE_DRIFT）。"""
    out = []
    for table, column, want_nullable, want_type, why in COLUMN_RULES:
        cur.execute(
            "SELECT is_nullable AS is_nullable, column_type AS column_type, extra AS extra "
            "FROM information_schema.columns "
            "WHERE table_schema=%s AND table_name=%s AND column_name=%s", (db, table, column))
        row = cur.fetchone()
        if row is None:
            out.append({"table": table, "column": column, "state": "N/A(列缺失)", "why": why})
            continue
        state = "OK"
        if row["is_nullable"] != want_nullable:
            state = "VIOLATION(期望IS_NULLABLE=%s实为%s)" % (want_nullable, row["is_nullable"])
        elif not row["column_type"].lower().startswith(want_type.lower()):
            state = "TYPE_DRIFT(期望%s前缀实为%s)" % (want_type, row["column_type"])
        out.append({"table": table, "column": column, "state": state,
                    "actual": {"is_nullable": row["is_nullable"], "type": row["column_type"]},
                    "why": why})
    return out


def check_domain_rules(cur, db):
    """扩项③：域外值扫描（空=PASS）+ 全分布证据；空表记 EMPTY 不计 PASS。"""
    out = []
    for table, column, domain, allow_null in DOMAIN_RULES:
        if not table_exists(cur, db, table):
            out.append({"table": table, "column": column, "state": "N/A(表不存在)"})
            continue
        placeholders = ", ".join(["%s"] * len(domain))
        cur.execute(
            "SELECT `%s` AS v, COUNT(*) AS c FROM `%s`.`%s` "
            "WHERE `%s` IS NOT NULL AND `%s` NOT IN (%s) GROUP BY `%s`"
            % (column, db, table, column, column, placeholders, column),
            tuple(sorted(domain)))
        bad = cur.fetchall()
        cur.execute("SELECT `%s` AS v, COUNT(*) AS c FROM `%s`.`%s` GROUP BY `%s` ORDER BY c DESC"
                    % (column, db, table, column))
        dist = cur.fetchall()
        null_count = next((r["c"] for r in dist if r["v"] is None), 0)
        total = sum(r["c"] for r in dist)
        if total == 0:
            state = "EMPTY(证据不足不计PASS)"
        elif bad:
            state = "OUT_OF_DOMAIN(%s)" % ", ".join("%s×%d" % (r["v"], r["c"]) for r in bad)
        elif null_count and not allow_null:
            state = "NULL_VIOLATION(null×%d，契约不允许NULL)" % null_count
        else:
            state = "IN_DOMAIN"
        out.append({"table": table, "column": column, "state": state, "distribution": dist})
    return out


def check_grant_rules(cur, resolved_dbs):
    """扩项②：表级 GRANT 白名单核验（查 mysql.tables_priv，只读，不随库循环）。
    判定语义 = 表级白名单行必须含四权（库级 SELECT,INSERT 掩盖效应见模块 docstring）。"""
    out = []
    for db, table, user, host, want in GRANT_RULES:
        if db not in resolved_dbs:
            out.append({"db": db, "table": table, "state": "N/A(库缺失)"})
            continue
        cur.execute(
            "SELECT table_priv AS table_priv FROM mysql.tables_priv "
            "WHERE user=%s AND host=%s AND db=%s AND table_name=%s", (user, host, db, table))
        row = cur.fetchone()
        if row is None or not row["table_priv"]:
            state = "MISSING(无表级白名单行)"
            have = set()
        else:
            have = {p.strip().upper() for p in row["table_priv"].split(",") if p.strip()}
            missing = want - have
            state = "FULL" if not missing else "PARTIAL(缺%s)" % ",".join(sorted(missing))
        out.append({"db": db, "table": table, "user": "%s@%s" % (user, host), "state": state,
                    "granted": sorted(have)})
    return out


def check_db(cur, db):
    out = {"db": db}
    out["stage_actions_exists"] = table_exists(cur, db, "stage_actions")
    if out["stage_actions_exists"]:
        out["idx_sa_project_code"] = index_info(cur, db, "stage_actions", "idx_sa_project_code")
        # 顺带报出该表现有索引，避免「换了个名字其实已存在」的误判
        cur.execute(
            "SELECT DISTINCT index_name AS n FROM information_schema.statistics "
            "WHERE table_schema=%s AND table_name='stage_actions' ORDER BY n", (db,))
        out["stage_actions_indexes"] = [r["n"] for r in cur.fetchall()]

    tbl = "launch_date_change_requests"
    out["ldcr_table_exists"] = table_exists(cur, db, tbl)
    if out["ldcr_table_exists"]:
        out["uk_ldcr_pending_project"] = index_info(cur, db, tbl, "uk_ldcr_pending_project")
        out["col_pending_project_id"] = column_exists(cur, db, tbl, "pending_project_id")
        out["col_version"] = column_exists(cur, db, tbl, "version")
        cur.execute(
            "SELECT COUNT(*) AS c FROM `%s`.launch_date_change_requests "
            "WHERE status='PENDING_SECOND' AND IFNULL(del_flag,'0')='0' "
            "GROUP BY project_id HAVING COUNT(*)>1" % db)
        out["duplicate_pending_rows"] = [r["c"] for r in cur.fetchall()]
    # D 批次机制 2 扩项：列约束 + 值域抽查（随库）
    out["column_rules"] = check_column_rules(cur, db)
    out["domain_rules"] = check_domain_rules(cur, db)
    return out


def judge(results):
    verdicts = []
    for r in results:
        db = r["db"]
        idx = r.get("idx_sa_project_code")
        col_states = [c["state"] for c in r.get("column_rules", [])]
        dom_states = [d["state"] for d in r.get("domain_rules", [])]
        verdicts.append({
            "db": db,
            "项5_idx_sa_project_code": "APPLIED" if idx else ("MISSING" if r["stage_actions_exists"] else "N/A(表不存在)"),
            "项1a_uk_ldcr_pending_project": ("APPLIED" if r.get("uk_ldcr_pending_project")
                                        else "NOT_APPLIED" if r.get("ldcr_table_exists") else "N/A(表不存在)"),
            "项1b_version列": ("APPLIED" if r.get("col_version") is True
                          else "NOT_APPLIED" if r.get("ldcr_table_exists") else "N/A(表不存在)"),
            "双签表存在": bool(r.get("ldcr_table_exists")),
            "列约束": _summarize(col_states),
            "值域": _summarize(dom_states),
        })
    return verdicts


def _summarize(states):
    """把逐条 state 压缩成一句 verdict；好状态（OK/IN_DOMAIN/EMPTY/N/A/TYPE_DRIFT）折叠计数。"""
    bad = [s for s in states if s.startswith(_BAD_PREFIXES)]
    good = len(states) - len(bad)
    head = "OK %d/%d" % (good, len(states)) if states else "N/A"
    return head + ("; " + "; ".join(bad) if bad else "")


def grant_verdict(grant_results):
    if not grant_results:
        return "N/A"
    bad = [g for g in grant_results if g["state"].startswith(("MISSING", "PARTIAL"))]
    if not bad:
        return "FULL %d/%d" % (len(grant_results), len(grant_results))
    return "缺口 %d/%d: %s" % (len(bad), len(grant_results),
                            "; ".join("%s=%s" % (g["table"], g["state"]) for g in bad))


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--dbs", nargs="*", default=DEFAULT_DBS)
    ap.add_argument("--all", action="store_true",
                    help="扫所有含 stage_actions / launch_date_change_requests 等登记表的非系统库")
    ap.add_argument("--tcp", action="store_true",
                    help="走 127.0.0.1:3306 TCP（对齐应用真实连接方式）而不是 unix socket")
    ap.add_argument("--strict", action="store_true",
                    help="门禁模式：任一 VIOLATION/MISSING/PARTIAL/OUT_OF_DOMAIN/NULL_VIOLATION → exit 1")
    ap.add_argument("--out", default=str(
        REPO / ("docs/ipd-系统说明/验收/ddl-apply-check-result-%s.json" % datetime.now().strftime("%Y%m%d"))))
    args = ap.parse_args()

    conn = connect(tcp=args.tcp)
    with conn.cursor() as cur:
        dbs, all_schemas = existing_dbs(cur, args.dbs, all_with_table=args.all)
        results = [check_db(cur, db) for db in dbs]
        grants = check_grant_rules(cur, dbs)
    conn.close()

    verdicts = judge(results)
    payload = {
        "checkedAt": datetime.now().astimezone().isoformat(timespec="seconds"),
        "credentialSource": str(CNF.relative_to(REPO)),
        "requestedDbs": args.dbs,
        "resolvedDbs": dbs,
        "allSchemas": all_schemas,
        "detail": results,
        "grantRules": grants,
        "verdict": verdicts,
        "grantVerdict": grant_verdict(grants),
    }
    Path(args.out).write_text(json.dumps(payload, ensure_ascii=False, indent=2))
    print(json.dumps(payload["verdict"], ensure_ascii=False, indent=2))
    print("表级GRANT:", payload["grantVerdict"])
    if not dbs:
        print("!! 指定的库都不存在，凭证或库名有误", file=sys.stderr)
        return 2
    if args.strict:
        bad_states = [s for r in results
                      for s in ([c["state"] for c in r.get("column_rules", [])]
                                + [d["state"] for d in r.get("domain_rules", [])])
                      if s.startswith(_BAD_PREFIXES)]
        bad_grants = [g["state"] for g in grants if g["state"].startswith(("MISSING", "PARTIAL"))]
        if bad_states or bad_grants:
            print("!! --strict 门禁拦截：列约束/值域违规 %d 条，GRANT 缺口 %d 条"
                  % (len(bad_states), len(bad_grants)), file=sys.stderr)
            return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
