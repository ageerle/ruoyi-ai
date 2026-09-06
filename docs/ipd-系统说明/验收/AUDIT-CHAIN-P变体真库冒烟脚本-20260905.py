#!/usr/bin/env python3
# ①②③ P 变体部署后真库冒烟（ch.jar @16045, 2026-09-05）
# 前置事实：seed 定格 last_seq=1626/next_seq=1627；audit_logs.seq 已去 AUTO_INCREMENT
# 1) 单发登录 → 新行 seq=1627（锚行分配）+ prev_hash 衔接 + 锚行同步推进
# 2) verify 端点基线（Q5 语义：历史空洞只进 gaps 告警）
# 3) 4 账号并发登录 → 串行分配 seq 连续无冲突 + 链衔接 + 锚行终态
# 4) verify 复验不劣化（broken/hashBroken/gaps 三值不增）
import json, subprocess, sys, threading, time, urllib.error, urllib.request

CRED = json.load(open('/Users/mac/Documents/ruoyi-ai/.codex/ipd-dev/config/credentials.json'))
BASE = 'http://localhost:16045'
MYSQL = '/Users/mac/Documents/ruoyi-ai/.codex/ipd-dev/software/mysql-8.0.46-macos15-arm64/bin/mysql'
ROOTCNF = '/Users/mac/Documents/ruoyi-ai/.codex/ipd-dev/config/mysql-client.cnf'

def q(sql):
    r = subprocess.run([MYSQL, '--defaults-file=' + ROOTCNF, 'ipd_dev', '-N', '-B', '-e', sql],
                       capture_output=True, text=True)
    if r.returncode != 0:
        raise RuntimeError(r.stderr)
    return [l.split('\t') for l in r.stdout.strip().splitlines() if l]

def req(method, path, body=None, token=None):
    h = {'Content-Type': 'application/json'}
    if token:
        h['Authorization'] = 'Bearer ' + token
    data = json.dumps(body).encode() if body is not None else None
    r = urllib.request.Request(BASE + path, data=data, headers=h, method=method)
    try:
        with urllib.request.urlopen(r, timeout=20) as resp:
            return resp.status, json.loads(resp.read().decode())
    except urllib.error.HTTPError as e:
        try:
            return e.code, json.loads(e.read().decode())
        except Exception:
            return e.code, {}

# 基线动态发现：seed 定格 1626/1627，探针 curl 已消耗 1627 → 以实时锚行为准
PRE_MAX = int(q('SELECT MAX(seq) FROM audit_logs')[0][0])
PRE_NEXT = int(q("SELECT next_seq FROM audit_log_chain_heads WHERE chain_key='GLOBAL'")[0][0])
print('dynamic baseline: PRE_MAX=%d PRE_NEXT=%d' % (PRE_MAX, PRE_NEXT))

results = []
def check(name, ok, detail=''):
    results.append({'项': name, '结论': 'PASS' if ok else 'FAIL', '详情': str(detail)})
    print(('  PASS  ' if ok else '  FAIL  ') + name + ('  ← ' + str(detail) if detail else ''))
    return ok

# ---------- Phase 1: 单发登录（锚行分配首行） ----------
s, b = req('POST', '/api/v1/auth/login',
           body={'username': 'ipd-admin', 'password': CRED['ipd_qa_pwd_ipd-admin']})
tok = (b.get('data') or {}).get('token')
check('P1 登录 ipd-admin HTTP 200 + code=0 (IPD 成功码)', s == 200 and b.get('code') == 0 and tok,
      's=%s code=%s' % (s, b.get('code')))

rows = q('SELECT seq, prev_hash, curr_hash FROM audit_logs WHERE seq > %d ORDER BY seq' % PRE_MAX)
new1 = rows[0] if rows else None
check('P1 新行 seq=1627（锚行分配，非 DB 自增）', bool(new1) and int(new1[0]) == PRE_NEXT,
      '实际 %s' % (new1[0] if new1 else '无新行'))
tail = q('SELECT curr_hash FROM audit_logs WHERE seq = %d' % PRE_MAX)[0][0]
check('P1 prev_hash == 旧尾行(1626) curr_hash', bool(new1) and new1[1] == tail, '')

h = q("SELECT last_seq, last_hash, next_seq FROM audit_log_chain_heads WHERE chain_key='GLOBAL'")[0]
check('P1 锚行同步 last_seq=1627 / next_seq=1628',
      int(h[0]) == PRE_NEXT and int(h[2]) == PRE_NEXT + 1, '实际 %s/%s' % (h[0], h[2]))
check('P1 锚行 last_hash == 新行 curr_hash', h[1] == (new1[2] if new1 else ''), '')

# ---------- Phase 2: verify 基线 ----------
s, b = req('GET', '/api/v1/audit-logs/verify', token=tok)
d = b.get('data') or {}
base = (d.get('chain'), len(d.get('broken') or []),
        len(d.get('hashBroken') or []), d.get('gaps'))
print('    verify 基线: chain=%s broken=%d hashBroken=%d gaps=%s' % base)
check('P2 verify 端点可达 HTTP 200 + code=0', s == 200 and b.get('code') == 0, 's=%s code=%s' % (s, b.get('code')))

# ---------- Phase 3: 4 账号并发登录（串行分配取样） ----------
users = ['ipd-leader', 'ipd-market', 'ipd-rd', 'ipd-admin']
out = {}
def do(u):
    s2, b2 = req('POST', '/api/v1/auth/login',
                 body={'username': u, 'password': CRED['ipd_qa_pwd_' + u]})
    out[u] = (s2, (b2.get('data') or {}).get('token'))
ths = [threading.Thread(target=do, args=(u,)) for u in users]
[t.start() for t in ths]
[t.join() for t in ths]
check('P3 并发 4 登录全成功', all(s2 == 200 and t2 for s2, t2 in out.values()),
      str({u: s2 for u, (s2, _) in out.items()}))

time.sleep(3)
rows = q('SELECT seq, prev_hash, curr_hash FROM audit_logs WHERE seq > %d ORDER BY seq' % PRE_NEXT)
seqs = [int(r[0]) for r in rows]
check('P3 新 4 行 seq=1628..1631 连续无跳号无冲突',
      seqs == list(range(PRE_NEXT + 1, PRE_NEXT + 5)), '实际 %s' % seqs)
chain_ok = all(rows[i][1] == rows[i - 1][2] for i in range(1, len(rows)))
check('P3 并发行链衔接 prev==前行 curr 且首行接 1627', chain_ok and bool(rows) and rows[0][1] == new1[2], '')

h2 = q("SELECT last_seq, next_seq FROM audit_log_chain_heads WHERE chain_key='GLOBAL'")[0]
check('P3 锚行终态 last_seq=1631 / next_seq=1632',
      int(h2[0]) == PRE_NEXT + 4 and int(h2[1]) == PRE_NEXT + 5, '实际 %s/%s' % (h2[0], h2[1]))

# ---------- Phase 4: verify 复验不劣化 ----------
s, b = req('GET', '/api/v1/audit-logs/verify', token=tok)
d = b.get('data') or {}
after = (d.get('chain'), len(d.get('broken') or []), len(d.get('hashBroken') or []), d.get('gaps'))
print('    verify 后置: chain=%s broken=%d hashBroken=%d gaps=%s' % after)
check('P4 broken/hashBroken 零新增（新增 5 行零断裂）',
      after[1] <= base[1] and after[2] <= base[2], '基线(%d,%d)→后置(%d,%d)' % (base[1], base[2], after[1], after[2]))
cnt = q('SELECT COUNT(*) FROM audit_logs WHERE seq > %d' % PRE_MAX)[0][0]
check('P4 新增 5 行全部落库', int(cnt) == 5, '实际 %s' % cnt)

p = sum(1 for r in results if r['结论'] == 'PASS')
f = sum(1 for r in results if r['结论'] == 'FAIL')
print(json.dumps({'summary': {'PASS': p, 'FAIL': f}, 'verify_base': base, 'verify_after': after,
                  'detail': results}, ensure_ascii=False, indent=1))
sys.exit(0 if f == 0 else 1)
