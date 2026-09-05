import sys, re
sys.path.insert(0, '/Users/mac/Documents/ruoyi-ai/docs/ipd-系统说明/vibe-kanban')
import manage
rows = manage.plan()
done = [r for r in rows if r['status'] == 'done']

def evidence_type(ss):
    if re.search(r'[0-9a-f]{7,}', ss): return 'git-hash'
    if re.search(r'Tests run:s*d+.*Failures:s*0', ss): return 'test-count'
    if re.search(r'BUILD SUCCESS', ss): return 'build-success'
    if re.search(r'd+/d+ PASS|d+ PASS', ss): return 'pass-count'
    if re.search(r'真库|HTTP|实灌|curl', ss): return 'integration'
    return 'asserted-only'

from collections import Counter
cats = Counter()
for r in done:
    cats[evidence_type(r['source_status'])] += 1
print('Evidence categories among done cards:')
for k, v in cats.most_common():
    print('  %s: %d' % (k, v))

weak = [r for r in done if evidence_type(r['source_status']) == 'asserted-only']
print('')
print('asserted-only done cards: %d' % len(weak))
for r in weak:
    src = r['source_status'].replace(chr(10), ' ')
    print('  %s: %s' % (r['key'], src[:120]))

mine = ['SEC-API-01','SEC-API-02','RISK-01','RISK-02','RISK-03','RISK-04','DB-02']
print('')
print('=== My onboarded cards ===')
by_key = {r['key']: r for r in rows}
for k in mine:
    r = by_key.get(k)
    if r:
        et = evidence_type(r['source_status'])
        src = r['source_status'].replace(chr(10), ' ')
        print('  %s: %s | %s' % (k, et, src[:120]))
