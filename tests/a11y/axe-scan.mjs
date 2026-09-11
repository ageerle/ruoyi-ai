#!/usr/bin/env node
/**
 * Wave21-A: axe-core a11y scan for IPD frontend critical pages.
 *
 * 模式选择（自动回退）：
 *   1) Puppeteer 模式（有 puppeteer-core + 系统 Chromium）：真浏览器跑 axe-core，结果最准
 *   2) jsdom 模式（默认）：纯 DOM fixture，CI 友好无浏览器依赖，结果快速但仅覆盖静态 HTML
 *
 * 输入：A11Y_TARGET_BASE_URL（默认 http://127.0.0.1:4173，ZK-IPD LIVE URL）
 *       A11Y_PAGES（默认 5 页 login/workbench/project-list/gate-review/dashboard）
 *       A11Y_MODE（puppeteer | jsdom | auto；默认 auto）
 *
 * 输出：
 *   tests/a11y/reports/a11y-report-<timestamp>.json   完整 axe 结果
 *   tests/a11y/reports/a11y-report-latest.json        最近一次结果（覆盖）
 *   控制台摘要：violations 按 impact 排序，>0 退出码 1（默认 WARN，调用方决定阻断）
 *
 * 阈值：A11Y_MAX_VIOLATIONS（默认不设，所有 violations 报告）
 *       A11Y_FAIL_ON_CRITICAL=1 时 critical 违规退出 2
 */
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import http from 'node:http';
import { JSDOM } from 'jsdom';
import axe from 'axe-core';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const REPORTS_DIR = path.join(__dirname, 'reports');

// 5 个 IPD 核心页面（与 docs/开发说明/_导航地图.md 对齐）
const DEFAULT_PAGES = [
  { name: 'login',         path: '/login' },
  { name: 'workbench',     path: '/workspace' },
  { name: 'project-list',  path: '/project-list' },
  { name: 'gate-review',   path: '/gate-review' },
  { name: 'dashboard',     path: '/dashboard' }
];

const BASE = process.env.A11Y_TARGET_BASE_URL || 'http://127.0.0.1:4173';
const MODE = (process.env.A11Y_MODE || 'auto').toLowerCase();
const PAGES = parsePagesEnv();
const MAX = process.env.A11Y_MAX_VIOLATIONS ? Number(process.env.A11Y_MAX_VIOLATIONS) : Infinity;
const FAIL_ON_CRITICAL = process.env.A11Y_FAIL_ON_CRITICAL === '1';

function parsePagesEnv() {
  if (!process.env.A11Y_PAGES) return DEFAULT_PAGES;
  try {
    const raw = JSON.parse(process.env.A11Y_PAGES);
    if (Array.isArray(raw) && raw.every(p => p.name && p.path)) return raw;
  } catch { /* fall through */ }
  return DEFAULT_PAGES;
}

function ensureReportsDir() {
  fs.mkdirSync(REPORTS_DIR, { recursive: true });
}

function summarize(results) {
  const lines = [];
  lines.push('');
  lines.push('========= A11y Scan Summary =========');
  lines.push(`Base URL : ${BASE}`);
  lines.push(`Mode     : ${results.mode}`);
  lines.push(`Pages    : ${results.pages.length}`);
  lines.push(`Total violations: ${results.totals.violations} (critical=${results.totals.critical}, serious=${results.totals.serious}, moderate=${results.totals.moderate}, minor=${results.totals.minor})`);
  lines.push('');
  for (const page of results.pages) {
    lines.push(`--- ${page.name} (${page.url}) status=${page.status} violations=${page.violations.length} ---`);
    if (page.violations.length === 0) {
      lines.push('  (clean)');
      continue;
    }
    for (const v of page.violations.slice(0, 5)) {
      lines.push(`  [${v.impact || 'unknown'}] ${v.id}: ${v.help} (nodes=${v.nodes.length})`);
      if (v.nodes[0]?.target) lines.push(`    target: ${JSON.stringify(v.nodes[0].target)}`);
    }
    if (page.violations.length > 5) lines.push(`  ... +${page.violations.length - 5} more`);
  }
  lines.push('=====================================');
  return lines.join('\n');
}

async function scanWithJsdom(page) {
  const url = BASE + page.path;
  const html = await fetchHtml(url);
  const dom = new JSDOM(html, { url, runScripts: 'outside-only', pretendToBeVisual: true });
  // axe-core 需要注入到目标 window
  dom.window.axe = axe;
  const result = await dom.window.axe.run(dom.window.document, {
    resultTypes: ['violations'],
    runOnly: { type: 'tag', values: ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'best-practice'] }
  });
  return { url, status: 200, violations: result.violations };
}

function fetchHtml(targetUrl) {
  return new Promise((resolve, reject) => {
    const req = http.get(targetUrl, { timeout: 5000 }, (res) => {
      if (res.statusCode && res.statusCode >= 400) {
        res.resume();
        reject(new Error(`HTTP ${res.statusCode}`));
        return;
      }
      let body = '';
      res.setEncoding('utf8');
      res.on('data', (c) => (body += c));
      res.on('end', () => resolve(body));
    });
    req.on('timeout', () => { req.destroy(new Error('timeout')); });
    req.on('error', reject);
  });
}

async function scanWithPuppeteer(page) {
  // 延迟导入 puppeteer-core（可选依赖，缺则回退 jsdom）
  const puppeteer = await import('puppeteer-core').catch(() => null);
  if (!puppeteer) throw new Error('puppeteer-core not installed');

  const executablePath = process.env.A11Y_CHROMIUM_PATH
    || '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome'
    || '/usr/bin/google-chrome';
  const browser = await puppeteer.default.launch({
    executablePath,
    headless: 'new',
    args: ['--no-sandbox', '--disable-setuid-sandbox', '--disable-dev-shm-usage']
  });
  try {
    const p = await browser.newPage();
    const url = BASE + page.path;
    const resp = await p.goto(url, { waitUntil: 'networkidle0', timeout: 15000 }).catch(() => null);
    await p.evaluate(axe.source); // 注入 axe-core
    const result = await p.evaluate(async () => {
      // eslint-disable-next-line no-undef
      return await axe.run(document, {
        runOnly: { type: 'tag', values: ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'best-practice'] }
      });
    });
    return { url, status: resp ? resp.status() : 0, violations: result.violations };
  } finally {
    await browser.close();
  }
}

async function probeCapability() {
  if (MODE === 'jsdom') return 'jsdom';
  if (MODE === 'puppeteer') return 'puppeteer';
  // auto: 试 puppeteer，失败回退 jsdom
  try {
    const puppeteer = await import('puppeteer-core').catch(() => null);
    if (!puppeteer) return 'jsdom';
    const exec = process.env.A11Y_CHROMIUM_PATH;
    if (exec && !fs.existsSync(exec)) return 'jsdom';
    return 'puppeteer';
  } catch { return 'jsdom'; }
}

function countByImpact(violations) {
  const tally = { critical: 0, serious: 0, moderate: 0, minor: 0 };
  for (const v of violations) {
    const impact = v.impact || 'minor';
    if (tally[impact] !== undefined) tally[impact]++;
  }
  return tally;
}

async function main() {
  ensureReportsDir();
  const mode = await probeCapability();
  const pages = [];
  for (const page of PAGES) {
    process.stderr.write(`[a11y] scanning ${page.name} @ ${BASE}${page.path} (mode=${mode})\n`);
    try {
      const r = mode === 'puppeteer' ? await scanWithPuppeteer(page) : await scanWithJsdom(page);
      pages.push({ name: page.name, ...r });
    } catch (err) {
      pages.push({
        name: page.name,
        url: BASE + page.path,
        status: 0,
        violations: [],
        error: String(err && err.message || err)
      });
    }
  }
  const totals = pages.reduce(
    (acc, p) => {
      const t = countByImpact(p.violations);
      acc.violations += p.violations.length;
      acc.critical += t.critical;
      acc.serious += t.serious;
      acc.moderate += t.moderate;
      acc.minor += t.minor;
      return acc;
    },
    { violations: 0, critical: 0, serious: 0, moderate: 0, minor: 0 }
  );

  const results = {
    timestamp: new Date().toISOString(),
    base: BASE,
    mode,
    pages,
    totals
  };
  const stamp = results.timestamp.replace(/[:.]/g, '-');
  const jsonPath = path.join(REPORTS_DIR, `a11y-report-${stamp}.json`);
  const latestPath = path.join(REPORTS_DIR, 'a11y-report-latest.json');
  fs.writeFileSync(jsonPath, JSON.stringify(results, null, 2));
  fs.writeFileSync(latestPath, JSON.stringify(results, null, 2));
  process.stdout.write(summarize(results) + '\n');
  process.stdout.write(`[a11y] report written: ${jsonPath}\n`);

  const criticalCount = totals.critical;
  if (FAIL_ON_CRITICAL && criticalCount > 0) {
    process.stderr.write(`[a11y] FAIL_ON_CRITICAL=1 and ${criticalCount} critical violations found — exit 2\n`);
    process.exit(2);
  }
  if (totals.violations > MAX) {
    process.stderr.write(`[a11y] violations ${totals.violations} > MAX ${MAX} — exit 1\n`);
    process.exit(1);
  }
  // 默认 WARN：不阻断 PR（与 docs-link-check.yml 风格一致）
  process.stdout.write(`[a11y] WARN mode (violations=${totals.violations}); set A11Y_FAIL_ON_CRITICAL=1 to fail on critical.\n`);
}

main().catch((err) => {
  process.stderr.write(`[a11y] FATAL: ${err.stack || err}\n`);
  process.exit(3);
});
