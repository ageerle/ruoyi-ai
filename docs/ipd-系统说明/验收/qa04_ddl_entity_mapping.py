#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
QA-04 三方对照脚本：仓库 DDL ↔ domain 实体 ↔ ipd_dev 线上库 schema。

对照维度（卡面验收要点）：
  1. 表 ↔ @TableName 类（29 张表：26 主表 + 3 增量表）
  2. 列 ↔ 实体字段（含 BaseEntity 继承字段，排除 @TableField(exist=false)）
  3. JSON 列 ↔ String 契约（type-mapping.md §1.4）
  4. datetime 列 ↔ java.util.Date
  5. del_flag / tenant_id 存在性（audit_logs 只追加、无 del_flag 属设计内例外）
  6. DDL 默认值 ↔ Java 字段初始化值（info 级）
  7. 唯一键：仓库 DDL ↔ ipd_dev 线上库（漂移检测）
  8. 线上库列集 ↔ 仓库 DDL 列集（漂移检测）

用法（仓库根目录执行）：
  python3 docs/ipd-系统说明/验收/qa04_ddl_entity_mapping.py \
      [--out docs/ipd-系统说明/验收/QA-04-mapping-result.json] \
      [--live-columns /tmp/qa04_ipd_dev_schema.tsv] \
      [--live-indexes /tmp/qa04_ipd_dev_indexes.tsv]

退出码：0=无 ERROR 级问题；1=存在 ERROR 级问题（WARN 不影响退出码）。
不打印任何口令；只读仓库文件与 --live-* TSV（由 root 会话预先导出）。
"""
import argparse
import json
import os
import re
import sys
from collections import OrderedDict

REPO = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", ".."))
DDL_DIR = os.path.join(REPO, "docs", "script", "sql", "update")
DOMAIN_DIR = os.path.join(
    REPO, "ruoyi-modules", "ruoyi-ipd", "src", "main", "java", "org", "ruoyi", "ipd", "domain")

DDL_FILES_MAIN = ["2026-09-04-ipd-p0-tables.sql"]
DDL_FILES_INC = [
    "2026-09-05-ipd-p143-optimistic-lock.sql",
    "2026-09-05-ipd-perf01-nextcode-unique.sql",
    "2026-09-05-ipd-project-cert-items.sql",
    "2026-09-05-ipd-coefficient-change-requests.sql",
    "2026-09-05-ipd-launch-date-change-requests.sql",
]
# legacy-import 只取其 ADD COLUMN（含数据导入语句，整文件不可重放）
DDL_FILES_ALTER_ONLY = ["2026-09-05-ipd-legacy-import.sql"]

JAVA_TYPE_OK = {
    "bigint": {"Long", "long"},
    "int": {"Integer", "int"},
    "integer": {"Integer", "int"},
    "smallint": {"Integer", "int", "Short"},
    "tinyint": {"Integer", "int", "Boolean"},
    "decimal": {"BigDecimal"},
    "numeric": {"BigDecimal"},
    "varchar": {"String"},
    "char": {"String"},
    "text": {"String"},
    "mediumtext": {"String"},
    "longtext": {"String"},
    "json": {"String"},
    "datetime": {"Date", "LocalDateTime"},
    "date": {"Date", "LocalDate"},
    "timestamp": {"Date", "LocalDateTime"},
}
JSON_TYPES = {"json"}
DATE_TYPES = {"datetime", "date", "timestamp"}

# audit_logs 为只追加+hash 链表，无 del_flag/update_* 属设计内例外（外部资源 v3 TS-06）
NO_DEL_FLAG_WHITELIST = {"audit_logs"}


def strip_comments(sql):
    """去掉 -- 行注释与 '...' 字符串字面量（comment 内容含逗号/括号，必须先移除）。"""
    sql = re.sub(r"--[^\n]*", "", sql)
    return re.sub(r"'(?:[^'\\]|\\.)*'", "''", sql)


def split_top_commas(body):
    """按顶层逗号切分（忽略括号内的逗号，如 decimal(10,6) / unique key(a,b)）。"""
    parts, depth, cur = [], 0, []
    for ch in body:
        if ch == "(":
            depth += 1
        elif ch == ")":
            depth -= 1
        if ch == "," and depth == 0:
            parts.append("".join(cur).strip())
            cur = []
        else:
            cur.append(ch)
    if cur:
        parts.append("".join(cur).strip())
    return [p for p in parts if p]


def parse_create_tables(sql, tables):
    for stmt in re.findall(r"create\s+table\s+(?:if\s+not\s+exists\s+)?`?(\w+)`?\s*\((.*?)\)\s*engine\s*=",
                           sql, re.S | re.I):
        tname, body = stmt[0], stmt[1]
        t = tables.setdefault(tname, {"columns": OrderedDict(), "unique_keys": {}, "source": []})
        t["source"].append("create")
        for item in split_top_commas(strip_comments(body)):
            low = item.strip().lower()
            if low.startswith(("primary key", "unique key", "unique index", "key ", "index ", "constraint")):
                m = re.match(r"(?:unique\s+key|unique\s+index)\s+(\w+)\s*\(([^)]*)\)", low, re.I)
                if m:
                    cols = [c.strip().strip("`") for c in m.group(2).split(",")]
                    t["unique_keys"][m.group(1)] = cols
                continue
            m = re.match(r"`?(\w+)`?\s+([a-z]+(?:\s*\([^)]*\))?)\s*(.*)$", item.strip(), re.I)
            if not m:
                continue
            name, coltype, rest = m.group(1), re.sub(r"\s+", "", m.group(2).lower()), m.group(3).lower()
            dm = re.search(r"default\s+('(?:[^'\\]|\\.)*'|[\w.]+)", rest)
            default = None
            if dm:
                default = dm.group(1).strip("'")
            elif re.search(r"not\s+null", rest) is None:
                default = "<null-allowed>"
            t["columns"][name] = {
                "type": coltype,
                "base_type": re.sub(r"\(.*", "", coltype),
                "not_null": bool(re.search(r"not\s+null", rest)),
                "auto_increment": "auto_increment" in rest,
                "default": default,
            }


def parse_alters(sql, tables, fname):
    """解析 ADD COLUMN 列清单与 ADD UNIQUE KEY。"""
    for tname, body in re.findall(r"alter\s+table\s+`?(\w+)`?\s+(.*?);", sql, re.S | re.I):
        t = tables.setdefault(tname, {"columns": OrderedDict(), "unique_keys": {}, "source": []})
        body_nc = strip_comments(body)
        for m in re.finditer(r"add\s+column\s+(?:if\s+not\s+exists\s+)?`?(\w+)`?\s+([a-z]+(?:\s*\([^)]*\))?)\s*([^,]*)(?:,|$)",
                             body_nc, re.I):
            cname, coltype, rest = m.group(1), re.sub(r"\s+", "", m.group(2).lower()), m.group(3).lower()
            if cname in t["columns"]:
                continue
            dm = re.search(r"default\s+('(?:[^'\\]|\\.)*'|[\w.]+)", rest)
            t["columns"][cname] = {
                "type": coltype,
                "base_type": re.sub(r"\(.*", "", coltype),
                "not_null": bool(re.search(r"not\s+null", rest)),
                "auto_increment": False,
                "default": (dm.group(1).strip("'") if dm else ("<null-allowed>" if not re.search(r"not\s+null", rest) else None)),
                "added_by": fname,
            }
        for m in re.finditer(r"add\s+unique\s+key\s+(\w+)\s*\(([^)]*)\)", body_nc, re.I):
            cols = [c.strip().strip("`") for c in m.group(2).split(",")]
            if m.group(1) in t["unique_keys"] and t["unique_keys"][m.group(1)] != cols:
                t["unique_keys"]["__conflict_" + m.group(1)] = cols
                t["unique_keys"].setdefault("__conflicts", [])
                t["unique_keys"]["__conflicts"].append(m.group(1))
            else:
                t["unique_keys"][m.group(1)] = cols


def load_ddl():
    tables = OrderedDict()
    # 先解析全部 CREATE TABLE（主文件 26 张 + 增量文件 3 张 IF NOT EXISTS）
    for f in DDL_FILES_MAIN + DDL_FILES_INC:
        parse_create_tables(open(os.path.join(DDL_DIR, f), encoding="utf-8").read(), tables)
    # 再解析增量 ALTER（legacy-import 只取 ADD COLUMN / ADD UNIQUE KEY）
    for f in DDL_FILES_INC + DDL_FILES_ALTER_ONLY:
        parse_alters(open(os.path.join(DDL_DIR, f), encoding="utf-8").read(), tables, f)
    return tables


def camel_to_snake(name):
    return re.sub(r"(?<=[a-z0-9])([A-Z])", r"_\1", name).lower()


def parse_base_entity():
    path = os.path.join(REPO, "ruoyi-common", "ruoyi-common-mybatis", "src", "main", "java",
                        "org", "ruoyi", "common", "mybatis", "core", "domain", "BaseEntity.java")
    return parse_fields(open(path, encoding="utf-8").read())


def parse_fields(src):
    """解析字段声明：@TableField/@TableId/@Version + private Type name (= init)?;"""
    fields = OrderedDict()
    ann = {}
    for line in src.splitlines():
        s = line.strip()
        m = re.match(r"@TableField\s*\((.*)\)\s*$", s)
        if m:
            ann["tableField"] = m.group(1)
            continue
        if re.match(r"@TableId\s*(\(.*\))?\s*$", s):
            ann["tableId"] = True
            continue
        if re.match(r"@Version\s*$", s):
            ann["version"] = True
            continue
        m = re.match(r"private\s+([\w.<>\[\]]+)\s+(\w+)\s*(?:=\s*([^;]+))?;\s*$", s)
        if m:
            jtype, jname, init = m.group(1), m.group(2), (m.group(3).strip() if m.group(3) else None)
            tf = ann.get("tableField", "")
            if re.search(r"exist\s*=\s*false", tf):
                ann.clear()
                continue
            col = None
            mv = re.search(r'value\s*=\s*"([^"]+)"', tf)
            ms = re.match(r'"([^"]+)"\s*(,|$)', tf)
            if mv:
                col = mv.group(1)
            elif ms:
                col = ms.group(1)
            else:
                col = camel_to_snake(jname)
            fields[jname] = {
                "column": col,
                "java_type": jtype,
                "is_id": "tableId" in ann,
                "is_version": "version" in ann,
                "init": init,
            }
            ann.clear()
        elif s and not s.startswith(("@", "//", "*", "/*", "{", "}")):
            ann.clear() if s.startswith("private") else None
    return fields


def load_entities():
    base = parse_base_entity()
    ents = OrderedDict()
    for fn in sorted(os.listdir(DOMAIN_DIR)):
        if not fn.endswith(".java") or fn in ("SoftDeletable.java",):
            continue
        src = open(os.path.join(DOMAIN_DIR, fn), encoding="utf-8").read()
        m = re.search(r"@TableName\s*\(\s*(?:value\s*=\s*)?\"([^\"]+)\"", src)
        if not m:
            continue
        decl = re.search(r"class\s+\w+\s+extends\s+BaseEntity", src)
        class_head = re.search(r"class\s+\w+[^{]*\{", src).group(0)
        fields = OrderedDict(base) if decl else parse_fields(src)
        fields.update(parse_fields(src))
        ents[fn[:-5]] = {
            "table": m.group(1),
            "soft_deletable": "SoftDeletable" in class_head,
            "fields": fields,
            "file": fn,
        }
    return ents


def check_entities(ents, tables):
    rows, errors, warns = [], [], []
    ddl_tables = set(tables)
    ent_tables = {v["table"]: k for k, v in ents.items()}
    for t in sorted(ddl_tables - set(ent_tables)):
        errors.append({"level": "ERROR", "code": "TABLE_WITHOUT_ENTITY", "table": t,
                       "detail": "DDL 表无对应 @TableName 实体，26 表映射缺口"})
    for t in sorted(set(ent_tables) - ddl_tables):
        errors.append({"level": "ERROR", "code": "ENTITY_WITHOUT_TABLE", "entity": ent_tables[t],
                       "table": t, "detail": "实体声明的表不在仓库 DDL 中"})
    for ename, e in ents.items():
        tname = e["table"]
        if tname not in tables:
            continue
        t = tables[tname]
        ddl_cols = set(t["columns"])
        ent_cols = {f["column"]: f for f in e["fields"].values()}
        ok = mismatch_type = 0
        for col, f in ent_cols.items():
            if col not in ddl_cols:
                errors.append({"level": "ERROR", "code": "COLUMN_NOT_IN_DDL", "table": tname,
                               "entity": ename, "column": col,
                               "detail": "实体字段 %s(%s) 在 DDL 无对应列（insert/update 将失败）"
                                         % (f["java_type"], col)})
                continue
            base = t["columns"][col]["base_type"]
            allowed = JAVA_TYPE_OK.get(base, set())
            if f["java_type"] not in allowed:
                severity = "ERROR" if base in JSON_TYPES | DATE_TYPES else "WARN"
                item = {"level": severity, "code": "TYPE_MISMATCH", "table": tname, "entity": ename,
                        "column": col, "detail": "DDL %s ↔ Java %s（契约 %s）" % (base, f["java_type"], sorted(allowed))}
                (errors if severity == "ERROR" else warns).append(item)
                mismatch_type += 1
            else:
                ok += 1
            if f["is_version"] and f["java_type"] not in ("Integer", "int", "Long", "long"):
                errors.append({"level": "ERROR", "code": "VERSION_TYPE", "table": tname,
                               "entity": ename, "column": col, "detail": "@Version 必须是整型"})
        ddl_only = sorted(ddl_cols - set(ent_cols))
        for col in ddl_only:
            c = t["columns"][col]
            if c["not_null"] and c["default"] is None and not c["auto_increment"]:
                errors.append({"level": "ERROR", "code": "REQUIRED_COLUMN_UNMAPPED", "table": tname,
                               "entity": ename, "column": col,
                               "detail": "DDL NOT NULL 无默认值列未被实体映射（insert 必失败）"})
            else:
                warns.append({"level": "WARN", "code": "DDL_COLUMN_UNMAPPED", "table": tname,
                              "entity": ename, "column": col,
                              "detail": "DDL 列未被实体映射（应用不可读写，info 级）"})
        for col in ("del_flag", "tenant_id"):
            has_ddl, has_ent = col in ddl_cols, col in ent_cols
            if tname in NO_DEL_FLAG_WHITELIST and col == "del_flag":
                continue
            if has_ddl != has_ent:
                # tenant_id 缺失由 TenantLineInnerInterceptor 对非排除表自动补列兜底；
                # ipd 26 表已整体登记 tenant.excludes（application.yml:164-194），DB 默认 '000000' 生效，
                # 实体侧字段缺失无运行时影响 → WARN。
                # del_flag 缺失同理为设计分歧（stage_actions/gate_review_elements 无软删入口）→ WARN。
                level = "WARN"
                code = ("DELFLAG_MISMATCH" if col == "del_flag" else "TENANT_MISMATCH")
                warns.append({"level": level, "code": code, "table": tname, "entity": ename,
                              "detail": "%s DDL存在=%s 实体存在=%s（无运行时影响，设计分歧）" % (col, has_ddl, has_ent)})
        # 默认值对照（实体有初始化值时）
        for col, f in ent_cols.items():
            if f["init"] and col in ddl_cols and t["columns"][col]["default"] not in (None, "<null-allowed>"):
                ddl_def = t["columns"][col]["default"]
                java_def = f["init"].strip("\"'")
                warns.append({"level": "INFO", "code": "DEFAULT_COMPARE", "table": tname, "entity": ename,
                              "column": col, "detail": "DDL default='%s' ↔ Java init='%s'（%s）"
                                                       % (ddl_def, java_def, "一致" if ddl_def == java_def else "不一致")})
        rows.append({
            "table": tname, "entity": ename,
            "ddl_columns": len(ddl_cols), "entity_columns": len(ent_cols),
            "type_ok": ok, "type_mismatch": mismatch_type,
            "entity_only": sorted(set(ent_cols) - ddl_cols),
            "ddl_only": ddl_only,
        })
    return rows, errors, warns


def load_live(tsv):
    live = {}
    if not os.path.exists(tsv):
        return None
    for line in open(tsv, encoding="utf-8"):
        parts = line.rstrip("\n").split("\t")
        if len(parts) >= 5:
            live.setdefault(parts[0], OrderedDict())[parts[1]] = {"type": parts[2], "nullable": parts[3], "default": parts[4]}
    return live


def load_live_indexes(tsv):
    idx = {}
    if not os.path.exists(tsv):
        return None
    for line in open(tsv, encoding="utf-8"):
        parts = line.rstrip("\n").split("\t")
        if len(parts) >= 4:
            idx.setdefault(parts[0], {})[parts[1]] = {"non_unique": parts[2], "columns": parts[3].split(",")}
    return idx


def crosscheck(tables, live_cols, live_idx, warns):
    if live_cols is None:
        return None
    drift = {"tables_only_in_ddl": [], "tables_only_in_live": [], "columns": [], "unique_keys": []}
    for t in sorted(set(tables) - set(live_cols)):
        drift["tables_only_in_ddl"].append(t)
    for t in sorted(set(live_cols) - set(tables)):
        drift["tables_only_in_live"].append(t)
    for t in sorted(set(tables) & set(live_cols)):
        d, l = set(tables[t]["columns"]), set(live_cols[t])
        for c in sorted(d - l):
            drift["columns"].append({"table": t, "column": c, "detail": "仓库 DDL 有、ipd_dev 无"})
        for c in sorted(l - d):
            drift["columns"].append({"table": t, "column": c, "detail": "ipd_dev 有、仓库 DDL 无"})
    if live_idx:
        for t in sorted(set(tables) & set(live_idx)):
            for name, meta in sorted(live_idx[t].items()):
                if int(meta["non_unique"]) != 0 or name == "PRIMARY":
                    continue
                ddl_uk = tables[t]["unique_keys"].get(name)
                if ddl_uk is None:
                    drift["unique_keys"].append({"table": t, "key": name, "detail": "ipd_dev 存在但仓库 DDL 未定义"})
                elif sorted(ddl_uk) != sorted(meta["columns"]):
                    drift["unique_keys"].append({"table": t, "key": name,
                                                 "detail": "列不同：DDL %s ↔ 线上 %s" % (ddl_uk, meta["columns"])})
            for name, cols in sorted(tables[t]["unique_keys"].items()):
                if name.startswith("__"):
                    continue
                if name not in live_idx.get(t, {}):
                    drift["unique_keys"].append({"table": t, "key": name, "detail": "仓库 DDL 定义但 ipd_dev 未建（迁移未生效）",
                                                 "columns": cols})
    for item in drift["unique_keys"]:
        warns.append({"level": "WARN", "code": "LIVE_DRIFT_UK", **item})
    for item in drift["columns"]:
        warns.append({"level": "WARN", "code": "LIVE_DRIFT_COLUMN", **item})
    return drift


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default=os.path.join(os.path.dirname(__file__), "QA-04-mapping-result.json"))
    ap.add_argument("--live-columns", default="/tmp/qa04_ipd_dev_schema.tsv")
    ap.add_argument("--live-indexes", default="/tmp/qa04_ipd_dev_indexes.tsv")
    args = ap.parse_args()

    tables = load_ddl()
    ents = load_entities()
    rows, errors, warns = check_entities(ents, tables)
    live_cols = load_live(args.live_columns)
    live_idx = load_live_indexes(args.live_indexes)
    drift = crosscheck(tables, live_cols, live_idx, warns)

    result = {
        "generated_by": "qa04_ddl_entity_mapping.py",
        "scope": {"ddl_main": DDL_FILES_MAIN, "ddl_incremental": DDL_FILES_INC + DDL_FILES_ALTER_ONLY,
                  "domain_dir": os.path.relpath(DOMAIN_DIR, REPO)},
        "summary": {
            "tables_in_ddl": len(tables), "entities": len(ents),
            "matched": len(rows), "errors": len(errors), "warns": len(warns),
            "columns_total": sum(r["ddl_columns"] for r in rows),
        },
        "matrix": rows,
        "issues": {"errors": errors, "warnings": warns},
        "live_drift_ipd_dev": drift,
    }
    with open(args.out, "w", encoding="utf-8") as fh:
        json.dump(result, fh, ensure_ascii=False, indent=2)
    print("tables=%d entities=%d matched=%d errors=%d warns=%d" % (
        len(tables), len(ents), len(rows), len(errors), len(warns)))
    for e in errors:
        print("ERROR %s %s: %s" % (e["code"], e.get("table", ""), e["detail"]))
    sys.exit(1 if errors else 0)


if __name__ == "__main__":
    main()
