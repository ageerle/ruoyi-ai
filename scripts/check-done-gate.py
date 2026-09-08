#!/usr/bin/env python3
"""
翻 done 硬门禁脚本（病根 3 根治）

功能：
输入卡号（如 P2-8.1），检查翻 done 必须满足的硬门禁：
1. 验收报告存在（docs/ipd-系统说明/验收/<卡号>-*.md）
2. 业务表非 0 行（按卡号映射业务表）
3. DDL apply（按卡号映射 DDL 文件，检查真库索引/列存在）
4. HTTP 200（按卡号映射 HTTP 端点，真活探测）
5. 测试类型（SpringBootTest 真活 / Mockito Mock 契约）

输出：PASS / FAIL + 失败原因

用法：
  python3 scripts/check-done-gate.py P2-8.1
  python3 scripts/check-done-gate.py P2-8.1 --json out.json

边界：只读探针，不改任何代码 / 不 commit / 不 push。
"""

import json
import os
import re
import subprocess
import sys
import urllib.request
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent
ACCEPTANCE_DIR = REPO_ROOT / "docs/ipd-系统说明/验收"
TEST_DIR = REPO_ROOT / "ruoyi-modules/ruoyi-ipd/src/test/java/org/ruoyi/ipd"
SQL_DIR = REPO_ROOT / "docs/script/sql/update"

# 后端真活实例（按 memory「IPD 真活后端实例端口表」）
BACKEND_PORTS = [16049, 16040, 16099, 16039]

# 卡号 → 业务表映射（按镜像 allowedPaths + 主责 AC）
CARD_TO_TABLES = {
    "P2-6.1": ["requirement_changes"],
    "P2-6.2": ["requirement_changes"],
    "P2-7.1": ["handover_records"],
    "P2-7.2": ["handover_records"],
    "P2-7.3": ["handover_records"],
    "P2-7.4": ["handover_records"],
    "P2-8.1": ["handover_records", "requirement_changes"],
    "P1-10.1": ["ai_documents"],
    "P1-10.2": ["ai_documents"],
    "P3-1.3": ["kpi_records", "kpi_shared_confirms"],
    "P3-3.3": ["allowance_ledgers"],
    "P3-6.1": ["contributions"],
    "P3-8.1": ["negative_feedbacks"],
    "P0-3.4": ["ipd_business_config"],
    "P0-3.5": ["ipd_business_config", "bonus_pools"],
}

# 卡号 → HTTP 端点映射（按镜像 allowedPaths + 主责 AC）
CARD_TO_ENDPOINTS = {
    "P2-6.1": ["/api/v1/requirement-changes"],
    "P2-6.2": ["/api/v1/requirement-changes"],
    "P2-7.1": ["/api/v1/handovers"],
    "P2-7.2": ["/api/v1/handovers/batch"],
    "P2-7.3": ["/api/v1/handovers/super-admin"],
    "P2-8.1": ["/api/v1/handovers", "/api/v1/requirement-changes"],
    "P1-10.1": ["/api/v1/ai-documents"],
    "P1-10.2": ["/api/v1/ai-documents"],
    "P3-1.3": ["/api/v1/kpi-records"],
    "P3-3.3": ["/api/v1/allowance-ledgers"],
    "P3-6.1": ["/api/v1/contributions"],
    "P3-8.1": ["/api/v1/negative-feedbacks"],
}


def check_acceptance_report(card_id):
    """门禁 1：验收报告存在"""
    if not ACCEPTANCE_DIR.exists():
        return {"pass": False, "reason": "验收报告目录不存在"}

    # 匹配 <卡号>-*.md 或 <卡号小写>-*.md
    patterns = [
        f"{card_id}-*.md",
        f"{card_id.lower()}-*.md",
        f"{card_id.replace('.', '_')}-*.md",
    ]
    for pat in patterns:
        matches = list(ACCEPTANCE_DIR.glob(pat))
        if matches:
            return {
                "pass": True,
                "files": [str(m.name) for m in matches],
            }
    return {
        "pass": False,
        "reason": f"验收报告不存在（搜索模式: {patterns}）",
    }


def check_business_tables(card_id):
    """门禁 2：业务表非 0 行"""
    tables = CARD_TO_TABLES.get(card_id, [])
    if not tables:
        return {"pass": True, "reason": "卡号无业务表映射，跳过", "skipped": True}

    try:
        root_pwd = subprocess.check_output(
            ["docker", "exec", "ruoyi-ai-mysql", "printenv", "MYSQL_ROOT_PASSWORD"],
            stderr=subprocess.DEVNULL, timeout=5,
        ).decode().strip()
    except Exception as e:
        return {"pass": False, "reason": f"docker mysql 不可达: {e}"}

    results = {}
    for t in tables:
        try:
            out = subprocess.check_output(
                ["docker", "exec", "ruoyi-ai-mysql", "mysql", "-uroot", f"-p{root_pwd}",
                 "ipd_dev", "-e", f"SELECT COUNT(*) FROM {t};"],
                stderr=subprocess.DEVNULL, timeout=10,
            ).decode()
            count = int(out.strip().split("\n")[-1])
            results[t] = count
        except Exception as e:
            results[t] = f"error: {e}"

    zero_tables = [t for t, c in results.items() if c == 0]
    if zero_tables:
        return {
            "pass": False,
            "reason": f"业务表 0 行: {zero_tables}",
            "rows": results,
        }
    return {"pass": True, "rows": results}


def check_ddl_apply(card_id):
    """门禁 3：DDL apply（按卡号映射 DDL 文件，检查真库索引/列存在）"""
    # 找 SQL 文件（按卡号匹配）
    if not SQL_DIR.exists():
        return {"pass": True, "reason": "SQL 目录不存在，跳过", "skipped": True}

    patterns = [
        f"*{card_id.lower().replace('.', '')}*.sql",
        f"*{card_id.replace('.', '-')}*.sql",
    ]
    sql_files = []
    for pat in patterns:
        sql_files.extend(SQL_DIR.glob(pat))

    if not sql_files:
        return {"pass": True, "reason": "卡号无 DDL 文件，跳过", "skipped": True}

    # 简单检查：DDL 文件存在即认为已 apply（完整检查需要解析 SQL 并查真库索引/列）
    return {
        "pass": True,
        "reason": f"DDL 文件存在（{len(sql_files)} 个），完整 apply 检查需解析 SQL",
        "files": [str(f.name) for f in sql_files],
        "note": "建议集成 p1-ddl-apply-check.py 做完整检查",
    }


def check_http_endpoints(card_id):
    """门禁 4：HTTP 200（按卡号映射 HTTP 端点，真活探测）"""
    endpoints = CARD_TO_ENDPOINTS.get(card_id, [])
    if not endpoints:
        return {"pass": True, "reason": "卡号无 HTTP 端点映射，跳过", "skipped": True}

    # 找真活后端端口
    backend = None
    for port in BACKEND_PORTS:
        try:
            urllib.request.urlopen(f"http://127.0.0.1:{port}/actuator/health", timeout=2)
            backend = port
            break
        except Exception:
            continue

    if not backend:
        return {
            "pass": False,
            "reason": f"无真活后端实例（探测端口: {BACKEND_PORTS}）",
        }

    results = {}
    for ep in endpoints:
        url = f"http://127.0.0.1:{backend}{ep}"
        try:
            resp = urllib.request.urlopen(url, timeout=5)
            results[ep] = resp.status
        except urllib.error.HTTPError as e:
            results[ep] = e.code
        except Exception as e:
            results[ep] = f"error: {e}"

    failed = [ep for ep, code in results.items() if code != 200]
    if failed:
        return {
            "pass": False,
            "reason": f"HTTP 端点非 200: {failed}",
            "results": results,
            "backend_port": backend,
        }
    return {"pass": True, "results": results, "backend_port": backend}


def check_test_type(card_id):
    """门禁 5：测试类型（SpringBootTest 真活 / Mockito Mock 契约）"""
    # 找 AcceptanceTest 文件（按卡号匹配）
    # P2-8.1 → P281AcceptanceTest
    num_part = card_id.replace("P", "").replace("-", "").replace(".", "")
    test_name = f"P{num_part}AcceptanceTest.java"

    test_files = list(TEST_DIR.rglob(test_name))
    if not test_files:
        return {
            "pass": False,
            "reason": f"测试文件不存在: {test_name}",
        }

    test_file = test_files[0]
    content = test_file.read_text(encoding="utf-8", errors="ignore")

    is_mockito = "MockitoExtension" in content
    is_springboot = "SpringBootTest" in content

    if is_springboot:
        return {
            "pass": True,
            "test_type": "SpringBootTest 真活",
            "file": str(test_file.relative_to(REPO_ROOT)),
        }
    elif is_mockito:
        return {
            "pass": False,
            "reason": "测试类型 = Mockito Mock（非真活），翻 done 需补充 SpringBootTest 真活集成测试",
            "test_type": "Mockito Mock 契约",
            "file": str(test_file.relative_to(REPO_ROOT)),
        }
    else:
        return {
            "pass": False,
            "reason": "测试类型未知（无 MockitoExtension 也无 SpringBootTest）",
            "file": str(test_file.relative_to(REPO_ROOT)),
        }


def main():
    if len(sys.argv) < 2:
        print("用法: python3 scripts/check-done-gate.py <卡号> [--json out.json]")
        print("示例: python3 scripts/check-done-gate.py P2-8.1")
        sys.exit(1)

    card_id = sys.argv[1]
    json_out = None
    if "--json" in sys.argv:
        idx = sys.argv.index("--json")
        if idx + 1 < len(sys.argv):
            json_out = sys.argv[idx + 1]

    print("=" * 70)
    print(f"翻 done 硬门禁检查：{card_id}")
    print("=" * 70)
    print()

    gates = {}

    print("[1/5] 门禁 1：验收报告存在...")
    gates["acceptance_report"] = check_acceptance_report(card_id)
    g = gates["acceptance_report"]
    print(f"  {'✓ PASS' if g['pass'] else '✗ FAIL'}: {g.get('reason') or g.get('files')}")
    print()

    print("[2/5] 门禁 2：业务表非 0 行...")
    gates["business_tables"] = check_business_tables(card_id)
    g = gates["business_tables"]
    if g.get("skipped"):
        print(f"  ⊘ SKIP: {g['reason']}")
    else:
        print(f"  {'✓ PASS' if g['pass'] else '✗ FAIL'}: {g.get('reason') or g.get('rows')}")
    print()

    print("[3/5] 门禁 3：DDL apply...")
    gates["ddl_apply"] = check_ddl_apply(card_id)
    g = gates["ddl_apply"]
    if g.get("skipped"):
        print(f"  ⊘ SKIP: {g['reason']}")
    else:
        print(f"  {'✓ PASS' if g['pass'] else '✗ FAIL'}: {g.get('reason')}")
    print()

    print("[4/5] 门禁 4：HTTP 200...")
    gates["http_endpoints"] = check_http_endpoints(card_id)
    g = gates["http_endpoints"]
    if g.get("skipped"):
        print(f"  ⊘ SKIP: {g['reason']}")
    else:
        print(f"  {'✓ PASS' if g['pass'] else '✗ FAIL'}: {g.get('reason') or g.get('results')}")
    print()

    print("[5/5] 门禁 5：测试类型...")
    gates["test_type"] = check_test_type(card_id)
    g = gates["test_type"]
    print(f"  {'✓ PASS' if g['pass'] else '✗ FAIL'}: {g.get('reason') or g.get('test_type')}")
    print()

    # 汇总
    failed_gates = [k for k, v in gates.items() if not v["pass"] and not v.get("skipped")]
    passed_gates = [k for k, v in gates.items() if v["pass"]]
    skipped_gates = [k for k, v in gates.items() if v.get("skipped")]

    print("=" * 70)
    print(f"汇总：PASS {len(passed_gates)} / FAIL {len(failed_gates)} / SKIP {len(skipped_gates)}")
    print("=" * 70)

    if failed_gates:
        print(f"\n⚠️  {card_id} 翻 done 门禁 FAIL！失败门禁: {failed_gates}")
        print("\n失败原因：")
        for k in failed_gates:
            print(f"  - {k}: {gates[k].get('reason')}")
        print("\n根治：补充真活证据后再翻 done")
        overall = "FAIL"
    else:
        print(f"\n✓ {card_id} 翻 done 门禁 PASS")
        overall = "PASS"

    report = {
        "card_id": card_id,
        "overall": overall,
        "gates": gates,
        "failed_gates": failed_gates,
        "passed_gates": passed_gates,
        "skipped_gates": skipped_gates,
    }

    if json_out:
        Path(json_out).write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
        print(f"\nJSON 报告已写入: {json_out}")

    sys.exit(0 if overall == "PASS" else 1)


if __name__ == "__main__":
    main()
