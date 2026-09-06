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
            "WHERE table_name IN ('stage_actions','launch_date_change_requests')")
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
    return out


def judge(results):
    verdicts = []
    for r in results:
        db = r["db"]
        idx = r.get("idx_sa_project_code")
        verdicts.append({
            "db": db,
            "项5_idx_sa_project_code": "APPLIED" if idx else ("MISSING" if r["stage_actions_exists"] else "N/A(表不存在)"),
            "项1a_uk_ldcr_pending_project": ("APPLIED" if r.get("uk_ldcr_pending_project")
                                        else "NOT_APPLIED" if r.get("ldcr_table_exists") else "N/A(表不存在)"),
            "项1b_version列": ("APPLIED" if r.get("col_version") is True
                          else "NOT_APPLIED" if r.get("ldcr_table_exists") else "N/A(表不存在)"),
            "双签表存在": bool(r.get("ldcr_table_exists")),
        })
    return verdicts


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--dbs", nargs="*", default=DEFAULT_DBS)
    ap.add_argument("--all", action="store_true",
                    help="扫所有含 stage_actions / launch_date_change_requests 的非系统库")
    ap.add_argument("--tcp", action="store_true",
                    help="走 127.0.0.1:3306 TCP（对齐应用真实连接方式）而不是 unix socket")
    ap.add_argument("--out", default=str(REPO / "docs/ipd-系统说明/验收/ddl-apply-check-result-20260905.json"))
    args = ap.parse_args()

    conn = connect(tcp=args.tcp)
    with conn.cursor() as cur:
        dbs, all_schemas = existing_dbs(cur, args.dbs, all_with_table=args.all)
        results = [check_db(cur, db) for db in dbs]
    conn.close()

    payload = {
        "checkedAt": datetime.now().astimezone().isoformat(timespec="seconds"),
        "credentialSource": str(CNF.relative_to(REPO)),
        "requestedDbs": args.dbs,
        "resolvedDbs": dbs,
        "allSchemas": all_schemas,
        "detail": results,
        "verdict": judge(results),
    }
    Path(args.out).write_text(json.dumps(payload, ensure_ascii=False, indent=2))
    print(json.dumps(payload["verdict"], ensure_ascii=False, indent=2))
    if not dbs:
        print("!! 指定的库都不存在，凭证或库名有误", file=sys.stderr)
        return 2
    return 0


if __name__ == "__main__":
    sys.exit(main())
