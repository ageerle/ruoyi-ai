// qa05-load 前端增强版：对 49 页矩阵核心页并发压测（TTFB + 吞吐 + P95）
import http from 'node:http';

const BASE = 'http://127.0.0.1:4177';
const PAGES = ['/login','/workspace','/project-list','/gate-review','/dashboard','/projects','/demands','/kpi/functional','/incentive/bonus-pool','/audit'];
const CONC = 16, PER = 20; // 16 并发 × 每页 20 次（对齐 QA-05-P1 后端 16 并发口径）

function get(path) {
  return new Promise((resolve) => {
    const t0 = process.hrtime.bigint();
    const req = http.get(BASE + path, (res) => {
      res.resume();
      res.on('end', () => resolve({ path, ms: Number(process.hrtime.bigint() - t0) / 1e6, code: res.statusCode }));
    });
    req.on('error', () => resolve({ path, ms: -1, code: 0 }));
  });
}
const pct = (a, p) => a[Math.min(a.length - 1, Math.floor(a.length * p))];

console.log(`qa05-frontend-load: base=${BASE} pages=${PAGES.length} conc=${CONC} per=${PER} total=${PAGES.length * PER * CONC}`);
const all = [];
for (const path of PAGES) {
  const results = [];
  for (let round = 0; round < PER; round++) {
    const batch = Array.from({ length: CONC }, () => get(path));
    results.push(...await Promise.all(batch));
  }
  const ok = results.filter(r => r.code === 200);
  const times = ok.map(r => r.ms).sort((a, b) => a - b);
  const row = { path, ok: ok.length, fail: results.length - ok.length,
    p50: +pct(times, .5).toFixed(1), p95: +pct(times, .95).toFixed(1), max: +times[times.length-1].toFixed(1) };
  all.push(row);
  console.log(`${path.padEnd(24)} ok=${row.ok}/${results.length} p50=${row.p50}ms p95=${row.p95}ms max=${row.max}ms`);
}
const tot = all.reduce((s, r) => s + r.ok, 0), fail = all.reduce((s, r) => s + r.fail, 0);
const p95s = all.map(r => r.p95).sort((a,b)=>a-b);
console.log(`\nTOTAL ok=${tot} fail=${fail} worstPageP95=${p95s[p95s.length-1]}ms`);
import fs from 'node:fs';
fs.writeFileSync('/Users/mac/Documents/ruoyi-ai/docs/ipd-系统说明/验收/qa05-frontend-load-result-20260907.json', JSON.stringify({ base: BASE, conc: CONC, per: PER, pages: all, total: { ok: tot, fail } }, null, 2));
console.log('report: docs/ipd-系统说明/验收/qa05-frontend-load-result-20260907.json');
