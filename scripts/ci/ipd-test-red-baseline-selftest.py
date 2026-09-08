#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""ipd-test-red-baseline.py 的自测：证明每道护栏真的会拦。

为什么门禁脚本自己也要被测（2026-09-08）：
    本仓 AGENTS.md 记的假绿陷阱同样适用于门禁本身——护栏条件写反、阈值配错、
    或被后续改动悄悄削弱，表现都是「检查通过」，与真的通过无法区分。
    只跑正向用例（当前红 == 基线 → PASS）证明不了任何事：把 check 改成
    `return 0` 也能通过正向用例。

    因此本脚本对每个 BLOCKED 分支都用构造样本触发一次，断言退出码与关键字。
    所有样本在临时目录里生成，不碰真实 target/，可随时重跑。

覆盖的护栏（与 ipd-test-red-baseline.py 的阻断分支一一对应）：
    pos_baseline_match      当前红 == 基线                     → 放行
    neg_new_red             出现基线之外的新红                 → 阻断
    neg_fixed_not_blocking  基线红被修好（清单变短）           → 放行但提示收紧
    neg_no_report           报告目录为空（套件没跑起来）       → 阻断
    neg_total_drop          测试总数骤降（@Tag 过滤/中断）     → 阻断
    neg_unparsable          报告 XML 损坏                      → 阻断
    neg_dtd                 报告含 DOCTYPE/ENTITY              → 阻断
    neg_ghost_report        混入上次跑的离群 mtime 报告        → 阻断
    neg_ancient_run         整批报告都是历史产物               → 阻断
    neg_silent_class        测试类有源文件却没产出报告         → 阻断
    pos_exempt_class        同上但已登记豁免                   → 放行
    neg_stale_exempt        豁免登记的类已无源文件             → 阻断
    neg_bad_src_glob        --src-glob 配错致覆盖校验失效      → 阻断
"""

from __future__ import annotations

import os
import subprocess
import sys
import tempfile
import time
from pathlib import Path

TARGET = Path(__file__).resolve().parent / "ipd-test-red-baseline.py"
HOUR = 3600


def write_report(reports: Path, cls: str, tests: int, reds=(), raw: str | None = None,
                 age_seconds: float = 0.0) -> Path:
    """写一份最小 surefire 报告；raw 非空时直接落原文（用于损坏/DTD 样本）。"""
    reports.mkdir(parents=True, exist_ok=True)
    path = reports / f"TEST-{cls}.xml"
    if raw is not None:
        path.write_text(raw, encoding="utf-8")
    else:
        reds = set(reds)
        cases = []
        for i in range(tests):
            method = f"m{i}"
            body = ('<failure message="boom" type="AssertionError">stack</failure>'
                    if method in reds else "")
            cases.append(f'<testcase name="{method}" classname="{cls}" time="0.01">{body}</testcase>')
        path.write_text(
            '<?xml version="1.0" encoding="UTF-8"?>\n'
            f'<testsuite name="{cls}" tests="{tests}" failures="{len(reds)}" '
            f'errors="0" skipped="0" time="0.1">\n' + "\n".join(cases) + "\n</testsuite>\n",
            encoding="utf-8")
    if age_seconds:
        old = time.time() - age_seconds
        os.utime(path, (old, old))
    return path


def write_src(root: Path, cls: str) -> None:
    """造一个与 FQN 对应的测试源文件，供覆盖校验对账。"""
    path = root / "src/test/java" / (cls.replace(".", "/") + ".java")
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(f"package x;\nclass {cls.rsplit('.', 1)[1]} {{}}\n", encoding="utf-8")


def write_baseline(path: Path, reds=(), total: int | None = None, exempt=()) -> Path:
    lines = ["# selftest baseline"]
    if total is not None:
        lines.append(f"# total_tests={total}")
    lines.extend(f"# exempt_class={c} 理由=selftest" for c in exempt)
    lines.extend(sorted(reds))
    path.write_text("\n".join(lines) + "\n", encoding="utf-8")
    return path


def run(reports: str, src: str, baseline: str, sub: str = "check",
        extra: tuple[str, ...] = ()) -> tuple[int, str]:
    cmd = [sys.executable, str(TARGET), "--reports", reports, "--src-glob", src, sub]
    if sub == "check":
        cmd += ["--baseline", baseline]
    cmd += list(extra)
    proc = subprocess.run(cmd, capture_output=True, text=True)
    return proc.returncode, proc.stdout + proc.stderr


RESULTS: list[tuple[str, bool, str]] = []


def expect(name: str, want_exit: int, got_exit: int, out: str, keyword: str = "") -> None:
    ok = got_exit == want_exit and (not keyword or keyword in out)
    detail = f"exit={got_exit}（期望 {want_exit}）"
    if keyword:
        detail += f" | 关键字 {keyword!r} {'命中' if keyword in out else '缺失'}"
    RESULTS.append((name, ok, detail))


def main() -> int:
    if not TARGET.exists():
        print(f"自测无法进行：被测脚本不存在 {TARGET}", file=sys.stderr)
        return 1

    with tempfile.TemporaryDirectory(prefix="ipd-gate-selftest-") as tmp:
        root = Path(tmp)
        A, B = "com.x.FooTest", "com.x.BarTest"

        # pos_baseline_match：红与基线一致，类覆盖齐全 → 放行
        d = root / "pos"; rep = d / "reports"
        write_report(rep, A, 3, reds={"m1"}); write_src(d, A)
        bl = write_baseline(d / "base.txt", reds={f"{A}#m1"}, total=3)
        code, out = run(f"{rep}/TEST-*.xml", f"{d}/src/test/java/**/*Test.java", str(bl))
        expect("pos_baseline_match", 0, code, out, "无新增红")

        # neg_new_red：多出一条基线外的红 → 阻断
        d = root / "newred"; rep = d / "reports"
        write_report(rep, A, 3, reds={"m1"}); write_report(rep, B, 2, reds={"m0"})
        write_src(d, A); write_src(d, B)
        bl = write_baseline(d / "base.txt", reds={f"{A}#m1"}, total=5)
        code, out = run(f"{rep}/TEST-*.xml", f"{d}/src/test/java/**/*Test.java", str(bl))
        expect("neg_new_red", 1, code, out, f"NEW  {B}#m0")

        # neg_fixed_not_blocking：基线红被修好 → 放行，但要求收紧基线
        d = root / "fixed"; rep = d / "reports"
        write_report(rep, A, 3); write_src(d, A)
        bl = write_baseline(d / "base.txt", reds={f"{A}#m1"}, total=3)
        code, out = run(f"{rep}/TEST-*.xml", f"{d}/src/test/java/**/*Test.java", str(bl))
        expect("neg_fixed_not_blocking", 0, code, out, "FIXED")

        # neg_no_report：报告目录空 → 阻断（不得按零红放行）
        d = root / "noreport"; (d / "reports").mkdir(parents=True); write_src(d, A)
        bl = write_baseline(d / "base.txt", reds=set(), total=3)
        code, out = run(f"{d}/reports/TEST-*.xml", f"{d}/src/test/java/**/*Test.java", str(bl))
        expect("neg_no_report", 1, code, out, "测试没跑起来")

        # neg_total_drop：报告在但测试数骤降 → 阻断
        d = root / "drop"; rep = d / "reports"
        write_report(rep, A, 1); write_src(d, A)
        bl = write_baseline(d / "base.txt", reds=set(), total=100)
        code, out = run(f"{rep}/TEST-*.xml", f"{d}/src/test/java/**/*Test.java", str(bl))
        expect("neg_total_drop", 1, code, out, "低于基线")

        # neg_unparsable：XML 截断损坏 → 阻断（不得静默当零红）
        d = root / "bad"; rep = d / "reports"
        write_report(rep, A, 3, raw='<?xml version="1.0"?><testsuite name="com.x.FooTest" tests="3"><testcase')
        write_src(d, A)
        bl = write_baseline(d / "base.txt", reds=set(), total=3)
        code, out = run(f"{rep}/TEST-*.xml", f"{d}/src/test/java/**/*Test.java", str(bl))
        expect("neg_unparsable", 1, code, out, "UNTRUSTED")

        # neg_dtd：报告含 DOCTYPE + 实体定义 → 阻断（防实体膨胀 / XXE）
        d = root / "dtd"; rep = d / "reports"
        write_report(rep, A, 3, raw='<?xml version="1.0"?>\n<!DOCTYPE testsuite [<!ENTITY a "b">]>\n'
                                    '<testsuite name="com.x.FooTest" tests="3"></testsuite>\n')
        write_src(d, A)
        bl = write_baseline(d / "base.txt", reds=set(), total=3)
        code, out = run(f"{rep}/TEST-*.xml", f"{d}/src/test/java/**/*Test.java", str(bl))
        expect("neg_dtd", 1, code, out, "DOCTYPE")

        # neg_ghost_report：混入 3 小时前的离群报告 → 阻断
        d = root / "ghost"; rep = d / "reports"
        write_report(rep, A, 3); write_report(rep, B, 2, age_seconds=3 * HOUR)
        write_src(d, A); write_src(d, B)
        bl = write_baseline(d / "base.txt", reds=set(), total=5)
        code, out = run(f"{rep}/TEST-*.xml", f"{d}/src/test/java/**/*Test.java", str(bl))
        expect("neg_ghost_report", 1, code, out, "幽灵报告")

        # neg_ancient_run：整批报告都是历史产物 → 阻断
        d = root / "ancient"; rep = d / "reports"
        write_report(rep, A, 3, age_seconds=3 * HOUR); write_src(d, A)
        bl = write_baseline(d / "base.txt", reds=set(), total=3)
        code, out = run(f"{rep}/TEST-*.xml", f"{d}/src/test/java/**/*Test.java", str(bl))
        expect("neg_ancient_run", 1, code, out, "历史报告")

        # neg_silent_class：B 有源文件却没报告（等价 pom testExcludes 豁免）→ 阻断
        d = root / "silent"; rep = d / "reports"
        write_report(rep, A, 3); write_src(d, A); write_src(d, B)
        bl = write_baseline(d / "base.txt", reds=set(), total=3)
        code, out = run(f"{rep}/TEST-*.xml", f"{d}/src/test/java/**/*Test.java", str(bl))
        expect("neg_silent_class", 1, code, out, f"SILENT {B}")

        # pos_exempt_class：同上但 B 已显式登记豁免 → 放行
        d = root / "exempt"; rep = d / "reports"
        write_report(rep, A, 3); write_src(d, A); write_src(d, B)
        bl = write_baseline(d / "base.txt", reds=set(), total=3, exempt=[B])
        code, out = run(f"{rep}/TEST-*.xml", f"{d}/src/test/java/**/*Test.java", str(bl))
        expect("pos_exempt_class", 0, code, out, "无新增红")

        # neg_stale_exempt：豁免登记的类已无源文件 → 阻断（防永久后门）
        d = root / "stale"; rep = d / "reports"
        write_report(rep, A, 3); write_src(d, A)
        bl = write_baseline(d / "base.txt", reds=set(), total=3, exempt=["com.x.GoneTest"])
        code, out = run(f"{rep}/TEST-*.xml", f"{d}/src/test/java/**/*Test.java", str(bl))
        expect("neg_stale_exempt", 1, code, out, "陈旧豁免")

        # neg_bad_src_glob：src-glob 指向空处，覆盖校验会静默失效 → 阻断
        d = root / "badglob"; rep = d / "reports"
        write_report(rep, A, 3); write_src(d, A)
        bl = write_baseline(d / "base.txt", reds=set(), total=3)
        code, out = run(f"{rep}/TEST-*.xml", f"{d}/nowhere/**/*.java", str(bl))
        expect("neg_bad_src_glob", 1, code, out, "glob 配错")

    print(f"{'用例':<26}{'结果':<8}详情")
    print("-" * 96)
    failed = 0
    for name, ok, detail in RESULTS:
        print(f"{name:<26}{'PASS' if ok else 'FAIL':<8}{detail}")
        failed += 0 if ok else 1
    print("-" * 96)
    total = len(RESULTS)
    if failed:
        print(f"自测失败 {failed}/{total} —— 门禁护栏未按设计工作，不得上线", file=sys.stderr)
        return 1
    print(f"自测全绿 {total}/{total}：1 放行基线 + 1 放行豁免 + 1 放行已修复 + {total - 3} 条阻断分支均如期触发")
    return 0


if __name__ == "__main__":
    sys.exit(main())
