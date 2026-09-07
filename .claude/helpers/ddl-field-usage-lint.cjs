#!/usr/bin/env node
/**
 * ddl-field-usage-lint.cjs
 * PostToolUse hook: 写 SQL DDL 文件（CREATE TABLE / ALTER TABLE）后，
 * 扫描新增字段在 Java 代码中的引用次数；若 7 天内 < 3 处引用，
 * stderr 输出 WARN 提示「字段孤岛」。
 *
 * 触发条件: Write | Edit | MultiEdit 命中 *.sql 路径
 * 行为: WARN 级（exit 0，不阻断；与 sensitive-field-guard ERROR 阻断区分）
 * 输出: stderr 人读警告 + JSON 报告（机器可读）
 *
 * 模式: --report=bootstrap|weekly 时，跳过 stdin 解析，进入全量回扫模式
 *       （owner 用于决策是否注册 settings.json 前的端到端验证）
 *
 * 关联卡: ROOT-R5-字段孤岛治理-20260906.md / P1-13
 */

'use strict';

const fs = require('fs');
const path = require('path');
const { execFileSync } = require('child_process');

// ---------- 工具函数 ----------

function readStdinSync() {
  try {
    // 直接从 fd 0 读 stdin，避开 process.stdin.read() 在同步上下文里的不可靠性
    return fs.readFileSync(0, 'utf8');
  } catch (e) {
    return '';
  }
}

function extractFilePath(input) {
  if (!input) return '';
  return input.file_path || input.filePath || input.path || '';
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

// ---------- DDL 解析 ----------

/**
 * 从 SQL 内容中提取「字段定义」
 * 支持 CREATE TABLE (...) 和 ALTER TABLE ... ADD COLUMN ...
 * 返回 [{ column, table, line }]
 */
function parseDdlColumns(sql) {
  const cols = [];
  const lines = sql.split(/\r?\n/);

  // 1) CREATE TABLE xxx ( ... )
  let inCreate = null;
  for (let i = 0; i < lines.length; i++) {
    const line = lines[i].trim();
    const m = line.match(/^CREATE\s+TABLE\s+(?:IF\s+NOT\s+EXISTS\s+)?[`"']?(\w+)[`"']?\s*\(/i);
    if (m) {
      inCreate = { table: m[1], startLine: i };
      continue;
    }
    if (inCreate) {
      // 行内字段定义: col_name TYPE [constraints]
      const fm = line.match(/^[`"']?(\w+)[`"']?\s+(?:BIGINT|INT|INTEGER|VARCHAR|TEXT|DATETIME|TIMESTAMP|DATE|DECIMAL|FLOAT|DOUBLE|TINYINT|SMALLINT|JSON|BOOLEAN|BIT|CHAR|BLOB)/i);
      if (fm) {
        cols.push({ column: fm[1], table: inCreate.table, line: i + 1, source: 'CREATE TABLE' });
      }
      // CREATE TABLE 块结束
      if (line.endsWith(');') || line === ');') {
        inCreate = null;
      }
    }
  }

  // 2) ALTER TABLE ... ADD [COLUMN] col_name TYPE
  for (let i = 0; i < lines.length; i++) {
    const line = lines[i].trim();
    const m = line.match(/^ALTER\s+TABLE\s+[`"']?(\w+)[`"']?\s+ADD\s+(?:COLUMN\s+)?[`"']?(\w+)[`"']?\s+/i);
    if (m) {
      cols.push({ column: m[2], table: m[1], line: i + 1, source: 'ALTER TABLE' });
    }
  }

  return cols;
}

// ---------- 字段命名风格转换 ----------

/**
 * snake_case → camelCase
 * DDL 字段命名（del_flag / project_id / is_locked）→ Java 实体命名（delFlag / projectId / isLocked）
 * 与 ZK-IPD live URL 字段命名风格匹配
 */
function snakeToCamel(s) {
  return s.replace(/_([a-z0-9])/g, (_, ch) => ch.toUpperCase());
}

/**
 * camelCase → snake_case（DDL 字段若已用 camel 也兼容反向查找）
 */
function camelToSnake(s) {
  return s.replace(/[A-Z]/g, (ch) => '_' + ch.toLowerCase()).replace(/^_/, '');
}

// ---------- 字段引用扫描 ----------

/**
 * 把 git grep -c 的多行输出聚合成总数
 * 输出格式: <path>:<count>（路径在前，数字在行尾——每行）
 *
 * [ROOT-R5 fix] 旧实现按 /^(\d+):/ 解析方向错（以为数字在前），
 * 导致全部算 0、字段孤岛永远 WARN；改为行尾冒号后数字。
 */
function sumGrepCount(out) {
  let total = 0;
  for (const line of out.split('\n')) {
    if (!line.trim()) continue;
    const m = line.match(/:\s*(\d+)\s*$/);
    if (m) total += parseInt(m[1], 10);
  }
  return total;
}

// scope 形参 → git pathspec 映射（paths 全用 pathspec glob 字符串）
//   'main'  → ruoyi-modules/**/src/main/java/**/*.java（生产代码）
//   'test'  → **/src/test/java/**/*.java（测试代码）
//   'all'   → *.java（全仓库，默认）
function scopeToPaths(scope) {
  if (scope === 'main') return ['ruoyi-modules/**/src/main/java/**/*.java'];
  if (scope === 'test') return ['**/src/test/java/**/*.java'];
  return ['*.java'];
}

/**
 * 用 git grep 数字段在 *.java 中的引用行数（粗计数）
 *
 * 同时查 snake_case 与 camelCase 两种命名——DDL 字段命名风格不统一：
 *   - snake: del_flag / project_id / is_locked（SQL DDL 主流）
 *   - camel: delFlag / projectId / isLocked（Java entity 主流）
 * 去重后逐个 grep，单字段最多 3 个查询（fieldName + snake + camel）。
 *
 * scope 形参已实际驱动 pathspec，不再是装饰形参。
 *
 * timeout 8s 防止 PreToolUse 超时（默认 10s）
 */
function countJavaReferences(fieldName, scope = 'all') {
  const paths = scopeToPaths(scope);
  const names = new Set([fieldName, snakeToCamel(fieldName), camelToSnake(fieldName)]);
  let total = 0;
  for (const name of names) {
    try {
      const out = execFileSync(
        'git',
        ['grep', '-c', '-w', name, '--', ...paths],
        { cwd: process.cwd(), timeout: 8000, stdio: ['ignore', 'pipe', 'ignore'] },
      ).toString();
      total += sumGrepCount(out);
    } catch (e) {
      // git grep 退出码 1 表示 0 命中、0 表示 ≥1 命中、128 表示仓库外
      if (e.status === 1) continue; // 0 命中，正常
      return -1; // 仓库外或其他错误
    }
  }
  return total;
}

// ---------- 时间窗判定 ----------

/**
 * 判断当前是否处于「字段建而不接」的合理窗口期
 * 通过 git log 检测该 SQL 文件 7 天内是否有改动
 * 若有：返回 true（合理窗口，不告警）
 * 若无：返回 false（已超过窗口，应当告警）
 *
 * 简化策略：扫描整个仓库最近 7 天内是否对该 SQL 文件做过改动。
 * 真实场景下"7 天前加的字段"才进入告警判定；
 * 此处取宽口径：本次写操作如果是 CREATE/ALTER，立即告警（owner 应当当场补 4 层）。
 */
function isWithinWindow() {
  // 设计：本次 DDL 写入即视为「新建字段时刻」，
  // owner 在 commit 前的 review 窗口应立即触发 4 层检查，
  // 7 天窗口由 owner 后续用 weekly report 跑历史回扫。
  return false;
}

// ---------- 主流程 ----------

function processPayload(payload) {
  const toolName = payload.tool_name || payload.toolName || '';
  if (!/^(Write|Edit|MultiEdit)$/.test(toolName)) return null;

  const filePath = extractFilePath(payload.tool_input || payload.toolInput || {});
  if (!/\.(sql)$/i.test(filePath)) return null;

  const content = extractContent(payload.tool_input || payload.toolInput || {});
  const cols = parseDdlColumns(content);
  if (cols.length === 0) return null;

  const findings = [];
  for (const c of cols) {
    const refs = countJavaReferences(c.column);
    const status = refs < 3 ? 'WARN' : 'OK';
    findings.push({
      table: c.table,
      column: c.column,
      java_refs: refs,
      source: c.source,
      line: c.line,
      status,
    });
  }

  return { filePath, findings };
}

function emitReport(report) {
  if (!report || report.findings.length === 0) return;
  const warns = report.findings.filter((f) => f.status === 'WARN');
  if (warns.length === 0) return;

  const lines = [];
  lines.push(`\n[ddl-field-usage-lint] WARN ${report.filePath} 新增字段 Java 引用 < 3，建议检查 4 层消费链路:`);
  for (const w of warns) {
    lines.push(`  - ${w.table}.${w.column} (${w.source}, line ${w.line}) → Java 引用 ${w.java_refs} 处`);
  }
  lines.push('  4 层消费链路: Entity / Mapper / DTO / Controller');
  lines.push('  关联卡: ROOT-R5-字段孤岛治理-20260906.md');
  lines.push('');

  // 人读警告（stderr）
  process.stderr.write(lines.join('\n') + '\n');

  // JSON 报告（stderr 第二段，机器可读）
  const jsonReport = {
    hook: 'ddl-field-usage-lint.cjs',
    timestamp: new Date().toISOString(),
    file: report.filePath,
    findings: warns,
    policy: 'DDL 新增字段 7 天内 Java 引用 >= 3 处',
  };
  process.stderr.write('\n---JSON---\n' + JSON.stringify(jsonReport, null, 2) + '\n---END---\n');
}

function main() {
  // 模式 1: --report=bootstrap|weekly → 全量回扫（owner 端到端验证用）
  const args = process.argv.slice(2);
  if (args.includes('--report=bootstrap') || args.includes('--report=weekly')) {
    process.stderr.write(
      '[ddl-field-usage-lint] 全量回扫模式未实现（待 owner 决策后扩展），当前仅 stdin 模式可用。\n',
    );
    process.exit(0);
  }

  // 模式 2: stdin payload → 单次写入扫描
  const raw = readStdinSync();
  if (!raw.trim()) process.exit(0);

  let payload;
  try {
    payload = JSON.parse(raw);
  } catch (e) {
    process.exit(0);
  }

  const report = processPayload(payload);
  if (!report) process.exit(0);

  emitReport(report);
  process.exit(0); // WARN 级不阻断
}

main();