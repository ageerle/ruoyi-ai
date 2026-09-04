#!/usr/bin/env node
/**
 * wiki-lint.cjs — RuoYi-AI wiki 完整性检查
 *
 * 检查项：
 *   1. 每篇 wiki 文章的 raw: 字段列出的文件都存在
 *   2. 每篇 wiki 文章含 topic / title / updated frontmatter
 *   3. 没有孤立的 raw 文件（被任何 wiki 引用过）
 *
 * 使用：node docs/wiki/wiki-lint.cjs
 */

const fs = require('fs');
const path = require('path');

const WIKI_DIR = path.resolve(__dirname);
const RAW_DIR = path.join(WIKI_DIR, 'raw');
const WIKI_SUBDIR = path.join(WIKI_DIR, 'wiki');

let pass = 0, fail = 0;

function ok(msg) { pass++; console.log('✅ ' + msg); }
function bad(msg) { fail++; console.log('❌ ' + msg); }

function findArticles(dir) {
  const out = [];
  function walk(d) {
    if (!fs.existsSync(d)) return;
    for (const f of fs.readdirSync(d)) {
      const full = path.join(d, f);
      const stat = fs.statSync(full);
      if (stat.isDirectory()) walk(full);
      else if (f.endsWith('.md') && f !== 'index.md' && f !== 'log.md') out.push(full);
    }
  }
  walk(dir);
  return out;
}

const articles = findArticles(WIKI_SUBDIR);
console.log('=== ' + articles.length + ' 篇 wiki 文章 ===\n');

const rawFilesUsed = new Set();

for (const article of articles) {
  const rel = path.relative(WIKI_DIR, article);
  const content = fs.readFileSync(article, 'utf8');

  const fmMatch = content.match(/^---\n([\s\S]*?)\n---/);
  if (!fmMatch) { bad('[' + rel + '] 缺 frontmatter'); continue; }
  const fm = fmMatch[1];

  if (!/^topic:\s*.+$/m.test(fm)) bad('[' + rel + '] frontmatter 缺 topic');
  else ok('[' + rel + '] topic OK');
  if (!/^title:\s*.+$/m.test(fm)) bad('[' + rel + '] frontmatter 缺 title');
  if (!/^updated:\s*.+$/m.test(fm)) bad('[' + rel + '] frontmatter 缺 updated');

  const rawSection = content.match(/^raw:\n((?:  - .+\n?)+)/m);
  if (!rawSection) { bad('[' + rel + '] 缺 raw 列表'); continue; }
  const rawRefs = (rawSection[1].match(/  - (.+)/g) || [])
    .map(line => line.replace(/^  - /, '').trim());

  if (rawRefs.length === 0) { bad('[' + rel + '] raw 列表为空'); continue; }

  for (const ref of rawRefs) {
    const rawPath = path.join(WIKI_DIR, ref);
    if (!fs.existsSync(rawPath)) bad('[' + rel + '] raw 引用不存在: ' + ref);
    else { ok('[' + rel + '] 引用 ' + ref); rawFilesUsed.add(ref); }
  }
}

console.log('\n=== 孤立 raw 文件检查 ===\n');

function listRaw(dir, base) {
  const out = [];
  if (!fs.existsSync(dir)) return out;
  for (const f of fs.readdirSync(dir)) {
    const full = path.join(dir, f);
    const stat = fs.statSync(full);
    if (stat.isDirectory()) out.push(...listRaw(full, path.join(base || '', f)));
    else if (f.endsWith('.md')) out.push(path.join(base || '', f));
  }
  return out;
}

const allRaw = listRaw(RAW_DIR, 'raw');
let orphanCount = 0;
for (const r of allRaw) {
  if (!rawFilesUsed.has(r)) {
    if (r.endsWith('.gitkeep')) continue;
    if (orphanCount < 10) bad('孤立 raw（未被引用）: ' + r);
    orphanCount++;
  }
}
if (orphanCount > 10) bad('... 还有 ' + (orphanCount - 10) + ' 个孤立 raw 文件');

console.log('\n' + '='.repeat(50));
console.log('通过: ' + pass + ' | 失败: ' + fail + ' | 孤立 raw: ' + orphanCount);
console.log('='.repeat(50));

process.exit(fail > 0 ? 1 : 0);