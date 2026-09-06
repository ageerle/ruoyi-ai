#!/usr/bin/env node
/**
 * ipd-frontend-drift-guard.cjs
 * PreToolUse hook: 阻断 IPD 前端工程写入"已存在正本"的重复文件 / 重复函数。
 *
 * 触发条件: Write | Edit | MultiEdit 工具
 * 阻断目标 (基于 2026-09-06 反思-前端代码漂移根因分析):
 *   1. 创建/重写 `apps/web-antd/src/views/ipd/<domain>/<domain>-error.ts` 类文件
 *      → 强制走 `apps/web-antd/src/views/ipd/_shared/ipd-error-text.ts`
 *   2. 创建/重写 `apps/web-antd/src/api/ipd/product-group.ts`
 *      → 强制走 `apps/web-antd/src/api/ipd/product.ts`（product.ts 已有 ProductGroup + listProductGroups）
 *   3. 在 `apps/web-antd/src/api/ipd/*.ts` 中新增 `export (async )?function <name>` 时
 *      → 与其他 api/ipd/*.ts 已导出同名函数冲突时阻断
 *   4. 在 `apps/web-antd/src/api/ipd/*.ts` 中新增 `export interface/const <Name>`
 *      → 与其他 api/ipd/*.ts 已导出同名 Symbol 冲突时阻断
 *
 * 设计: 阻断而非警告——以"重写即拒绝"为强制机制，逼迫新建前先 grep。
 *
 * 退出码:
 *   0  = 允许
 *   2  = 阻断，stderr 输出阻断原因
 *   其他 = 错误（不阻断）
 */

const fs = require('fs');
const path = require('path');

const FRONTEND_ROOT = '/Users/mac/Documents/ruoyi-ipd-web';
const API_DIR = path.join(FRONTEND_ROOT, 'apps/web-antd/src/api/ipd');
const VIEWS_DIR = path.join(FRONTEND_ROOT, 'apps/web-antd/src/views/ipd');
const SHARED_ERROR = path.join(FRONTEND_ROOT, 'apps/web-antd/src/views/ipd/_shared/ipd-error-text.ts');

function readStdinSync() {
  try {
    return fs.readFileSync(0, 'utf8');
  } catch (e) {
    return '';
  }
}

function extractFilePath(input) {
  if (!input) return '';
  return input.file_path || input.filePath || input.path || input.notebook_path || '';
}

function extractContent(input) {
  if (!input) return '';
  return (
    input.content ||
    input.new_string ||
    input.newString ||
    input.text ||
    JSON.stringify(input)
  );
}

function extractEdits(input) {
  // MultiEdit 模式: edits: [{old_string, new_string}, ...]
  if (Array.isArray(input.edits)) return input.edits;
  return null;
}

/** 列出 api/ipd 下所有 .ts 文件名（不含 .test.ts） */
function listApiFiles() {
  if (!fs.existsSync(API_DIR)) return [];
  return fs
    .readdirSync(API_DIR)
    .filter((f) => f.endsWith('.ts') && !f.endsWith('.test.ts'));
}

/** 提取一个 .ts 文件里所有 export 顶层声明（function / async function / const / interface） */
function extractExports(content) {
  const exports = [];
  const regex = /export\s+(?:async\s+)?function\s+([A-Za-z0-9_]+)/g;
  let m;
  while ((m = regex.exec(content))) {
    exports.push({ kind: 'function', name: m[1] });
  }
  const reConst = /export\s+const\s+([A-Za-z0-9_]+)/g;
  while ((m = reConst.exec(content))) {
    exports.push({ kind: 'const', name: m[1] });
  }
  const reIf = /export\s+interface\s+([A-Za-z0-9_]+)/g;
  while ((m = reIf.exec(content))) {
    exports.push({ kind: 'interface', name: m[1] });
  }
  const reType = /export\s+type\s+([A-Za-z0-9_]+)/g;
  while ((m = reType.exec(content))) {
    exports.push({ kind: 'type', name: m[1] });
  }
  return exports;
}

/** 从 src/views/ipd/<x>/<x>-error.ts 模式提取 domain（<x>） */
function domainErrorFilePath(fp) {
  const m = fp.match(/views\/ipd\/([a-z0-9_-]+)\/([a-z0-9_-]+)-error\.ts$/);
  if (!m) return null;
  return m[1];
}

function inFrontendScope(fp) {
  return fp.startsWith(FRONTEND_ROOT) || fp.startsWith('apps/web-antd/src/');
}

function relativeToFrontend(fp) {
  if (fp.startsWith(FRONTEND_ROOT)) {
    return path.relative(FRONTEND_ROOT, fp);
  }
  return fp;
}

/** 是否在 api/ipd/ 范围内（兼容绝对路径与相对 apps/ 前缀） */
function isInApiScope(relOrAbs) {
  return /(^|\/)api\/ipd\/[a-z0-9_-]+\.ts$/.test(relOrAbs);
}

/** 是否在 views/ipd/ 范围内 */
function isInViewsScope(relOrAbs) {
  return /(^|\/)views\/ipd\//.test(relOrAbs);
}

function main() {
  const raw = readStdinSync();
  if (!raw.trim()) process.exit(0);

  let payload;
  try {
    payload = JSON.parse(raw);
  } catch (e) {
    process.exit(0);
  }

  const toolName = payload.tool_name || payload.toolName || '';
  if (!/^(Write|Edit|MultiEdit)$/.test(toolName)) process.exit(0);

  const toolInput = payload.tool_input || payload.toolInput || {};
  const filePath = extractFilePath(toolInput);
  const content = extractContent(toolInput);
  const edits = extractEdits(toolInput);

  if (!filePath) process.exit(0);
  if (!inFrontendScope(filePath)) process.exit(0);

  const rel = relativeToFrontend(filePath);

  // 1) 拦截: 在 views/ipd/<domain>/ 下新建 <domain>-error.ts
  //    强制走 _shared/ipd-error-text.ts
  if (toolName === 'Write' && /views\/ipd\/[a-z0-9_-]+\/[a-z0-9_-]+-error\.ts$/.test(rel)) {
    const domain = domainErrorFilePath(rel);
    process.stderr.write(
      `\n[ipd-frontend-drift-guard] ⛔ 阻断: 在 views/ipd/${domain}/ 下新建 ${domain}-error.ts。\n` +
        `  理由: 错误码 → 中文文案映射必须集中。\n` +
        `  已有: ${path.relative(FRONTEND_ROOT, SHARED_ERROR)}\n` +
        `  处置: 改为 import { ipdErrorText } from '${path.relative(path.dirname(filePath), SHARED_ERROR).replace(/\\/g, '/').replace(/\.ts$/, '')}'，\n` +
        `        然后在共享表加该域覆写 codeTexts。\n\n`,
    );
    process.exit(2);
  }

  // 2) 拦截: 写 api/ipd/product-group.ts（重写整个文件）
  if (toolName === 'Write' && /api\/ipd\/product-group\.ts$/.test(rel)) {
    process.stderr.write(
      `\n[ipd-frontend-drift-guard] ⛔ 阻断: api/ipd/product-group.ts 是历史漂移产物。\n` +
        `  理由: api/ipd/product.ts 已有 ProductGroup / listProductGroups / normalizeGroup。\n` +
        `  已有: apps/web-antd/src/api/ipd/product.ts:31 (ProductGroup), 94 (listProductGroups)\n` +
        `  处置: 在 product.ts 中追加功能，删除 product-group.ts（或保留文件做 @deprecated 重导出）。\n\n`,
    );
    process.exit(2);
  }

  // 3) 函数级 / Symbol 级冲突扫描（针对 api/ipd/*.ts）
  if (!isInApiScope(rel) || !rel.endsWith('.ts')) process.exit(0);
  if (rel.endsWith('.test.ts')) process.exit(0);

  // 把目标文件自身的"已存在导出"作为基线
  const targetFileAbs = path.isAbsolute(filePath) ? filePath : path.join(FRONTEND_ROOT, rel);
  const baseFileName = path.basename(rel); // 如 product.ts
  const targetExports = new Set(
    fs.existsSync(targetFileAbs)
      ? extractExports(fs.readFileSync(targetFileAbs, 'utf8')).map((e) => e.name)
      : [],
  );

  // 收集待写入的"新增"导出（出现在新内容但不在原文件）
  const candidateExports = extractExports(content);
  const newExports = candidateExports.filter((e) => !targetExports.has(e.name));

  if (newExports.length === 0) process.exit(0);

  // 扫描 api/ipd 下其他文件已存在的同名导出
  const conflicts = [];
  for (const file of listApiFiles()) {
    if (file === baseFileName) continue; // 自身跳过
    const sibling = path.join(API_DIR, file);
    if (!fs.existsSync(sibling)) continue;
    const siblingExports = extractExports(fs.readFileSync(sibling, 'utf8'));
    for (const ne of newExports) {
      const hit = siblingExports.find((se) => se.name === ne.name && se.kind === ne.kind);
      if (hit) {
        conflicts.push({
          newName: ne.name,
          newKind: ne.kind,
          existingFile: `api/ipd/${file}`,
        });
      }
    }
  }

  if (conflicts.length > 0) {
    const lines = conflicts.map(
      (c) =>
        `  - ${c.newKind} ${c.newName}  已被 ${c.existingFile} 定义`,
    );
    process.stderr.write(
      `\n[ipd-frontend-drift-guard] ⛔ 阻断: ${rel} 新增导出与既有文件冲突。\n` +
        `  冲突清单:\n${lines.join('\n')}\n` +
        `  理由: 同名导出在 api/ipd/ 下不可二义（类型/接口/常量都会让消费方被迫二选一）。\n` +
        `  处置: import 既有实现并 re-export，或在既有文件中追加；不要另起炉灶。\n\n`,
    );
    process.exit(2);
  }

  // 4) MultiEdit 模式: 检查每条 edit 的 new_string 是否有新增冲突导出
  if (edits) {
    for (const edit of edits) {
      const editNew = edit.new_string || edit.newString || '';
      if (!editNew) continue;
      const editExports = extractExports(editNew);
      const newOnes = editExports.filter((e) => !targetExports.has(e.name));
      for (const file of listApiFiles()) {
        if (file === baseFileName) continue;
        const sibling = path.join(API_DIR, file);
        if (!fs.existsSync(sibling)) continue;
        const siblingExports = extractExports(fs.readFileSync(sibling, 'utf8'));
        for (const ne of newOnes) {
          if (siblingExports.find((se) => se.name === ne.name && se.kind === ne.kind)) {
            process.stderr.write(
              `\n[ipd-frontend-drift-guard] ⛔ 阻断: ${rel} MultiEdit 引入冲突 ${ne.kind} ${ne.name}（与 ${file} 重复）。\n\n`,
            );
            process.exit(2);
          }
        }
      }
    }
  }

  process.exit(0);
}

main();
