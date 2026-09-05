#!/usr/bin/env python3
"""Reconcile the repository plan with its isolated local Vibe Kanban board."""
import argparse
from collections import Counter
import hashlib
import json
from pathlib import Path
import re
import urllib.request

ROOT = Path(__file__).resolve().parents[3]
PLAN = ROOT / 'docs/ipd-系统说明/开发计划-看板镜像.md'
STATE = ROOT / '.codex/vibe-kanban'
BASE = 'http://127.0.0.1:62250'
PROJECT = 'ruoyi-ai'
STATES = {'todo': '⬜', 'inprogress': '▶', 'inreview': '◇', 'done': '✅', 'cancelled': '⊘'}
KEY = re.compile(r'^(?:P[0-4]-\d+(?:\.\d+)?|(?:OPS-VK|AUD|DOC|SEC(?:-API)?|DATA|API|OPS|QA|RISK|DB)-\d+(?:\.\d+)?)$')
PRIORITIES = {'U0': '紧急', 'U1': '高', 'U2': '中', 'U3': '后续', 'P1': 'P1', 'P2': 'P2', 'P3': 'P3', '汇总': '汇总'}


def has_external_blocker(source_status):
    # History is evidence, not the current execution decision. A resolved
    # blocker retained after "前态记录" must not keep a card unclaimable.
    current = source_status.split('；前态记录：', 1)[0]
    return bool(re.search(r'BLOCKED_(?:ENVIRONMENT|DEPENDENCY|PERMISSION)|等待owner|待业务裁决', current))


class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        raise RuntimeError('Local board unexpectedly redirected; refusing to follow')


def api(path, method='GET', payload=None):
    data = None if payload is None else json.dumps(payload, ensure_ascii=False).encode()
    request = urllib.request.Request(BASE + path, data=data, method=method,
                                     headers={'Content-Type': 'application/json'})
    with urllib.request.build_opener(urllib.request.ProxyHandler({}), NoRedirect).open(request, timeout=15) as response:
        if 'application/json' not in response.headers.get('Content-Type', ''):
            raise RuntimeError(f'{path}: expected JSON, not an HTML fallback')
        result = json.load(response)
    if result.get('success') is not True:
        raise RuntimeError(f'{path}: {result.get("message", result)}')
    return result['data']


def plan():
    rows = []
    for line_index, line in enumerate(PLAN.read_text().splitlines()):
        parts = [part.strip() for part in line.strip().strip('|').split('|')]
        if not parts or not KEY.fullmatch(parts[0]):
            continue
        if len(parts) == 3:
            # Three historical rows omitted the pipe before the completion cell.
            match = re.search(r'\s+(✅|▶|⬜|◐|◇|⊘)', parts[2])
            if not match:
                raise ValueError(f'Plan row {parts[0]} has no status cell')
            parts = parts[:2] + [parts[2][:match.start()].strip(), parts[2][match.start():].strip()]
        if len(parts) not in (4, 10):
            raise ValueError(f'Plan row {parts[0]} has {len(parts)} cells')
        key, title, acceptance, source_status = parts[:4]
        priority, dependencies, owner, paths, validation, source = parts[4:] if len(parts) == 10 else ('汇总', '—', '待认领', '—', '—', '历史计划')
        if priority not in PRIORITIES:
            raise ValueError(f'Unknown priority in {key}: {priority}')
        state = next((name for name, mark in STATES.items() if source_status.startswith(mark)), None)
        if state is None and source_status.startswith('◐'):
            state = 'todo'  # Partial/deferred is not an active execution claim.
        if state is None:
            raise ValueError(f'Unknown status in {key}: {source_status}')
        rows.append(dict(key=key, title=title, acceptance=acceptance,
                         source_status=source_status, status=state, line=line_index,
                         priority=priority, dependencies=dependencies, owner=owner,
                         allowedPaths=paths, validation=validation, source=source, extended=len(parts) == 10))
    if len({row['key'] for row in rows}) != len(rows) or not rows:
        raise ValueError('Empty plan or duplicate source IDs')
    keys = {row['key'] for row in rows}
    graph = {row['key']: re.findall(r'(?:P[0-4]-\d+(?:\.\d+)?|(?:OPS-VK|AUD|DOC|SEC|DATA|API|OPS|QA)-\d+(?:\.\d+)?)', row['dependencies']) for row in rows}
    def visit(key, trail):
        if key in trail:
            raise ValueError('Cyclic task dependency: ' + ' -> '.join(trail + [key]))
        for dep in graph[key]:
            if dep not in keys:
                raise ValueError(f'{key}: missing dependency {dep}')
            visit(dep, trail + [key])
    for key in keys:
        visit(key, [])
    return rows


def get_project(create=False):
    matches = [p for p in api('/api/projects') if p['name'] == PROJECT]
    if len(matches) > 1:
        raise RuntimeError('Duplicate project names; resolve identity before continuing')
    if not matches:
        if not create:
            return None
        matches = [api('/api/projects', 'POST', {
            'name': PROJECT, 'repositories': [{'display_name': 'ruoyi-ai', 'git_repo_path': str(ROOT)}]})]
    project = matches[0]
    if project.get('remote_project_id'):
        raise RuntimeError('Project is linked to a remote board; local-only mode required')
    repos = api(f'/api/projects/{project["id"]}/repositories')
    if len(repos) != 1 or repos[0]['path'] != str(ROOT):
        raise RuntimeError('Project repository does not match the canonical checkout')
    return project


def description(row):
    key = row['key']
    return (f'优先级：{row["priority"]} {PRIORITIES[row["priority"]]}\n'
            f'依赖：{row["dependencies"]}\n责任泳道：{row["owner"]}\n认领：以源状态及卡后人工备注为准；未明确认领则待认领。\n\n'
            f'allowedPaths：{row["allowedPaths"]}\n\n'
            f'验收要点：{row["acceptance"]}\n\n'
            f'验证命令/步骤：{row["validation"]}\n'
            '命令中的新增测试名/脚本为该卡交付要求，未实现前不得当作现成通过证据；命令不存在、零测试或仅Mock均不得证明真实业务验收。\n\n'
            f'需求/缺口依据：{row["source"]}\n\n'
            f'源状态与证据：{row["source_status"]}\n\n'
            f'计划编号：{key}\n来源：docs/ipd-系统说明/开发计划-看板镜像.md\n\n'
            '此处同步源文档记录；历史已完成项不代表本次重新验收。\n'
            '执行者在当前宿主机项目中工作，完成前补充真实验证证据并同步源文档。\n'
            f'<!-- ruoyi-plan:{key}:begin -->\n'
            f'<!-- ruoyi-plan:{key}:end -->')


def reconcile(apply=False):
    rows = plan()
    source_hash = hashlib.sha256(PLAN.read_bytes()).hexdigest()
    project = get_project()
    mapping_file = STATE / 'mapping.json'
    if apply and project and mapping_file.exists():
        previous = json.loads(mapping_file.read_text())
        if previous.get('project_id') == project['id']:
            omitted = sorted(set(previous['task_ids']) - {row['key'] for row in rows})
            if omitted:
                raise RuntimeError(
                    f'Plan omits {len(omitted)} previously mapped IDs '
                    f'({", ".join(omitted[:10])}); refusing all sync writes. '
                    'Restore or explicitly reconcile the plan before retrying.')
    if project is None and apply:
        project = get_project(create=True)
    tasks = api(f'/api/tasks?project_id={project["id"]}') if project else []
    actions, mapping = [], {}
    for row in rows:
        key = row['key']
        matches = [t for t in tasks if t['title'].startswith(f'[{key}] ')]
        if len(matches) > 1:
            raise RuntimeError(f'Duplicate board cards for {key}')
        priority_label = '汇总' if row['priority'] == '汇总' else f'{row["priority"]} {PRIORITIES[row["priority"]]}'
        title = f'[{key}] [{priority_label}] {row["title"]}'
        desc = description(row)
        task = matches[0] if matches else None
        if task:
            existing = task.get('description') or ''
            block = rf'^.*?<!-- ruoyi-plan:{re.escape(key)}:begin -->.*?<!-- ruoyi-plan:{re.escape(key)}:end -->'
            if not re.search(block, existing, re.S):
                raise RuntimeError(f'{key}: same-title unmanaged card; refusing to overwrite')
            desc = re.sub(block, lambda _: desc, existing, flags=re.S)
        desired = dict(title=title, description=desc, status=row['status'])
        action = 'create' if task is None else ('update' if any(task.get(k) != v for k, v in desired.items()) else 'unchanged')
        if apply and action != 'unchanged':
            if hashlib.sha256(PLAN.read_bytes()).hexdigest() != source_hash:
                raise RuntimeError('Plan changed during synchronization; rerun from the current plan')
            if task is None:
                task = api('/api/tasks', 'POST', {**desired, 'project_id': project['id']})
                # Some versions create all tasks in todo. Set and verify the desired state explicitly.
                if task['status'] != row['status']:
                    task = api(f'/api/tasks/{task["id"]}', 'PUT', desired)
            else:
                task = api(f'/api/tasks/{task["id"]}', 'PUT', desired)
            actual = api(f'/api/tasks/{task["id"]}')
            if any(actual.get(k) != v for k, v in desired.items()):
                raise RuntimeError(f'{key}: read-back verification failed')
        if task:
            mapping[key] = task['id']
        actions.append({'key': key, 'action': action, 'status': row['status']})
    # Same-project cards outside the SSOT must remain visible to the check.
    # Do not delete or adopt them implicitly: retain their identity for review.
    mapped_ids = set(mapping.values())
    unmanaged_cards = [{k: task.get(k) for k in ('id', 'title', 'status')}
                       for task in tasks if task['id'] not in mapped_ids]
    result = {'project_id': project['id'] if project else None, 'source_sha256': source_hash,
              'total': len(rows), 'counts': dict(Counter(a['action'] for a in actions)),
              'statuses': dict(Counter(r['status'] for r in rows)), 'actions': actions,
              'board_total': len(tasks),  # Actual API snapshot, before any sync writes.
              'unmanaged_cards': unmanaged_cards,
              'has_drift': bool(unmanaged_cards) or any(a['action'] != 'unchanged' for a in actions)}
    if apply:
        STATE.mkdir(parents=True, exist_ok=True)
        mapping_file.write_text(json.dumps({**result, 'task_ids': mapping}, ensure_ascii=False, indent=2) + '\n')
    return result


def set_status(key, status, note):
    rows = plan()
    row = next((r for r in rows if r['key'] == key), None)
    if not row:
        raise ValueError(f'No task with source ID {key}')
    if not note.strip() or '\n' in note or '|' in note:
        raise ValueError('A single-line owner/blocker/evidence note without a pipe is required')
    if get_project() is None:
        raise RuntimeError('Initialize the local project with sync --apply first')
    if row['source_status'].startswith(f'{STATES[status]} {note}；'):
        return reconcile(apply=True)
    original = PLAN.read_text()
    lines = original.splitlines()
    prior = row['source_status']
    source_status = f'{STATES[status]} {note}；前态记录：{prior}'
    cells = [key, row['title'], row['acceptance'], source_status]
    if row['extended']:
        cells += [row['priority'], row['dependencies'], row['owner'], row['allowedPaths'], row['validation'], row['source']]
    lines[row['line']] = '| ' + ' | '.join(cells) + ' |'
    STATE.mkdir(parents=True, exist_ok=True)
    backup = STATE / ('plan-before-' + hashlib.sha256(original.encode()).hexdigest()[:12] + '.md')
    if not backup.exists():
        backup.write_text(original)
    PLAN.write_text('\n'.join(lines) + '\n')
    # The Markdown remains authoritative if the API fails; retry sync after recovery.
    return reconcile(apply=True)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    sub = parser.add_subparsers(dest='command', required=True)
    sub.add_parser('plan')
    sub.add_parser('check')
    sub.add_parser('list')
    queue_cmd = sub.add_parser('queue')
    queue_cmd.add_argument('--limit', type=int, default=20)
    queue_cmd.add_argument('--ready', action='store_true', help='Only tasks without unfinished or external dependencies')
    sync = sub.add_parser('sync')
    sync.add_argument('--apply', action='store_true')
    set_cmd = sub.add_parser('set')
    set_cmd.add_argument('key')
    set_cmd.add_argument('status', choices=STATES)
    set_cmd.add_argument('--note', required=True)
    args = parser.parse_args()
    if args.command == 'plan':
        result = plan()
    elif args.command == 'queue':
        rows = plan()
        completed = {r['key'] for r in rows if r['status'] == 'done'}
        queue = []
        for r in rows:
            if r['priority'] == '汇总' or r['status'] in ('done', 'cancelled'):
                continue
            deps = [d for d in re.findall(KEY.pattern[1:-1], r['dependencies']) if d not in completed]
            external = has_external_blocker(r['source_status'])
            queue.append({k: r[k] for k in ('key', 'priority', 'title', 'status', 'owner', 'line')}
                         | {'blocked_by': deps, 'external_blocker': r['source_status'] if external else None})
        queue.sort(key=lambda r: (r['priority'], bool(r['blocked_by'] or r['external_blocker']), r['line']))
        if args.ready:
            queue = [r for r in queue if not r['blocked_by'] and not r['external_blocker'] and r['status'] == 'todo']
        result = queue[:max(0, args.limit)]
    elif args.command == 'list':
        p = get_project()
        result = api(f'/api/tasks?project_id={p["id"]}') if p else []
    elif args.command == 'set':
        result = set_status(args.key, args.status, args.note)
    else:
        result = reconcile(apply=args.command == 'sync' and args.apply)
    print(json.dumps(result, ensure_ascii=False, indent=2))
    if args.command == 'check' and result['has_drift']:
        raise SystemExit(1)
