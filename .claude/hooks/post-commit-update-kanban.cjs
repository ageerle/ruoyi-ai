#!/usr/bin/env node
/**
 * post-commit-update-kanban.cjs
 * PostToolUse hook: git commit 成功后，自动 reconcile commit message 中的卡号到
 * vibe-kanban（manage.py set KEY STATUS --note ...）。
 *
 * 触发条件: PostToolUse Bash 命中 git commit
 * 行为: stderr 报告 updated X cards / skipped Y，不阻断 commit（exit 0）。
 *
 * 设计依据（R6 update_task 流程系统化 — 2026-09-06）:
 *   - R7 根因：本会话 20+ commit 卡面零同步，因 update_task 工具被系统性绕开
 *   - 根治方案: 自动 reconcile，commit message 提取卡号 → manage.py set
 *   - 容错: 卡号无/错 → 静默失败；status 解析失败 → 默认 done
 *   - 防抖: 用 .codex/vibe-kanban/last-processed-commit-sha 跟踪，避免双触发
 *
 * 安全: 使用 execFileSync 不经 shell，避免命令注入
 */

'use strict';

const fs = require('fs');
const { execFileSync } = require('child_process');
const path = require('path');

const REPO_ROOT = process.env.CLAUDE_PROJECT_DIR || process.cwd();
const MANAGE_REL = 'docs/ipd-系统说明/vibe-kanban/manage.py';
const MANAGE_ABS = path.join(REPO_ROOT, MANAGE_REL);
const STATE_DIR = path.join(REPO_ROOT, '.codex', 'vibe-kanban');
const LAST_SHA_FILE = path.join(STATE_DIR, 'last-processed-commit-sha');

// 卡号提取正则（宽松版：覆盖本会话已知所有 commit 卡号格式）
const CARD_REGEX = /\b(?:P[0-4]-\d+(?:\.\d+)?|HIGH-\d+(?:\.\d+)?(?:-\w+)?|MEDIUM-\d+(?:\.\d+)?(?:-\w+)?|LOW-\d+(?:\.\d+)?|SEC-[A-Z]+-[\w.-]+|SEC-[\w-]+|ROOT-R\d+(?:-[\w-]+)?|FIX-[\w-]+|GOVERNANCE-\d+|CONSISTENCY-\d+|DOC-[\w-]+|REFLECTION-\d+|DDL-[\w-]+|GUARD-\d+|WAVE[\w-]+|R\d+|AUD(?:-\w+)?-\d+|API-\d+|OPS(?:-\w+)?-\d+|QA-\d+|RISK-\d+|DB-\d+|DEF-\d+)\b/g;

function readStdin() {
  try {
    return fs.readFileSync(0, 'utf8');
  } catch (_) {
    return '';
  }
}

function safeExecFile(file, args, opts = {}) {
  try {
    const result = execFileSync(file, args, {
      encoding: 'utf8',
      timeout: 8000,
      stdio: ['ignore', 'pipe', 'pipe'],
      ...opts,
    });
    return { stdout: result, stderr: '' };
  } catch (e) {
    // execFileSync 抛出时，stdout/stderr 在 error 对象上
    return {
      stdout: e.stdout ? e.stdout.toString() : '',
      stderr: e.stderr ? e.stderr.toString() : '',
      error: true,
    };
  }
}

function detectStatus(message) {
  const lower = message.toLowerCase();
  if (/wip\b|in progress\b|inprogress\b|进行中/.test(lower)) return 'inprogress';
  if (/in review\b|inreview\b|待审\b|review\b|reviewing/.test(lower)) return 'inreview';
  if (/\bblocked\b|\btodo\b|阻塞|待办/.test(lower)) return 'todo';
  // 默认 done：闭环 / 落地 / 实装 / fix / feat / refactor / done / merged
  return 'done';
}

function buildNote(subject) {
  // manage.py set 要求: 单行、无 |、无 \n
  let s = (subject || '').replace(/\|/g, '-').replace(/\s+/g, ' ').trim();
  if (s.length > 80) s = s.slice(0, 77) + '...';
  if (!s) s = 'auto-reconciled';
  return s;
}

function main() {
  const raw = readStdin();
  if (!raw.trim()) process.exit(0);

  let payload;
  try {
    payload = JSON.parse(raw);
  } catch (_) {
    process.exit(0);
  }

  if ((payload.tool_name || '') !== 'Bash') process.exit(0);

  const toolInput = payload.tool_input || {};
  const toolResult = payload.tool_result || {};
  const command = toolInput.command || '';

  // 仅 git commit 触发
  if (!/\bgit\s+commit\b/.test(command)) process.exit(0);

  // 检查 commit 是否成功
  if (toolResult.interrupted === true) process.exit(0);
  const stderrText = String(toolResult.stderr || '');
  const firstLines = stderrText.split('\n').slice(0, 3).join('\n');
  if (/^error:|fatal:|nothing to commit/i.test(firstLines)) {
    process.stderr.write('[post-commit-kanban] git commit failed, skipping reconcile\n');
    process.exit(0);
  }

  // 取最新 commit SHA
  const shaOut = safeExecFile('git', ['rev-parse', 'HEAD'], { cwd: REPO_ROOT });
  if (!shaOut || !shaOut.stdout.trim()) {
    process.stderr.write('[post-commit-kanban] cannot get HEAD sha, skipping\n');
    process.exit(0);
  }
  const headSha = shaOut.stdout.trim();

  // 防抖：同一 commit 不重复处理
  try {
    fs.mkdirSync(STATE_DIR, { recursive: true });
  } catch (_) {}
  let lastSha = '';
  try {
    lastSha = fs.readFileSync(LAST_SHA_FILE, 'utf8').trim();
  } catch (_) {}
  if (lastSha === headSha) {
    process.exit(0);
  }

  // 取 commit message (subject + body)
  const messageOut = safeExecFile('git', ['log', '-1', '--pretty=%s%n---BODY---%b'], { cwd: REPO_ROOT });
  if (!messageOut || !messageOut.stdout) {
    process.stderr.write('[post-commit-kanban] cannot read commit message, skipping\n');
    process.exit(0);
  }
  const parts = messageOut.stdout.split(/\n---BODY---\n/);
  const subject = parts[0] || '';
  const fullMessage = subject + '\n' + (parts[1] || '');

  // 提取卡号
  const matches = Array.from(new Set(fullMessage.match(CARD_REGEX) || []));
  if (matches.length === 0) {
    process.stderr.write(`[post-commit-kanban] ${headSha.slice(0, 7)} no card keys, skipping\n`);
    try { fs.writeFileSync(LAST_SHA_FILE, headSha); } catch (_) {}
    process.exit(0);
  }

  // 解析 status
  const status = detectStatus(fullMessage);
  const note = buildNote(subject);

  let updated = 0;
  let skipped = 0;
  const skippedKeys = [];

  for (const key of matches) {
    // execFile 不经 shell，args 数组安全传递
    const out = safeExecFile('python3', [MANAGE_ABS, 'set', key, status, '--note', note], { cwd: REPO_ROOT });
    if (!out) {
      skipped++;
      skippedKeys.push(`${key}(sys-err)`);
    } else if (out.error && /No task with source ID/i.test(out.stderr)) {
      skipped++;
      skippedKeys.push(key);
    } else if (out.error) {
      // 其他 manage.py 错误（如权限、目录），跳过但标记
      skipped++;
      skippedKeys.push(`${key}(err)`);
    } else {
      // 成功路径：解析 JSON 输出，统计 actions
      let did = null;
      try {
        const parsed = JSON.parse(out.stdout);
        const acts = parsed.actions || [];
        did = acts.find((a) => a.key === key);
      } catch (_) {
        // 非 JSON 也算成功
      }
      updated++;
    }
  }

  // 记录已处理 SHA
  try { fs.writeFileSync(LAST_SHA_FILE, headSha); } catch (_) {}

  const skippedSuffix = skippedKeys.length > 0
    ? ` (skipped: ${skippedKeys.slice(0, 5).join(',')}${skippedKeys.length > 5 ? '...' : ''})`
    : '';
  process.stderr.write(
    `[post-commit-kanban] ${headSha.slice(0, 7)} → status=${status}, updated ${updated} cards, skipped ${skipped}${skippedSuffix}\n`
  );
  process.exit(0);
}

main();
