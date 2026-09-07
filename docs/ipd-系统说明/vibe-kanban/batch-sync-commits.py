#!/usr/bin/env python3
"""
batch-sync-commits.py — R6 update_task 流程系统化·批量 reconcile（2026-09-06）

Wave21-B 扩展（2026-09-06）：
  - --card-mapping=FILE  引入外部 JSON 卡号映射（commit_prefix → card_short → card_title）
                     73 张 plan 未覆盖卡号由此注入；优先级高于 commit message 自动提取
  - --card-filter=PATTERN  正则过滤卡号（例 "^CONSISTENCY-" 仅同步勘误系列）
  - --dry-run  仅打印 what-if，不实际调 manage.py（已存在，本版保留）

扫描 git log 最近 N 条 commit，解析每条 commit message 中的卡号，
调 manage.py set KEY done --note "..." 落到 plan + 看板。

容错:
  - 卡号不在 plan 中 → 静默跳过
  - 卡号已经是 done → 跳过（unchanged）
  - manage.py 报错 → stderr 报告但不中断

用法:
  # 原始 R6 用法（兼容）
  python3 docs/ipd-系统说明/vibe-kanban/batch-sync-commits.py --limit 33 --dry-run

  # Wave21-B：73 张 plan 未覆盖卡号精准 reconcile
  python3 docs/ipd-系统说明/vibe-kanban/batch-sync-commits.py \\
    --card-mapping docs/ipd-系统说明/治理/卡号映射-20260906.json \\
    --card-filter '^(CONSISTENCY|GOVERNANCE|ROOT-R)' \\
    --dry-run
"""
import argparse
import json
import re
import subprocess
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[3]
MANAGE = REPO_ROOT / 'docs/ipd-系统说明/vibe-kanban/manage.py'

# 与 post-commit-update-kanban.cjs 同款的卡号正则（宽松版）
# R6 reconcile（2026-09-06）：提取严格化——自由文本先按 key 形状切 token，
# 再用 manage.py 的 KEY 正则（plan() 的同一道门）fullmatch 归一。
# 目的：掐掉两类伪卡——①CJK 吞并（DOC-09前端仓确认闭环镜像）②看板注记吞并
# （SEC-01done，实为「SEC-01 done」注记）；并修复前缀截断（SEC-REV）与
# 长后缀（ROOT-R2-P0-2-EXT → ROOT-R2）。
TOKEN_REGEX = re.compile(r'[A-Za-z][A-Za-z0-9]*(?:-[A-Za-z0-9]+)*(?:\.\d+)?')


def _load_manage_key():
    """加载 manage.py 的 KEY 正则，保证「可提取 ⇔ plan() 可解析」两处一致。"""
    import importlib.util
    spec = importlib.util.spec_from_file_location('ipd_manage', MANAGE)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module.KEY


MANAGE_KEY = _load_manage_key()


def normalize_key(candidate):
    """候选 token → plan 可解析的 key；不可归一则返回 None。

    规则（按序，取首个命中）：
    ① 非 ASCII 直接拒绝（CJK 吞并伪卡：DOC-09前端仓确认闭环镜像）；
    ② 数字后粘小写注记剥离（SEC-01done→SEC-01；SEC-06b 单字母不受影响）；
    ③ 原样 fullmatch（WAVE21-A-VERIFY / SEC-FIX-FOLLOWUP / P2-3.2 原样保留）；
    ④ 尾段含点号先试「段内截到主版本」（SEC-FIX-HIGH-5.2→SEC-FIX-HIGH-5，
       SEC 分支无小数点），再逐段弹尾 + 段内点号剥离（最长优先）：
       HIGH-5.2-FOLLOWUP→HIGH-5.2（HIGH 分支保留小数点），ROOT-R2-P0-2-EXT→ROOT-R2。
    """
    if not candidate.isascii():
        return None
    tail = re.sub(r'(?<=\d)[a-z]{2,}$', '', candidate)
    if tail != candidate and MANAGE_KEY.fullmatch(tail):
        return tail
    if MANAGE_KEY.fullmatch(candidate):
        return candidate
    segs = candidate.split('-')
    while len(segs) > 1:
        last = segs[-1]
        dot_pos = last.find('.')
        if dot_pos > 0:
            trial = '-'.join(segs[:-1] + [last[:dot_pos]])
            if MANAGE_KEY.fullmatch(trial):
                return trial
        segs.pop()
        joined = '-'.join(segs)
        for trial in (joined,
                      re.sub(r'\.\d+(?=-|$)', '', joined),
                      re.sub(r'\.\d+', '', joined)):
            if MANAGE_KEY.fullmatch(trial):
                return trial
    return None

# 已知 inreview 卡（兄弟流同步活动卡）—— 跳过避免覆盖
SKIP_INREVIEW = set()  # 由 --skip-inreview-file 注入


def git_log(limit):
    # 用 |DELIM| 作为占位符（commit message 中不出现），避免 %x00 不被 git 解释
    out = subprocess.run(
        ['git', 'log', f'-{limit}', '--pretty=%H|DELIM|%s|DELIM|%b'],
        cwd=REPO_ROOT, capture_output=True, text=True, encoding='utf-8', check=True,
    )
    commits = []
    for line in out.stdout.split('\n'):
        if not line.strip():
            continue
        parts = line.split('|DELIM|', 2)
        if len(parts) >= 2:
            sha = parts[0].strip()
            subject = parts[1].strip()
            body = parts[2].strip() if len(parts) > 2 else ''
            commits.append({'sha': sha, 'subject': subject, 'body': body})
    return commits


def extract_keys(text):
    keys = []
    for token in TOKEN_REGEX.findall(text):
        key = normalize_key(token)
        if key and key not in keys:
            keys.append(key)
    return keys


def build_note(subject):
    s = subject.replace('|', '-').replace('\n', ' ').strip()
    if len(s) > 80:
        s = s[:77] + '...'
    return s or 'auto-reconciled'


def run_manage(key, status, note, dry_run=False):
    if dry_run:
        return {'key': key, 'ok': True, 'action': 'dry-run', 'note': note}
    try:
        result = subprocess.run(
            ['python3', str(MANAGE), 'set', key, status, '--note', note],
            cwd=REPO_ROOT, capture_output=True, text=True, encoding='utf-8', timeout=15,
        )
        if result.returncode == 0:
            try:
                parsed = json.loads(result.stdout)
                actions = parsed.get('actions', [])
                did = next((a for a in actions if a['key'] == key), None)
                return {'key': key, 'ok': True, 'action': did['action'] if did else 'unknown'}
            except json.JSONDecodeError:
                return {'key': key, 'ok': True, 'action': 'unknown'}
        else:
            err = result.stderr or result.stdout
            if 'No task with source ID' in err:
                return {'key': key, 'ok': False, 'reason': 'not-in-plan'}
            return {'key': key, 'ok': False, 'reason': 'manage-error', 'stderr': err.strip()[:200]}
    except subprocess.TimeoutExpired:
        return {'key': key, 'ok': False, 'reason': 'timeout'}
    except Exception as e:
        return {'key': key, 'ok': False, 'reason': f'exception:{e}'}


def load_card_mapping(path):
    """加载外部卡号映射 JSON；返回 dict: commit_prefix → mapping entry.

    期望 JSON 形如:
      {"mappings": [{"commit_prefix": "02258e48", "card_short": "SEC-FIX-HIGH-1.1",
                     "card_title": "...", "owner_status": "..."}, ...]}
    """
    p = Path(path)
    if not p.is_absolute():
        p = REPO_ROOT / path
    if not p.exists():
        raise FileNotFoundError(f'card mapping file not found: {p}')
    raw = json.loads(p.read_text(encoding='utf-8'))
    entries = raw.get('mappings', [])
    index = {}
    for e in entries:
        cp = (e.get('commit_prefix') or '').strip()
        cs = (e.get('card_short') or '').strip()
        if not cp or not cs:
            continue
        index[cp] = {
            'card_short': cs,
            'card_title': e.get('card_title', ''),
            'owner_status': e.get('owner_status', ''),
        }
    return {'index': index, 'total': len(entries), 'source_file': str(p), '_meta': raw.get('_meta', {})}


def iter_commits_with_mapping(commits, mapping_index, card_filter_re=None):
    """产出 (commit, mapping_entry or None) 二元组，按 commit 顺序遍历。

    card_filter_re: 若非 None，仅返回 card_short 匹配该正则的项。
    """
    for c in commits:
        short_sha = c['sha'][:7]
        entry = mapping_index.get(short_sha)
        if entry is None:
            continue
        if card_filter_re and not card_filter_re.search(entry['card_short']):
            continue
        yield c, entry


def main():
    parser = argparse.ArgumentParser(
        description='批量 reconcile commits to vibe-kanban (Wave21-B 卡号映射扩展)',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog=__doc__,
    )
    parser.add_argument('--limit', type=int, default=33, help='扫描最近 N 条 commit')
    parser.add_argument('--status', default='done', help='设置状态（默认 done）')
    parser.add_argument('--dry-run', action='store_true', help='仅打印 what-if，不实际调 manage.py')
    parser.add_argument('--card-mapping', default=None,
                        help='外部卡号映射 JSON 路径（73 张 plan 未覆盖卡号用）')
    parser.add_argument('--card-filter', default=None,
                        help='正则过滤 card_short（如 "^(CONSISTENCY|GOVERNANCE|ROOT-R)"）')
    args = parser.parse_args()

    # 解析卡号过滤正则
    card_filter_re = None
    if args.card_filter:
        try:
            card_filter_re = re.compile(args.card_filter)
        except re.error as e:
            print(f'[batch-sync-commits] invalid --card-filter regex: {e}', file=sys.stderr)
            sys.exit(2)

    # 加载卡号映射（如提供）
    mapping = None
    if args.card_mapping:
        try:
            mapping = load_card_mapping(args.card_mapping)
        except (FileNotFoundError, json.JSONDecodeError) as e:
            print(f'[batch-sync-commits] failed to load card mapping: {e}', file=sys.stderr)
            sys.exit(2)
        print(
            f"[batch-sync-commits] loaded {mapping['total']} card mappings from "
            f"{mapping['source_file']}",
            file=sys.stderr,
        )

    print(f'[batch-sync-commits] scanning last {args.limit} commits at {REPO_ROOT}', file=sys.stderr)
    commits = git_log(args.limit)
    print(f'[batch-sync-commits] found {len(commits)} commits', file=sys.stderr)

    summary = {
        'total_commits': len(commits),
        'mode': 'card-mapping' if mapping else 'commit-message',
        'card_mapping_total': mapping['total'] if mapping else 0,
        'card_filter': args.card_filter,
        'updated': [], 'skipped': [], 'errors': [],
    }
    seen_keys = set()  # 去重：每张卡只动一次

    if mapping:
        # ====== Wave21-B 模式：外部 JSON 卡号映射 ======
        matched = 0
        for c, entry in iter_commits_with_mapping(commits, mapping['index'], card_filter_re):
            matched += 1
            key = entry['card_short']
            if key in seen_keys:
                continue
            seen_keys.add(key)
            note = f"{entry['card_short']} | {entry['card_title'][:60]}" if entry['card_title'] else build_note(c['subject'])
            result = run_manage(key, args.status, note, dry_run=args.dry_run)
            result['sha'] = c['sha'][:7]
            result['card_title'] = entry['card_title']
            result['owner_status'] = entry['owner_status']
            result['note'] = note
            result['mode'] = 'mapping'
            if result['ok']:
                summary['updated'].append(result)
                print(f"  ✓ {key} ← {c['sha'][:7]} ({result['action']}) [{entry['card_title'][:30]}]", file=sys.stderr)
            elif result.get('reason') == 'not-in-plan':
                summary['skipped'].append(result)
                print(f"  - {key} ← {c['sha'][:7]} (not-in-plan) [{entry['card_title'][:30]}]", file=sys.stderr)
            else:
                summary['errors'].append(result)
                print(f"  ✗ {key} ← {c['sha'][:7]} ({result.get('reason')})", file=sys.stderr)
        summary['commits_matched'] = matched
    else:
        # ====== 原始 R6 模式：commit message 正则提取 ======
        filtered_out = 0
        for c in commits:
            message = c['subject'] + '\n' + c['body']
            keys = extract_keys(message)
            if not keys:
                continue
            note = build_note(c['subject'])

            for key in keys:
                if args.card_filter and not card_filter_re.search(key):
                    filtered_out += 1
                    continue
                if key in seen_keys:
                    continue
                seen_keys.add(key)
                result = run_manage(key, args.status, note, dry_run=args.dry_run)
                result['sha'] = c['sha'][:7]
                result['note'] = note
                if result['ok']:
                    summary['updated'].append(result)
                    print(f"  ✓ {key} ← {c['sha'][:7]} ({result['action']})", file=sys.stderr)
                elif result.get('reason') == 'not-in-plan':
                    summary['skipped'].append(result)
                    print(f"  - {key} ← {c['sha'][:7]} (not-in-plan)", file=sys.stderr)
                else:
                    summary['errors'].append(result)
                    print(f"  ✗ {key} ← {c['sha'][:7]} ({result.get('reason')})", file=sys.stderr)

    summary['unique_keys_seen'] = len(seen_keys)
    if not mapping and args.card_filter:
        summary['filtered_out'] = filtered_out
    summary['updated_count'] = len(summary['updated'])
    summary['skipped_count'] = len(summary['skipped'])
    summary['error_count'] = len(summary['errors'])
    print(
        f"\n[batch-sync-commits] DONE: updated={summary['updated_count']} "
        f"skipped(not-in-plan)={summary['skipped_count']} errors={summary['error_count']}",
        file=sys.stderr,
    )
    print(json.dumps(summary, ensure_ascii=False, indent=2))


if __name__ == '__main__':
    main()
