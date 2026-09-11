#!/usr/bin/env python3
"""
IPD 全局完整性审计脚本（病根 1+2+3 综合诊断）

输出：
1. 看板全 status 分布（fresh GET）
2. AcceptanceTest 测试类型分布（Mockito vs SpringBootTest）
3. 镜像「todo | done」失真注记数
4. 验收报告 vs done 卡覆盖率
5. 业务表 0 行探测（docker exec mysql）
6. commit 写「feat」但只动 test 文件的失真 commit
7. 失真卡清单 + 病根分类报告

用法：
  python3 scripts/audit-ipd-completeness.py [--json out.json] [--verbose]

边界：只读探针，不改任何代码 / 不 commit / 不 push。
"""

import json
import os
import re
import subprocess
import sys
import urllib.request
from collections import Counter, defaultdict
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent
BOARD = "http://127.0.0.1:62250"
PID = "01dcf15c-86bb-4c7b-957c-8fe44bddd10d"  # ruoyi-ai 项目
MIRROR = REPO_ROOT / "docs/ipd-系统说明/开发计划-看板镜像.md"
ACCEPTANCE_DIR = REPO_ROOT / "docs/ipd-系统说明/验收"
TEST_DIR = REPO_ROOT / "ruoyi-modules/ruoyi-ipd/src/test/java/org/ruoyi/ipd"

# 业务表清单（病根 6：业务表无种子 + 无真活流量）
BUSINESS_TABLES = [
    "ai_documents",
    "contributions",
    "negative_feedbacks",
    "allowance_ledgers",
    "requirement_changes",
    "handover_records",
    "bonus_pools",
    "kpi_records",
    "kpi_shared_confirms",
    "projects",
    "persons",
    "audit_logs",
]


def fresh_board_status():
    """病根 3：看板 fresh GET 全 status 分布"""
    try:
        data = json.loads(
            urllib.request.urlopen(f"{BOARD}/api/tasks?project_id={PID}", timeout=10).read()
        )["data"]
    except Exception as e:
        return {"error": f"看板不可达: {e}", "tasks": []}
    c = Counter(t.get("status", "?") for t in data)
    return {"total": len(data), "by_status": dict(c), "tasks": data}


def scan_acceptance_tests():
    """病根 1：AcceptanceTest 测试类型分布（Mockito vs SpringBootTest）"""
    tests = list(TEST_DIR.rglob("*AcceptanceTest.java"))
    mockito = []
    springboot = []
    other = []
    for t in tests:
        content = t.read_text(encoding="utf-8", errors="ignore")
        if "MockitoExtension" in content:
            mockito.append(str(t.relative_to(REPO_ROOT)))
        elif "SpringBootTest" in content:
            springboot.append(str(t.relative_to(REPO_ROOT)))
        else:
            other.append(str(t.relative_to(REPO_ROOT)))
    return {
        "total": len(tests),
        "mockito_count": len(mockito),
        "mockito_pct": round(len(mockito) * 100 / max(len(tests), 1)),
        "springboot_count": len(springboot),
        "springboot_pct": round(len(springboot) * 100 / max(len(tests), 1)),
        "other_count": len(other),
        "springboot_files": springboot,
        "mockito_sample": mockito[:10],
    }


def scan_mirror_drift():
    """病根 3：镜像「todo | done」失真注记数"""
    if not MIRROR.exists():
        return {"error": "镜像文件不存在"}
    content = MIRROR.read_text(encoding="utf-8", errors="ignore")
    # 匹配「| todo | done |」或「todo → done」失真注记
    drift_pattern = re.compile(r"\|\s*todo\s*\|\s*done\s*\|")
    arrow_pattern = re.compile(r"todo\s*→\s*done|todo->done")
    drift_lines = []
    for i, line in enumerate(content.split("\n"), 1):
        if drift_pattern.search(line) or arrow_pattern.search(line):
            drift_lines.append({"line": i, "content": line.strip()[:200]})
    return {
        "drift_count": len(drift_lines),
        "drift_lines": drift_lines[:20],  # 只输出前 20 条
    }


def scan_acceptance_reports():
    """病根 3：验收报告 vs done 卡覆盖率"""
    if not ACCEPTANCE_DIR.exists():
        return {"error": "验收报告目录不存在"}
    reports = list(ACCEPTANCE_DIR.glob("*.md"))
    return {
        "report_count": len(reports),
        "report_files": [str(r.name) for r in reports[:20]],
    }


def probe_business_tables():
    """病根 6：业务表 0 行探测（docker exec mysql）"""
    try:
        root_pwd = subprocess.check_output(
            ["docker", "exec", "ruoyi-ai-mysql", "printenv", "MYSQL_ROOT_PASSWORD"],
            stderr=subprocess.DEVNULL,
            timeout=5,
        ).decode().strip()
    except Exception as e:
        return {"error": f"docker mysql 不可达: {e}"}

    sql_parts = [f"SELECT '{t}' AS tbl, COUNT(*) AS rows_count FROM {t}" for t in BUSINESS_TABLES]
    sql = " UNION ALL ".join(sql_parts) + ";"
    try:
        out = subprocess.check_output(
            ["docker", "exec", "ruoyi-ai-mysql", "mysql", "-uroot", f"-p{root_pwd}", "ipd_dev", "-e", sql],
            stderr=subprocess.DEVNULL,
            timeout=15,
        ).decode()
    except Exception as e:
        return {"error": f"mysql 查询失败: {e}"}

    rows = {}
    for line in out.strip().split("\n")[1:]:  # 跳过表头
        parts = line.split("\t")
        if len(parts) == 2:
            rows[parts[0]] = int(parts[1])
    zero_tables = [t for t, c in rows.items() if c == 0]
    return {
        "rows": rows,
        "zero_count": len(zero_tables),
        "zero_tables": zero_tables,
    }


def scan_feat_test_only_commits(since="2026-09-01"):
    """病根 2：commit 写「feat」但只动 test 文件的失真 commit"""
    try:
        log = subprocess.check_output(
            ["git", "log", "--all", "--oneline", f"--since={since}"],
            cwd=REPO_ROOT, stderr=subprocess.DEVNULL, timeout=10,
        ).decode()
    except Exception as e:
        return {"error": f"git log 失败: {e}"}

    suspicious = []
    for line in log.strip().split("\n"):
        if not line:
            continue
        parts = line.split(" ", 1)
        if len(parts) < 2:
            continue
        hash_, msg = parts
        if not msg.startswith("feat"):
            continue
        try:
            stat = subprocess.check_output(
                ["git", "show", "--stat", hash_],
                cwd=REPO_ROOT, stderr=subprocess.DEVNULL, timeout=5,
            ).decode()
        except Exception:
            continue
        # 只看 1-2 文件改动且含 Test.java 但无 Service/Controller/Mapper/Domain
        file_count_match = re.search(r"(\d+)\s+files?\s+changed", stat)
        if not file_count_match:
            continue
        file_count = int(file_count_match.group(1))
        if file_count > 2:
            continue
        if "Test.java" not in stat:
            continue
        if any(k in stat for k in ["Service.java", "Controller.java", "Mapper.java", "Domain.java", "/domain/"]):
            continue
        suspicious.append({"hash": hash_, "msg": msg[:120], "files": file_count})
    return {
        "count": len(suspicious),
        "commits": suspicious[:15],
    }


def scan_pom_test_excludes():
    """病根 1：pom testExcludes 静默排除项"""
    pom_files = [
        REPO_ROOT / "pom.xml",
        REPO_ROOT / "ruoyi-modules/ruoyi-ipd/pom.xml",
    ]
    excludes = []
    for pom in pom_files:
        if not pom.exists():
            continue
        content = pom.read_text(encoding="utf-8", errors="ignore")
        # 找 <testExcludes>...</testExcludes> 段
        for m in re.finditer(r"<testExcludes>(.*?)</testExcludes>", content, re.DOTALL):
            seg = m.group(1)
            items = re.findall(r"<exclude>(.*?)</exclude>", seg)
            excludes.append({
                "pom": str(pom.relative_to(REPO_ROOT)),
                "items": items,
            })
    return {"count": sum(len(e["items"]) for e in excludes), "excludes": excludes}


def classify_pathology(report):
    """病根分类汇总"""
    pathologies = []

    # 病根 1：测试金字塔倒置
    t = report.get("acceptance_tests", {})
    if t.get("springboot_pct", 100) < 10:
        pathologies.append({
            "id": 1,
            "name": "测试金字塔倒置",
            "severity": "致命",
            "evidence": f"{t.get('total', 0)} 张 AcceptanceTest 仅 {t.get('springboot_count', 0)} 张 SpringBootTest 真活（{t.get('springboot_pct', 0)}%），{t.get('mockito_count', 0)} 张 Mockito Mock（{t.get('mockito_pct', 0)}%）",
            "fix": "新增 IpdIntegrationTestBase 抽象基类（@SpringBootTest + 真库 + MockMvc）+ 改名 Mockito Mock 单测为 *ContractTest",
        })

    # 病根 2：commit 治理无门禁
    f = report.get("feat_test_only_commits", {})
    if f.get("count", 0) > 0:
        pathologies.append({
            "id": 2,
            "name": "commit 治理无门禁",
            "severity": "高",
            "evidence": f"{f.get('count', 0)} 个 commit 写「feat」但只动 test 文件（无 Service/Controller/Mapper）",
            "fix": "新增 .gitmessage commit 模板 + pre-commit hook（拦截 feat commit 但只动 test）+ commitlint",
        })

    # 病根 3：镜像/看板/验收报告三方无强一致性
    m = report.get("mirror_drift", {})
    r = report.get("acceptance_reports", {})
    b = report.get("board_status", {})
    done_count = b.get("by_status", {}).get("done", 0)
    report_count = r.get("report_count", 0)
    coverage = round(report_count * 100 / max(done_count, 1))
    if m.get("drift_count", 0) > 0 or coverage < 80:
        pathologies.append({
            "id": 3,
            "name": "镜像/看板/验收报告三方无强一致性",
            "severity": "高",
            "evidence": f"镜像 {m.get('drift_count', 0)} 处「todo | done」失真注记；验收报告覆盖率 {coverage}%（{report_count}/{done_count}）",
            "fix": "新增 scripts/check-mirror-vs-board.py + PUT 后强制 GET 复核 + 翻 done 硬门禁（验收报告 + 业务表非 0 行 + DDL apply + HTTP 200）",
        })

    # 病根 6：业务表无种子 + 无真活流量
    bt = report.get("business_tables", {})
    if bt.get("zero_count", 0) > 0:
        pathologies.append({
            "id": 6,
            "name": "业务表无种子 + 无真活流量",
            "severity": "中",
            "evidence": f"{bt.get('zero_count', 0)} 张业务表 0 行：{', '.join(bt.get('zero_tables', [])[:5])}",
            "fix": "写 scripts/seed-ipd-business-data.py（种子数据生成）",
        })

    # 病根 1 子项：pom testExcludes 静默排除
    p = report.get("pom_test_excludes", {})
    if p.get("count", 0) > 0:
        pathologies.append({
            "id": "1b",
            "name": "pom testExcludes 静默排除（第二层假绿）",
            "severity": "高",
            "evidence": f"pom 静默排除 {p.get('count', 0)} 个测试类：{p.get('excludes', [])}",
            "fix": "移除 pom testExcludes 静默排除项 + 写 scripts/check-pom-test-excludes.py 探测",
        })

    return pathologies


def main():
    verbose = "--verbose" in sys.argv
    json_out = None
    if "--json" in sys.argv:
        idx = sys.argv.index("--json")
        if idx + 1 < len(sys.argv):
            json_out = sys.argv[idx + 1]

    print("=" * 70)
    print("IPD 全局完整性审计（病根 1+2+3 综合诊断）")
    print("=" * 70)
    print()

    report = {}

    print("[1/7] fresh 拉看板全 status 分布...")
    report["board_status"] = fresh_board_status()
    bs = report["board_status"]
    if "error" in bs:
        print(f"  ✗ {bs['error']}")
    else:
        print(f"  总卡数 {bs['total']}")
        for k, v in sorted(bs["by_status"].items(), key=lambda x: -x[1]):
            print(f"    {k:12} {v}")
    print()

    print("[2/7] 扫描 AcceptanceTest 测试类型...")
    report["acceptance_tests"] = scan_acceptance_tests()
    at = report["acceptance_tests"]
    print(f"  总数 {at['total']}")
    print(f"  Mockito Mock:        {at['mockito_count']} ({at['mockito_pct']}%)")
    print(f"  SpringBootTest 真活: {at['springboot_count']} ({at['springboot_pct']}%)")
    print(f"  其他:                {at['other_count']}")
    if verbose and at["springboot_files"]:
        print(f"  SpringBootTest 文件: {at['springboot_files']}")
    print()

    print("[3/7] 扫描镜像「todo | done」失真注记...")
    report["mirror_drift"] = scan_mirror_drift()
    md = report["mirror_drift"]
    print(f"  失真注记数: {md.get('drift_count', 0)}")
    if verbose and md.get("drift_lines"):
        for d in md["drift_lines"][:5]:
            print(f"    line {d['line']}: {d['content'][:80]}")
    print()

    print("[4/7] 扫描验收报告覆盖率...")
    report["acceptance_reports"] = scan_acceptance_reports()
    ar = report["acceptance_reports"]
    done_count = bs.get("by_status", {}).get("done", 0)
    coverage = round(ar.get("report_count", 0) * 100 / max(done_count, 1))
    print(f"  验收报告数: {ar.get('report_count', 0)}")
    print(f"  done 卡数:  {done_count}")
    print(f"  覆盖率:     {coverage}%")
    print()

    print("[5/7] 探测业务表 0 行...")
    report["business_tables"] = probe_business_tables()
    bt = report["business_tables"]
    if "error" in bt:
        print(f"  ✗ {bt['error']}")
    else:
        print(f"  0 行业务表数: {bt['zero_count']}")
        for t, c in bt["rows"].items():
            mark = "✗" if c == 0 else "✓"
            print(f"    {mark} {t:25} {c}")
    print()

    print("[6/7] 扫描 commit 写「feat」但只动 test 文件...")
    report["feat_test_only_commits"] = scan_feat_test_only_commits()
    fc = report["feat_test_only_commits"]
    print(f"  失真 commit 数: {fc.get('count', 0)}")
    if verbose and fc.get("commits"):
        for c in fc["commits"][:5]:
            print(f"    {c['hash']} {c['msg'][:80]}")
    print()

    print("[7/7] 扫描 pom testExcludes 静默排除项...")
    report["pom_test_excludes"] = scan_pom_test_excludes()
    pe = report["pom_test_excludes"]
    print(f"  静默排除项数: {pe.get('count', 0)}")
    if verbose and pe.get("excludes"):
        for e in pe["excludes"]:
            print(f"    {e['pom']}: {e['items']}")
    print()

    print("=" * 70)
    print("病根分类汇总")
    print("=" * 70)
    pathologies = classify_pathology(report)
    report["pathologies"] = pathologies
    for p in pathologies:
        print(f"\n病根 {p['id']}：{p['name']}（严重度：{p['severity']}）")
        print(f"  证据: {p['evidence']}")
        print(f"  根治: {p['fix']}")
    print()

    if json_out:
        Path(json_out).write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
        print(f"JSON 报告已写入: {json_out}")

    print("=" * 70)
    print(f"审计完成。共发现 {len(pathologies)} 类病根。")
    print("=" * 70)


if __name__ == "__main__":
    main()
