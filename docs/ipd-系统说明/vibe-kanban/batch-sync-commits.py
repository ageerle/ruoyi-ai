#!/usr/bin/env python3
"""
batch-sync-commits.py — R6 update_task 流程系统化·批量 reconcile（2026-09-06）

扫描 git log 最近 N 条 commit，解析每条 commit message 中的卡号，
调 manage.py set KEY done --note "..." 落到 plan + 看板。

容错:
  - 卡号不在 plan 中 → 静默跳过
  - 卡号已经是 done → 跳过（unchanged）
  - manage.py 报错 → stderr 报告但不中断

用法:
  python3 docs/ipd-系统说明/vibe-kanban/batch-sync-commits.py [--limit 33] [--dry-run]
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
CARD_REGEX = re.compile(
    r'\b(?:'
    r'P[0-4]-\d+(?:\.\d+)?'
    r'|HIGH-\d+(?:\.\d+)?(?:-\w+)?'
    r'|MEDIUM-\d+(?:\.\d+)?(?:-\w+)?'
    r'|LOW-\d+(?:\.\d+)?'
    r'|SEC-[A-Z]+-[\w.-]+'
    r'|SEC-[\w-]+'
    r'|ROOT-R\d+(?:-[\w-]+)?'
    r'|FIX-[\w-]+'
    r'|GOVERNANCE-\d+'
    r'|CONSISTENCY-\d+'
    r'|DOC-[\w-]+'
    r'|REFLECTION-\d+'
    r'|DDL-[\w-]+'
    r'|GUARD-\d+'
    r'|WAVE[\w-]+'
    r'|R\d+'
    r'|AUD(?:-\w+)?-\d+'
    r'|API-\d+'
    r'|OPS(?:-\w+)?-\d+'
    r'|QA-\d+'
    r'|RISK-\d+'
    r'|DB-\d+'
    r'|DEF-\d+'
    r')\b'
)

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
    return list(dict.fromkeys(CARD_REGEX.findall(text)))


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


def main():
    parser = argparse.ArgumentParser(description='批量 reconcile commits to vibe-kanban')
    parser.add_argument('--limit', type=int, default=33, help='扫描最近 N 条 commit')
    parser.add_argument('--status', default='done', help='设置状态（默认 done）')
    parser.add_argument('--dry-run', action='store_true', help='仅打印不修改')
    args = parser.parse_args()

    print(f'[batch-sync-commits] scanning last {args.limit} commits at {REPO_ROOT}', file=sys.stderr)
    commits = git_log(args.limit)
    print(f'[batch-sync-commits] found {len(commits)} commits', file=sys.stderr)

    summary = {'total_commits': len(commits), 'updated': [], 'skipped': [], 'errors': []}
    seen_keys = set()  # 去重：每张卡只动一次

    for c in commits:
        message = c['subject'] + '\n' + c['body']
        keys = extract_keys(message)
        if not keys:
            continue
        note = build_note(c['subject'])

        for key in keys:
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
