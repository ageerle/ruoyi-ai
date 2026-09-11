#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
catalog-matrix-link-check.py — OD-AM-04 双向校验脚本

读 catalog（IPD系统_验收清单.md）和 matrix（acceptance-matrix.json），
做双向 diff：
  - catalog 存在但 matrix 缺 → 警告（owner 后续补导入）
  - matrix 存在但 catalog 缺 → 警告（matrix 引用了 catalog 已删除的 AC）

输出 JSON 报告 docs/ipd-系统说明/治理/catalog-matrix-link-check-{date}.json

不阻断 CI（exit 0）；仅 stdout 打印告警。
"""
import argparse
import json
import re
import sys
from datetime import datetime
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent
CATALOG_PATH = REPO_ROOT / "docs/ipd-系统说明/外部资源/IPD系统_验收清单.md"
MATRIX_PATH = REPO_ROOT / "docs/ipd-系统说明/治理/acceptance-matrix.json"
REPORT_DIR = REPO_ROOT / "docs/ipd-系统说明/治理"

ROW_RE = re.compile(r"^\|\s*(?P<ac>AC-[A-Z]+-\d+[a-z]?)(?:\s*[⚠️🆕\s]*)?\s*\|")
AC_ID_RE = re.compile(r"AC-[A-Z]+-\d+[a-z]?")


def parse_catalog(path: Path):
    if not path.exists():
        sys.stderr.write(f"[ERROR] catalog 不存在: {path}\n")
        sys.exit(1)
    rows = []
    for line in path.read_text(encoding="utf-8").splitlines():
        m = ROW_RE.match(line)
        if m:
            rows.append(m.group("ac"))
    return list(dict.fromkeys(rows))  # 去重保序


def parse_matrix(path: Path):
    if not path.exists():
        sys.stderr.write(f"[WARN] matrix 不存在: {path}\n")
        return []
    data = json.loads(path.read_text(encoding="utf-8"))
    return [r["ac_id"] for r in data.get("rows", []) if "ac_id" in r]


def crosscheck(catalog_ids, matrix_ids):
    cat_set, mat_set = set(catalog_ids), set(matrix_ids)
    only_in_catalog = sorted(cat_set - mat_set)
    only_in_matrix = sorted(mat_set - cat_set)
    common = sorted(cat_set & mat_set)
    return {
        "catalog_total": len(cat_set),
        "matrix_total": len(mat_set),
        "common": len(common),
        "only_in_catalog": only_in_catalog,
        "only_in_matrix": only_in_matrix,
    }


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--write", action="store_true", help="写 JSON 报告")
    args = p.parse_args()

    catalog_ids = parse_catalog(CATALOG_PATH)
    matrix_ids = parse_matrix(MATRIX_PATH)
    result = crosscheck(catalog_ids, matrix_ids)

    pct = result["common"] / max(result["catalog_total"], 1) * 100
    print(f"catalog 总数: {result['catalog_total']}")
    print(f"matrix  总数: {result['matrix_total']}")
    print(f"双向覆盖:     {result['common']}/{result['catalog_total']} ({pct:.1f}%)")
    print(f"⚠️  catalog 有 matrix 缺: {len(result['only_in_catalog'])} 条")
    print(f"⚠️  matrix 有 catalog 缺: {len(result['only_in_matrix'])} 条")
    if result["only_in_catalog"]:
        print("--- 前 20 条 catalog 缺 ---")
        for ac in result["only_in_catalog"][:20]:
            print(f"  {ac}")
    if result["only_in_matrix"]:
        print("--- 前 20 条 matrix 缺 ---")
        for ac in result["only_in_matrix"][:20]:
            print(f"  {ac}")

    if args.write:
        date = datetime.now().strftime("%Y%m%d")
        out = REPORT_DIR / f"catalog-matrix-link-check-{date}.json"
        out.write_text(
            json.dumps({
                "ts": datetime.now().astimezone().isoformat(timespec="seconds"),
                "purpose": "OD-AM-04 catalog ↔ matrix 双向校验",
                "catalog_path": str(CATALOG_PATH.relative_to(REPO_ROOT)),
                "matrix_path": str(MATRIX_PATH.relative_to(REPO_ROOT)),
                **result,
                "coverage_pct": round(pct, 2),
            }, ensure_ascii=False, indent=2) + "\n",
            encoding="utf-8",
        )
        print(f"[WRITE] 报告: {out.relative_to(REPO_ROOT)}")


if __name__ == "__main__":
    main()
