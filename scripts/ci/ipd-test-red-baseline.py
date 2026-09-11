#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""IPD 测试红名单基线提取 / 校验器。

为什么需要它（2026-09-08 实证）：
    本仓 6 个 workflow 中唯一碰 Java 构建的是 ipd-compile-gate.yml，命令为
    `mvn -DskipTests test-compile` —— CI 只保证「能编译」，不保证「行为对」。
    实测 origin/main 上 1893 个测试有 21 红，CI 完全看不见；任何人可以把
    21 红推到 100 红而所有检查依旧全绿。

    直接把 `mvn test` 设成硬阻断会让所有 PR 立刻变红、冻结全部并行交付，
    因此采用「基线冻结 + 只挡新增红」：既有红登记为已知债务逐条消，
    新增红当场阻断。

两个子命令共用同一套提取逻辑，避免「生成基线」与「校验」两处实现漂移：
    extract  从 surefire 报告提取红名单（含 total_tests 指令行），用于生成基线
    check    把当前红名单与基线比对：新增红 → exit 1；已修复红 → 仅提示更新基线

红名单条目规范化到「类#方法」级：剥离参数化用例的实参与 [n] 序号，
避免同一用例因参数展示串变化而在基线里抖动。

解析安全与假绿防护（两条都是硬约束，不是风格问题）：
    1) 拒绝带 DOCTYPE / ENTITY 声明的 XML —— 防实体膨胀（billion laughs）与外部
       实体注入。surefire 报告是构建自产物，本不该出现 DTD，出现即视为文件被污染。
    2) 任何一份报告解析失败都计入 bad 并让调用方 exit 1，不允许静默跳过。
       理由：静默跳过会把「这份报告读不出来」洗成「这份报告零红」，
       正是本仓 AGENTS.md 记的假绿陷阱在门禁脚本里的翻版。
    3) 报告时间戳离群即判为幽灵残留。本地实测（2026-09-08）：213 份报告中
       3 份是 02:32 的旧产物，其中两个类的源文件已被删除（幽灵报告），
       导致 total_tests 虚高 15（XML 求和 1908 vs 控制台 1893）。
       幽灵报告会让基线把「已删除/本次未跑的类」永久豁免，故必须拦。
    4) 每个测试源文件都必须产出报告，否则阻断。仅比对红名单拦不住「静默跳过」：
       本地实测 ruoyi-modules/ruoyi-ipd/pom.xml 的 <testExcludes> 豁免了 5 个测试类
       （214 个 *Test.java 只编出 209 个 class），它们不编译不执行、一份报告不出，
       mvn 依旧 BUILD SUCCESS，红名单比对对此完全无感。确需豁免必须在基线里写
       `# exempt_class=<FQN> 理由… 登记…`，使豁免显式、带理由、带台账号。
"""

from __future__ import annotations

import argparse
import datetime as _dt
import glob
import os
import re
import sys
import time
import xml.etree.ElementTree as ET

DEFAULT_REPORT_GLOB = "ruoyi-modules/ruoyi-ipd/target/surefire-reports/TEST-*.xml"
DEFAULT_SRC_GLOB = "ruoyi-modules/ruoyi-ipd/src/test/java/**/*Test.java"
SRC_MARKER = "src/test/java/"

# 参数化用例名形如 method(arg)[1] / method[3]，统一截到方法名
_PARAM_SUFFIX = re.compile(r"[\[(].*$")

# 2026-09-08 AM-BASELINE-TTL：超期阈值，默认 14 天。WARN 不阻断（渐进版）。
DEFAULT_TTL_DAYS = 14
# 首次出现日期入册格式：ISO 日期注释，不带时区，与仓内现存日期习惯一致
_FIRST_SEEN_RE = re.compile(r"#\s*first_seen=(\d{4}-\d{2}-\d{2})")


def _normalize(name: str) -> str:
    return _PARAM_SUFFIX.sub("", name or "").strip()


# 单份 surefire 报告实测最大约 1.5 MB；给足余量后仍设上限，防被塞入巨型文件耗尽内存
_MAX_REPORT_BYTES = 32 * 1024 * 1024
_DTD_MARKERS = (b"<!DOCTYPE", b"<!ENTITY")
_HEAD_SCAN_BYTES = 64 * 1024


def _safe_parse(path: str):
    """解析单份报告；遇 DTD / 实体声明或超大文件抛 ValueError，不交给 ET 处理。"""
    size = os.path.getsize(path)
    if size > _MAX_REPORT_BYTES:
        raise ValueError(f"文件异常巨大 {size} B，超上限 {_MAX_REPORT_BYTES} B")
    with open(path, "rb") as fh:
        head = fh.read(_HEAD_SCAN_BYTES).upper()
    for marker in _DTD_MARKERS:
        if marker in head:
            raise ValueError(
                f"检出 {marker.decode()} 声明，拒绝解析（防实体膨胀 / 外部实体注入）")
    return ET.parse(path).getroot()


def _report_bad(bad) -> None:
    for item in bad:
        print(f"UNTRUSTED: {item}", file=sys.stderr)


def collect(report_glob: str, max_age_minutes: float | None = None,
            max_spread_minutes: float | None = None):
    """返回 (红名单集合, 测试总数, 成功解析的报告数, 不可信报告列表)。

    不可信报告 = 解析失败、带 DTD、或时间戳离群的文件。调用方必须据此阻断：
    「读不出来」与「零红」在比对结果上无法区分，静默跳过即假绿。
    """
    reds: set[str] = set()
    classes: set[str] = set()
    total = 0
    files = 0
    bad: list[str] = []
    paths = sorted(glob.glob(report_glob))

    # 时间戳离群检测：先于解析，因为幽灵报告本身可能完全合法、只是不属于本次跑
    now = time.time()
    mtimes = {}
    for path in paths:
        try:
            mtimes[path] = os.path.getmtime(path)
        except OSError:
            pass
    if mtimes:
        newest = max(mtimes.values())
        oldest = min(mtimes.values())
        if max_age_minutes is not None and (now - newest) > max_age_minutes * 60:
            bad.append(f"最新报告 mtime 距今 {(now - newest) / 60:.0f} 分钟，超上限 "
                       f"{max_age_minutes:.0f} 分钟 —— 疑似拿历史报告充当本次结果")
        if max_spread_minutes is not None and (newest - oldest) > max_spread_minutes * 60:
            stale = sorted(p for p, m in mtimes.items()
                           if (newest - m) > max_spread_minutes * 60)
            names = ", ".join(os.path.basename(p) for p in stale[:5])
            bad.append(f"{len(stale)} 份报告 mtime 离群（时间差 {(newest - oldest) / 60:.0f} 分钟 > "
                       f"{max_spread_minutes:.0f}），疑似上次跑残留的幽灵报告：{names}"
                       + (" …" if len(stale) > 5 else ""))

    for path in paths:
        try:
            root = _safe_parse(path)
        except (ET.ParseError, OSError, ValueError) as exc:
            bad.append(f"{path}: {exc}")
            continue
        files += 1
        if root.tag != "testsuite":
            continue
        suite_name = (root.get("name") or "").strip()
        if suite_name:
            # 嵌套类报告名形如 Foo$Bar，归一到顶层类 Foo，才能与源文件 FQN 对账
            classes.add(suite_name.split("$")[0])
        total += int(root.get("tests", "0") or 0)
        for case in root.findall("testcase"):
            is_red = case.find("failure") is not None or case.find("error") is not None
            if not is_red:
                continue
            classname = (case.get("classname") or "").strip()
            method = _normalize(case.get("name"))
            if not classname or not method:
                continue
            reds.add(f"{classname}#{method}")
    return reds, total, files, bad, classes


def _source_test_classes(src_glob: str) -> set[str]:
    """测试源目录下所有 *Test.java 的 FQN。"""
    out: set[str] = set()
    for path in glob.glob(src_glob, recursive=True):
        if SRC_MARKER not in path or not path.endswith(".java"):
            continue
        out.add(path.split(SRC_MARKER, 1)[1][:-len(".java")].replace("/", "."))
    return out


def _emit(reds, total, files, exempt_lines=(), first_seen_map=None) -> str:
    """生成基线文件内容。

    2026-09-08 AM-BASELINE-TTL：每条红后补 `# first_seen=YYYY-MM-DD` 注释，
    首次出现的日期入册（已存在的保留原日期——extract 会从旧基线回填）。
    """
    first_seen_map = first_seen_map or {}
    today = _dt.date.today().isoformat()
    lines = [
        "# IPD 测试红名单基线 —— 由 scripts/ci/ipd-test-red-baseline.py extract 生成，勿手改",
        "# 语义：以下为「已知债务」，CI 不因其失败；出现本清单之外的新红即阻断。",
        "# 修好任意一条后请重跑 extract 覆盖本文件，使基线单调收敛。",
        "# 2026-09-08 AM-BASELINE-TTL：每条红带 first_seen 登记日期；超期条目（默认 14 天）",
        "#   在 check 时 WARN（渐进版不阻断），推动还款——债务可以缓期，不能免息。",
        f"# total_tests={total}",
        f"# report_files={files}",
    ]
    if exempt_lines:
        lines.append("#")
        lines.append("# 下列测试类被显式豁免于「每个测试类都必须真跑」校验（手工登记，extract 不会删除）：")
        lines.extend(exempt_lines)
    for red in sorted(reds):
        date = first_seen_map.get(red, today)
        lines.append(f"{red}  # first_seen={date}")
    return "\n".join(lines) + "\n"


def _load_baseline(path: str):
    """返回 (红名单 dict{条目:首次日期}, total_tests 下限, exempt_class 原始行列表, 豁免 FQN 集合)。

    2026-09-08 AM-BASELINE-TTL：每条红可带可选 `# first_seen=YYYY-MM-DD` 注释——
    旧基线（只有条目名）默认首次出现日期为今天，避免一次性全标今天扰乱逐条历史。
    """
    reds: dict[str, str] = {}
    exempt: set[str] = set()
    exempt_lines: list[str] = []
    total_floor = None
    today = _dt.date.today().isoformat()
    with open(path, encoding="utf-8") as fh:
        for raw in fh:
            line = raw.strip()
            if not line:
                continue
            if line.startswith("#"):
                m = re.match(r"^#\s*total_tests=(\d+)", line)
                if m:
                    total_floor = int(m.group(1))
                m2 = re.match(r"^#\s*exempt_class=(\S+)", line)
                if m2:
                    exempt.add(m2.group(1))
                    exempt_lines.append(line)
                continue
            # 条目行：「FQN#method」 可选后接 `# first_seen=YYYY-MM-DD`
            # 先按空白拆出条目名（避免把条目本身的「classname#method」误拆）
            key = line.split()[0] if line.split() else ""
            if not key:
                continue
            m = _FIRST_SEEN_RE.search(line)
            reds[key] = m.group(1) if m else today
    return reds, total_floor, exempt_lines, exempt


def cmd_extract(args) -> int:
    reds, total, files, bad, classes = collect(args.reports, args.max_age_minutes,
                                               args.max_spread_minutes)
    # 顺序要紧：先判「报告不可信」再判「没报告」。因为 files 只统计解析成功的份数，
    # 报告全损时 files 也为 0，若先报「测试没跑起来」会把「跑了但报告坏了」误判为「没跑」。
    if bad:
        _report_bad(bad)
        print(f"BLOCKED: {len(bad)} 份报告不可信，其红绿无法判定；据此生成的基线"
              "会漏掉真实失败，把债务洗白。请先修掉这些报告再提取。", file=sys.stderr)
        return 1
    if files == 0:
        print(f"BLOCKED: {args.reports} 未匹配到任何 surefire 报告 —— 测试根本没跑起来，"
              "不能据此生成空基线（否则门禁会永久假绿）", file=sys.stderr)
        return 1
    # extract 会覆写整个文件，先读回旧基线里的 exempt_class 手工登记行 + 已有的 first_seen，
    # 避免抹掉豁免台账、避免老条目首次出现日期被重置为今天。
    preserved: list[str] = []
    prior_first_seen: dict[str, str] = {}
    if args.output != "-" and os.path.exists(args.output):
        try:
            _, _, preserved, _ = _load_baseline(args.output)
            prior_first_seen, _, _, _ = _load_baseline(args.output)
        except OSError as exc:
            print(f"WARN: 旧基线读取失败，exempt 登记可能丢失: {exc}", file=sys.stderr)

    # 2026-09-08 AM-BASELINE-TTL：本次 extract 仍红的条目继承旧 first_seen；
    # 已修复（不在 reds 内）的保留在 prior_first_seen 不写出；新进红的填今天。
    today = _dt.date.today().isoformat()
    first_seen_map = {red: prior_first_seen.get(red, today) for red in reds}

    src_classes = _source_test_classes(args.src_glob)
    silent = sorted(src_classes - classes)
    if silent:
        print(f"提示：{len(silent)} 个测试类未产出报告。check 会因此阻断，"
              "请修根因让它真跑；确需豁免则在基线里手写 exempt_class 登记行：",
              file=sys.stderr)
        for c in silent:
            print(f"  SILENT {c}", file=sys.stderr)

    text = _emit(reds, total, files, preserved, first_seen_map)
    if args.output == "-":
        sys.stdout.write(text)
    else:
        os.makedirs(os.path.dirname(args.output) or ".", exist_ok=True)
        with open(args.output, "w", encoding="utf-8") as fh:
            fh.write(text)
        print(f"已写入 {args.output}: {len(reds)} 红 / total_tests={total} / {files} 份报告")
    return 0


def cmd_check(args) -> int:
    reds, total, files, bad, classes = collect(args.reports, args.max_age_minutes,
                                               args.max_spread_minutes)
    baseline, total_floor, _, exempt = _load_baseline(args.baseline)
    baseline_set = set(baseline.keys())  # 2026-09-08 AM-BASELINE-TTL：baseline 现在是 dict

    # 同 extract：先判不可信再判无报告，否则报告全损时会被误报为「测试没跑起来」
    if bad:
        _report_bad(bad)
        print(f"BLOCKED: {len(bad)} 份报告解析失败或被污染，其中的失败无法进入比对 —— "
              "放行等于默认它们全绿。拒绝假绿。", file=sys.stderr)
        return 1

    if files == 0:
        print(f"BLOCKED: {args.reports} 未匹配到任何 surefire 报告 —— 测试没跑起来，"
              "拒绝按「零新增红」放行", file=sys.stderr)
        return 1

    # 「该跑的有没有跑」——仅比对红名单拦不住静默跳过（pom testExcludes / @Tag 不符 / 类名不匹配）
    src_classes = _source_test_classes(args.src_glob)
    if not src_classes and classes:
        print(f"BLOCKED: --src-glob '{args.src_glob}' 未匹配到任何测试源文件，但报告里有 "
              f"{len(classes)} 个类 —— glob 配错会让覆盖校验静默失效，拒绝假绿。",
              file=sys.stderr)
        return 1
    if src_classes:
        silent = sorted(src_classes - classes - exempt)
        if silent:
            print(f"BLOCKED: {len(silent)} 个测试类有源文件却未产出任何 surefire 报告 —— "
                  "它们根本没执行，红名单比对对其完全无感：", file=sys.stderr)
            for c in silent:
                print(f"  SILENT {c}", file=sys.stderr)
            print("\n成因通常是 pom <testExcludes> 豁免、类名不匹配 surefire include、"
                  "或 @Tag 与 <groups> 不符。处置：修根因让它真跑；确需豁免则在基线加 "
                  "`# exempt_class=<FQN> 理由… 登记…` 行 —— 豁免必须显式、带理由、带台账号。",
                  file=sys.stderr)
            return 1
        stale_exempt = sorted(exempt - src_classes)
        if stale_exempt:
            print(f"BLOCKED: {len(stale_exempt)} 条 exempt_class 登记已无对应源文件（陈旧豁免）："
                  f"{', '.join(stale_exempt)} —— 请删除登记行，否则等于给将来的同名类预留后门。",
                  file=sys.stderr)
            return 1
        needless = sorted(exempt & classes)
        if needless:
            print(f"提示：{len(needless)} 条 exempt_class 实际已产出报告，豁免已无必要，"
                  f"请删除登记行保持豁免清单最小：{', '.join(needless)}")

    # 防「套件整体没执行」造成的假绿：测试总数骤降即视为异常
    if total_floor is not None and total < total_floor * args.min_total_ratio:
        print(f"BLOCKED: 测试总数 {total} 低于基线 {total_floor} 的 "
              f"{args.min_total_ratio:.0%}（阈值 {int(total_floor * args.min_total_ratio)}）—— "
              "疑似编译跳过、@Tag 过滤变化或套件中断，不是真的全绿", file=sys.stderr)
        return 1

    new_reds = sorted(reds - baseline_set)
    fixed = sorted(baseline_set - reds)

    print(f"测试总数 {total}（基线 {total_floor}）| 当前红 {len(reds)} | 基线红 {len(baseline)}")

    if fixed:
        print(f"\n已修复 {len(fixed)} 条基线红（请把基线收紧，勿留陈旧条目）：")
        for item in fixed:
            original = baseline.get(item, "?")
            print(f"  FIXED  {item}  # first_seen={original}")
        print(f"\n更新命令：python3 {os.path.relpath(__file__)} extract "
              f"--reports '{args.reports}' --output {args.baseline}")

    # 2026-09-08 AM-BASELINE-TTL：超期条目 WARN（渐进版不阻断）。
    # 债务可以缓期，不能免息——以可看可读的方式推动还款。
    today = _dt.date.today()
    ttl_days = args.ttl_days
    overdue: list[tuple[str, str, int]] = []
    for item in baseline_set & reds:
        try:
            first_seen = _dt.date.fromisoformat(baseline[item])
        except (ValueError, KeyError):
            continue
        days = (today - first_seen).days
        if days >= ttl_days:
            overdue.append((item, baseline[item], days))
    if overdue:
        print(f"\nWARN: {len(overdue)} 条基线红已挂账超过 {ttl_days} 天（渐进版不阻断）：",
              file=sys.stderr)
        for item, first_seen, days in sorted(overdue, key=lambda x: -x[2]):
            print(f"  OVERDUE  {item}  # first_seen={first_seen}  age={days}d",
                  file=sys.stderr)
        print(f"\n处置：尽快修掉、修不掉则更新 first_seen（例：# first_seen=YYYY-MM-DD 重记）"
              "并写明原因。下一阶段门槛会升到阻断。", file=sys.stderr)

    if new_reds:
        print(f"\nBLOCKED: 检出 {len(new_reds)} 条基线之外的新增红：", file=sys.stderr)
        for item in new_reds:
            print(f"  NEW  {item}", file=sys.stderr)
        print("\n处置：修掉它，或经 owner 同意后重跑 extract 把它并入基线（并登记债务卡）。"
              "不得为了让检查变绿而删除/放宽断言。", file=sys.stderr)
        return 1

    if not fixed:
        print("OK: 无新增红，基线未被突破")
    else:
        print("\nOK: 无新增红（另有已修复项待收紧基线，不阻断）")
    return 0


def main() -> int:
    parser = argparse.ArgumentParser(description="IPD 测试红名单基线提取 / 校验")
    parser.add_argument("--reports", default=DEFAULT_REPORT_GLOB,
                        help=f"surefire 报告 glob（默认 {DEFAULT_REPORT_GLOB}）")
    parser.add_argument("--max-age-minutes", type=float, default=120.0,
                        help="最新报告 mtime 距今上限（分钟），超出判为拿历史报告充当本次结果（默认 120）")
    parser.add_argument("--max-spread-minutes", type=float, default=30.0,
                        help="报告 mtime 最大时间差（分钟），超出判为混入上次跑的幽灵残留（默认 30）")
    parser.add_argument("--src-glob", default=DEFAULT_SRC_GLOB,
                        help="测试源文件 glob，用于校验每个测试类都真跑过（默认 %(default)s）")
    sub = parser.add_subparsers(dest="cmd", required=True)

    p_ex = sub.add_parser("extract", help="提取红名单并写基线")
    p_ex.add_argument("--output", default="-", help="输出文件，- 为 stdout")
    p_ex.set_defaults(func=cmd_extract)

    p_ck = sub.add_parser("check", help="与基线比对，新增红则 exit 1")
    p_ck.add_argument("--baseline", required=True, help="基线文件路径")
    p_ck.add_argument("--min-total-ratio", type=float, default=0.9,
                      help="测试总数相对基线的最低比例，低于则判为套件未正常执行（默认 0.9）")
    p_ck.add_argument("--ttl-days", type=int, default=DEFAULT_TTL_DAYS,
                      help=f"AM-BASELINE-TTL 超期阈值（默认 {DEFAULT_TTL_DAYS} 天）——"
                           "check 会 WARN 挂账超期的基线红，不阻断（渐进版）")
    p_ck.set_defaults(func=cmd_check)

    args = parser.parse_args()
    return args.func(args)


if __name__ == "__main__":
    sys.exit(main())
