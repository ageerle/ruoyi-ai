#!/usr/bin/env python3
"""
镜像 vs 看板 diff 校验脚本（病根 3 根治）

功能：
1. 解析 SSOT 镜像主表所有 P*-* 卡 status（从「| 卡号 | 标题 | ... | 状态 |」行）
2. fresh 拉看板所有 P*-* 卡 status
3. diff 输出不一致清单（镜像 done 但看板 todo / 镜像 todo 但看板 done 等）

用法：
  python3 scripts/check-mirror-vs-board.py [--json out.json] [--verbose]

边界：只读探针，不改任何代码 / 不 commit / 不 push。
"""

import json
import re
import sys
import urllib.request
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent
BOARD = "http://127.0.0.1:62250"
PID = "01dcf15c-86bb-4c7b-957c-8fe44bddd10d"
MIRROR = REPO_ROOT / "docs/ipd-系统说明/开发计划-看板镜像.md"

# 镜像主表状态符号映射
STATUS_SYMBOLS = {
    "✅": "done",
    "⬜": "todo",
    "◇": "inreview",
    "▶": "inprogress",
    "✗": "cancelled",
    "✓": "done",
}


def parse_mirror_status():
    """解析镜像主表所有 P*-* 卡 status"""
    if not MIRROR.exists():
        return {"error": "镜像文件不存在"}

    content = MIRROR.read_text(encoding="utf-8", errors="ignore")
    lines = content.split("\n")

    cards = {}
    # 匹配主表行：| P*-* | 标题 | ... | 状态符号 | ...
    # 主表通常在 line 50-200 段
    pattern = re.compile(r"^\|\s*(P\d+-\d+(?:\.\d+)?)\s*\|")

    for i, line in enumerate(lines, 1):
        m = pattern.match(line)
        if not m:
            continue
        card_id = m.group(1)
        # 找状态符号（✅ / ⬜ / ◇ / ▶ / ✗ / ✓）
        status = None
        for symbol, s in STATUS_SYMBOLS.items():
            if symbol in line:
                status = s
                break
        if status:
            cards[card_id] = {
                "line": i,
                "status": status,
                "raw": line.strip()[:200],
            }

    return {"count": len(cards), "cards": cards}


def fresh_board_status():
    """fresh 拉看板所有 P*-* 卡 status"""
    try:
        data = json.loads(
            urllib.request.urlopen(f"{BOARD}/api/tasks?project_id={PID}", timeout=10).read()
        )["data"]
    except Exception as e:
        return {"error": f"看板不可达: {e}", "cards": {}}

    cards = {}
    for t in data:
        # 看板卡 id 字段可能是 UUID，ref 字段可能是 P*-*
        # 或 title 字段含 [P*-*]
        card_id = None
        for field in ["ref", "id", "title"]:
            val = t.get(field) or ""
            m = re.search(r"(P\d+-\d+(?:\.\d+)?)", str(val))
            if m:
                card_id = m.group(1)
                break
        if card_id:
            cards[card_id] = {
                "status": t.get("status", "?"),
                "title": (t.get("title") or "")[:80],
                "uuid": t.get("id", "?"),
            }

    return {"count": len(cards), "cards": cards}


def diff_mirror_vs_board(mirror, board):
    """diff 输出不一致清单"""
    if "error" in mirror or "error" in board:
        return {"error": mirror.get("error") or board.get("error")}

    mirror_cards = mirror["cards"]
    board_cards = board["cards"]

    inconsistencies = []
    only_in_mirror = []
    only_in_board = []

    all_ids = set(mirror_cards.keys()) | set(board_cards.keys())
    for card_id in sorted(all_ids):
        in_mirror = card_id in mirror_cards
        in_board = card_id in board_cards

        if in_mirror and not in_board:
            only_in_mirror.append({
                "card_id": card_id,
                "mirror_status": mirror_cards[card_id]["status"],
                "mirror_line": mirror_cards[card_id]["line"],
            })
        elif in_board and not in_mirror:
            only_in_board.append({
                "card_id": card_id,
                "board_status": board_cards[card_id]["status"],
                "board_title": board_cards[card_id]["title"],
            })
        else:
            m_status = mirror_cards[card_id]["status"]
            b_status = board_cards[card_id]["status"]
            if m_status != b_status:
                inconsistencies.append({
                    "card_id": card_id,
                    "mirror_status": m_status,
                    "board_status": b_status,
                    "mirror_line": mirror_cards[card_id]["line"],
                    "board_title": board_cards[card_id]["title"],
                })

    return {
        "inconsistency_count": len(inconsistencies),
        "only_in_mirror_count": len(only_in_mirror),
        "only_in_board_count": len(only_in_board),
        "inconsistencies": inconsistencies,
        "only_in_mirror": only_in_mirror[:20],
        "only_in_board": only_in_board[:20],
    }


def main():
    verbose = "--verbose" in sys.argv
    json_out = None
    if "--json" in sys.argv:
        idx = sys.argv.index("--json")
        if idx + 1 < len(sys.argv):
            json_out = sys.argv[idx + 1]

    print("=" * 70)
    print("镜像 vs 看板 diff 校验（病根 3 根治）")
    print("=" * 70)
    print()

    print("[1/3] 解析镜像主表所有 P*-* 卡 status...")
    mirror = parse_mirror_status()
    if "error" in mirror:
        print(f"  ✗ {mirror['error']}")
        sys.exit(1)
    print(f"  镜像卡数: {mirror['count']}")
    print()

    print("[2/3] fresh 拉看板所有 P*-* 卡 status...")
    board = fresh_board_status()
    if "error" in board:
        print(f"  ✗ {board['error']}")
        sys.exit(1)
    print(f"  看板卡数: {board['count']}")
    print()

    print("[3/3] diff 输出不一致清单...")
    diff = diff_mirror_vs_board(mirror, board)
    if "error" in diff:
        print(f"  ✗ {diff['error']}")
        sys.exit(1)

    print(f"  不一致卡数:     {diff['inconsistency_count']}")
    print(f"  仅镜像有:       {diff['only_in_mirror_count']}")
    print(f"  仅看板有:       {diff['only_in_board_count']}")
    print()

    if diff["inconsistencies"]:
        print("--- 不一致清单（镜像 vs 看板）---")
        for d in diff["inconsistencies"][:30]:
            print(f"  {d['card_id']:12} 镜像={d['mirror_status']:10} 看板={d['board_status']:10} line={d['mirror_line']}")
            if verbose:
                print(f"    看板标题: {d['board_title']}")
        print()

    if verbose and diff["only_in_mirror"]:
        print("--- 仅镜像有（看板缺失）---")
        for d in diff["only_in_mirror"][:10]:
            print(f"  {d['card_id']:12} 镜像={d['mirror_status']:10} line={d['mirror_line']}")
        print()

    if verbose and diff["only_in_board"]:
        print("--- 仅看板有（镜像缺失）---")
        for d in diff["only_in_board"][:10]:
            print(f"  {d['card_id']:12} 看板={d['board_status']:10} title={d['board_title']}")
        print()

    report = {
        "mirror": mirror,
        "board": board,
        "diff": diff,
    }

    if json_out:
        Path(json_out).write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
        print(f"JSON 报告已写入: {json_out}")

    print("=" * 70)
    if diff["inconsistency_count"] > 0:
        print(f"⚠️  发现 {diff['inconsistency_count']} 张卡镜像 vs 看板 status 不一致！")
        print("   根治：PUT 看板后强制 GET 复核 + 翻 done 硬门禁")
    else:
        print("✓ 镜像 vs 看板 status 一致")
    print("=" * 70)


if __name__ == "__main__":
    main()
