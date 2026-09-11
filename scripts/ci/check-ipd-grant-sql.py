#!/usr/bin/env python3
"""IPD 表级 GRANT 登记件静态门禁（根除建议文档 §四 层 3 项 8 的 CI 形态）。

为什么需要这道门（2026-09-08 实证）：
  本机权限模型是「库级只授 SELECT+INSERT、表级逐表 GRANT 四权」。新建表
  SQL 漏写 GRANT 块时，INSERT 静默走库级权限（测不出缺口），UPDATE 直接
  500（kpi_shared_confirms / gate_arbitrations 已真实发生过）。真库是否
  apply 只能靠 p1-ddl-apply-check.py --strict（本机 DBA 域，CI 连不到
  13306）；但「登记件缺失」可以在 CI 静态抓——每张业务表的 GRANT 语句
  必须能在 docs/script/sql/ 某个 .sql 里找到（注释形态或活语句均可）。

检查语义（只挡登记件缺失，不验真库）：
  对 20 张 GRANT_RULES 表逐一在 docs/script/sql/**/*.sql 文本中匹配
  「同一 SQL 语句内含 GRANT + 表名 + ipd_app」；任一表找不到 → exit 1。

清单同源义务：
  TABLES 与 docs/ipd-系统说明/验收/p1-ddl-apply-check.py 的 GRANT_RULES
  同源。扩表时两处同步改（本脚本只做文本比对，不 import 那份 DBA 工具，
  避免 CI 依赖 pymysql）。

用法：
  python3 scripts/ci/check-ipd-grant-sql.py            # 检查仓库现状
  python3 scripts/ci/check-ipd-grant-sql.py --selftest # 门禁自测（护栏必须真的会拦）
"""

import argparse
import re
import sys
import tempfile
from pathlib import Path

REPO = Path(__file__).resolve().parents[2]
SQL_ROOT = REPO / "docs/script/sql"

# 与 p1-ddl-apply-check.py GRANT_RULES 同源（2026-09-08 版，20 表）。
# 扩表时两处同步；新表未建表/无写路径的可暂不进清单。
TABLES = [
    # 历史双漏（事故实证），首批必须
    "gate_arbitrations", "kpi_shared_confirms",
    # 2026-09-06 后新建表
    "contributions", "contribution_versions",
    "sop_template_instances", "post_launch_reviews",
    "switching_acceptance", "project_score_records",
    "project_score_tasks", "receipt_ledger",
    "gate_review_observers", "correction_logs", "kpi_rule_snapshots",
    "ipd_business_config", "ipd_business_config_versions",
    "negative_feedbacks", "launch_date_change_requests",
    "notification_events", "coefficient_change_requests", "project_cert_items",
]

# 匹配「一条 GRANT 语句内含目标表与 ipd_app」；SQL 里多为注释登记形态
# （-- GRANT ... TO 'ipd_app'@'127.0.0.1';），压平空白后一并覆盖。
_GRANT_TMPL = r"GRANT[^;]*?\b{table}\b[^;]*?ipd_app"


def check_sql_tree(sql_root: Path) -> list:
    """返回找不到 GRANT 登记语句的表清单（空 = 全覆盖）。"""
    texts = []
    for p in sorted(sql_root.rglob("*.sql")):
        flat = re.sub(r"\s+", " ", p.read_text(encoding="utf-8", errors="replace"))
        texts.append(flat)
    missing = []
    for t in TABLES:
        pat = re.compile(_GRANT_TMPL.format(table=re.escape(t)), re.IGNORECASE)
        if not any(pat.search(txt) for txt in texts):
            missing.append(t)
    return missing


def selftest() -> int:
    """门禁自测：只跑正向用例证明不了任何事，必须覆盖阻断分支。"""
    cases = [
        # (用例名, SQL 内容片段, 期望缺失表数)
        ("全20表注释形态全覆盖",
         "-- GRANT SELECT, INSERT, UPDATE, DELETE ON ipd_dev.{t} TO 'ipd_app'@'127.0.0.1';\n",
         0),
        ("活语句形态（非注释）",
         "GRANT SELECT, INSERT, UPDATE, DELETE ON ipd_dev.{t} TO 'ipd_app'@'127.0.0.1';\n",
         0),
        ("反引号+库前缀形态",
         "GRANT SELECT, INSERT, UPDATE, DELETE ON `ipd_dev`.`{t}` TO 'ipd_app'@'127.0.0.1';\n",
         0),
        ("小写 grant 也认",
         "grant select, insert, update, delete on ipd_dev.{t} to 'ipd_app'@'127.0.0.1';\n",
         0),
    ]
    failures = []
    with tempfile.TemporaryDirectory() as td:
        root = Path(td)
        for name, tmpl, expect_missing in cases:
            d = root / name.replace("/", "_")
            d.mkdir()
            (d / "all.sql").write_text(
                "".join(tmpl.format(t=t) for t in TABLES), encoding="utf-8")
            got = check_sql_tree(d)
            if len(got) != expect_missing:
                failures.append(f"{name}: 期望缺 {expect_missing} 表, 实得 {got}")

        # 阻断分支：单表缺失必须被抓到
        d = root / "missing_one"
        d.mkdir()
        (d / "partial.sql").write_text(
            "".join("-- GRANT SELECT, INSERT, UPDATE, DELETE ON ipd_dev.{t} TO 'ipd_app'@'127.0.0.1';\n"
                    .format(t=t) for t in TABLES if t != "receipt_ledger"),
            encoding="utf-8")
        got = check_sql_tree(d)
        if got != ["receipt_ledger"]:
            failures.append(f"阻断分支单表缺失: 期望 ['receipt_ledger'], 实得 {got}")

        # 阻断分支：授权给别的账号不算 ipd_app 白名单登记
        d = root / "wrong_user"
        d.mkdir()
        (d / "other.sql").write_text(
            "".join("GRANT SELECT ON ipd_dev.{t} TO 'someone_else'@'%';\n".format(t=t)
                    for t in TABLES),
            encoding="utf-8")
        got = check_sql_tree(d)
        if len(got) != len(TABLES):
            failures.append(f"阻断分支非ipd_app: 期望全缺 {len(TABLES)}, 实得 {len(got)}")

        # 阻断分支：空目录全缺
        d = root / "empty"
        d.mkdir()
        got = check_sql_tree(d)
        if len(got) != len(TABLES):
            failures.append(f"阻断分支空目录: 期望全缺, 实得 {len(got)}")

    if failures:
        print("SELFTEST FAIL:")
        for f in failures:
            print("  -", f)
        return 1
    print(f"SELFTEST OK: {len(cases) + 3} 用例全绿（含 3 条阻断分支）")
    return 0


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    ap.add_argument("--selftest", action="store_true", help="跑门禁自测后退出")
    args = ap.parse_args()
    if args.selftest:
        return selftest()
    if not SQL_ROOT.is_dir():
        print(f"FAIL: SQL 根目录不存在: {SQL_ROOT}")
        return 1
    missing = check_sql_tree(SQL_ROOT)
    if missing:
        print(f"FAIL: {len(missing)}/{len(TABLES)} 张表在 docs/script/sql/ 找不到 "
              f"GRANT ... <table> ... ipd_app 登记语句:")
        for t in missing:
            print(f"  MISSING {t}")
        print("（新建表必须在建表/补授 SQL 中登记 GRANT 四权块——"
              "库级权限只有 SELECT+INSERT，UPDATE 会 500；"
              "清单与 p1-ddl-apply-check.py 的 GRANT_RULES 同源，扩表两处同步。）")
        return 1
    print(f"OK: {len(TABLES)}/{len(TABLES)} 表 GRANT 登记语句齐全")
    return 0


if __name__ == "__main__":
    sys.exit(main())
