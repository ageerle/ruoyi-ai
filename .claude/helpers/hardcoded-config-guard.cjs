#!/usr/bin/env node
/**
 * hardcoded-config-guard.cjs
 * PostToolUse hook: 写 ruoyi-ipd 的 service impl Java 时，若出现业务参数硬编码字面量
 * （0.05、60、3、5、50000、24、48、3600 等），stderr WARN 提示「应走 BusinessConfigService」。
 *
 * 触发条件: Write / Edit / MultiEdit 命中 ruoyi-ipd 的 service impl 路径下的 Java
 * 行为: WARN 级（exit 0，不阻断；与 sensitive-field-guard ERROR 阻断区分）
 * 输出: stderr 人读警告 + JSON 报告（机器可读）
 *
 * 白名单（豁免）:
 *   - @Value("${...}") 引用
 *   - System.getenv(...) / System.getProperty(...)
 *   - 单行注释与块注释
 *   - BigDecimal.valueOf(...) / new BigDecimal(...)
 *   - 单元测试路径（src/test/）
 *
 * 关联卡: ROOT-R1 业务参数配置化根治 / P0-7
 */

'use strict';

const fs = require('fs');

// ---------- 业务参数键 → 默认值映射 ----------
// 与 BusinessConfigKeys.java 默认值一一对应（ROOT-R1 根治）
const CONFIG_DEFAULTS = [
  { key: 'bonus.poolRate',          value: '0.05',    pattern: /\b0\.0[0-9]{2,}\b/ },
  { key: 'kpi.stopThreshold',       value: '60',      pattern: /\b60\b/ },
  { key: 'deletion.cooldownDays',   value: '3',       pattern: /\b3\b/ },
  { key: 'gate.dualSignCount',      value: '3',       pattern: /\b3\b/ },
  { key: 'gate.signDeadlineDays',   value: '3',       pattern: /\b3\b/ },
  { key: 'gate.extension.maxCount', value: '1',       pattern: /\b1\b/ },
  { key: 'deletion.escalateTimeoutHours', value: '48', pattern: /\b48\b/ },
  { key: 'bonus.rows-per-page',     value: '5',       pattern: /\b5\b/ },
  { key: 'bonus.pageSize',          value: '50000',   pattern: /\b50000\b/ },
  { key: 'kpi.idle-days',           value: '24',      pattern: /\b24\b/ },
  { key: 'cache.ttl-seconds',       value: '3600',    pattern: /\b3600\b/ },
];

// 上下文化关键字（出现下列关键字时，附近的数字字面量更可能是业务参数）
const CONTEXT_KEYWORDS = [
  'kpi', 'bonus', 'pool', 'deletion', 'delete', 'gate', 'sign',
  'cooldown', 'threshold', 'days', 'deadline', 'escalate', 'tier',
  'coefficient', 'rate', 'idle', 'rows', 'page', 'cache', 'ttl',
];

// ---------- 工具函数 ----------

function readStdinSync() {
  try {
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

/**
 * 去除 Java 注释和字符串字面量，避免误报
 * 简化策略：
 *   - 块注释 整段替换为占位
 *   - 行注释 // ... \n → 整体替换为占位
 *   - 字符串字面量 "..." → 整体替换为占位
 */
function stripCommentsAndStrings(code) {
  return code
    .replace(/\/\*[\s\S]*?\*\//g, (m) => ' '.repeat(m.length))
    .replace(/\/\/[^\n]*/g, (m) => ' '.repeat(m.length))
    .replace(/"(?:[^"\\\n]|\\.)*"/g, (m) => ' '.repeat(m.length));
}

/**
 * 检测一段 Java 代码是否含 @Value 引用（已走配置中心，豁免）
 */
function hasValueAnnotation(code, literal) {
  // @Value("${...}") 或 @Value("#{...}")
  // 形式: literal 出现在 @Value 同一段代码块中即视为已豁免
  const valueRe = /@Value\s*\(\s*["']\$\{[^}]+\}["']\s*\)/g;
  const seen = [];
  let m;
  while ((m = valueRe.exec(code)) !== null) seen.push(m);
  if (seen.length === 0) return false;

  // 取每个 literal 命中行的上下 200 字符，看是否在 @Value 段
  const lines = code.split(/\r?\n/);
  const literalLines = [];
  for (let i = 0; i < lines.length; i++) {
    const re = new RegExp(`\\b${literal}\\b`);
    if (re.test(lines[i])) literalLines.push(i);
  }
  for (const li of literalLines) {
    const start = Math.max(0, li - 50);
    const end = Math.min(lines.length, li + 50);
    const ctx = lines.slice(start, end).join('\n');
    if (valueRe.test(ctx)) return true;
  }
  return false;
}

/**
 * 检测字面量所在行是否有上下文关键字（KPI/Days/Rows/Hours/Bonus...）
 * 防止泛整数误报（如数组下标 for (int i=0; i<5; i++)）
 */
function hasContextKeyword(line) {
  const lower = line.toLowerCase();
  return CONTEXT_KEYWORDS.some((k) => lower.includes(k));
}

/**
 * 排除明显非业务参数（数组下标、循环变量、月份）
 */
function isFalsePositive(line, literal) {
  // 数组下标、循环边界：for (int i = 0; i < N; i++)
  if (/for\s*\(.*<\s*\d+/.test(line) && /i\+\+/.test(line)) return true;
  // log/println 计数
  if (/log\.(info|debug|warn|error)\s*\(.*\b\d+\b/.test(line) && line.split(literal).length > 2) return true;
  // return 计数（return N;）— 不算业务参数
  if (/^\s*return\s+\d+\s*;?\s*$/.test(line)) return true;
  // import / package 行
  if (/^\s*(import|package)\b/.test(line)) return true;
  return false;
}

// ---------- 主流程 ----------

function processPayload(payload) {
  const toolName = payload.tool_name || payload.toolName || '';
  if (!/^(Write|Edit|MultiEdit)$/.test(toolName)) return null;

  const filePath = extractFilePath(payload.tool_input || payload.toolInput || {});
  // 仅扫描 ruoyi-ipd 的 service/impl/*.java
  if (!/ruoyi-ipd\/.*\/service\/impl\/[^/]+\.java$/.test(filePath)) return null;

  // 单元测试路径 → 静默跳过（service/impl 是 main，通常不会有 test 路径，但防御性兜底）
  if (/src\/test\//.test(filePath)) return null;

  const rawContent = extractContent(payload.tool_input || payload.toolInput || {});
  if (!rawContent || rawContent.length > 50 * 1024) {
    // 5KB / 50KB 上限防御（payload 远小于文件，正常不超过 50KB）
    return null;
  }
  const code = stripCommentsAndStrings(rawContent);

  const findings = [];

  for (const cfg of CONFIG_DEFAULTS) {
    const re = cfg.pattern;
    let m;
    re.lastIndex = 0;
    while ((m = re.exec(code)) !== null) {
      const literal = m[0];
      const idx = m.index;
      // 找所在行
      const before = code.slice(0, idx);
      const lineNo = (before.match(/\n/g) || []).length + 1;
      const lines = code.split(/\r?\n/);
      const line = lines[lineNo - 1] || '';

      // 豁免 1：上下文无 KPI/Days/Bonus 等
      if (!hasContextKeyword(line)) continue;
      // 豁免 2：明显误报（数组下标/循环/return）
      if (isFalsePositive(line, literal)) continue;
      // 豁免 3：@Value 同一上下文
      if (hasValueAnnotation(code, literal)) continue;

      findings.push({
        key: cfg.key,
        expected_default: cfg.value,
        literal,
        line: lineNo,
        line_excerpt: line.trim().slice(0, 120),
      });
      // 每个 pattern 在一次 payload 中只报 1 次（防刷屏）
      break;
    }
  }

  return { filePath, findings };
}

function emitReport(report) {
  if (!report || report.findings.length === 0) return;

  const lines = [];
  lines.push(
    `\n[hardcoded-config-guard] WARN ${report.filePath} 检出 ${report.findings.length} 处业务参数硬编码，建议改走 BusinessConfigService:`,
  );
  for (const f of report.findings) {
    lines.push(
      `  - line ${f.line}: 字面量 ${f.literal} → BusinessConfigKeys.${suggestKeyConstant(f.key)} (默认 ${f.expected_default})`,
    );
    lines.push(`      └─ ${f.line_excerpt}`);
  }
  lines.push('  解决方案: BusinessConfigService.getDouble/getInt(...) + BusinessConfigKeys.XXX 常量');
  lines.push('  关联卡: ROOT-R1 业务参数配置化根治');
  lines.push('');

  process.stderr.write(lines.join('\n') + '\n');

  const jsonReport = {
    hook: 'hardcoded-config-guard.cjs',
    timestamp: new Date().toISOString(),
    file: report.filePath,
    findings: report.findings,
    policy: '业务参数走 BusinessConfigService，禁止 service/impl 字面量',
  };
  process.stderr.write('\n---JSON---\n' + JSON.stringify(jsonReport, null, 2) + '\n---END---\n');
}

function suggestKeyConstant(key) {
  // bonus.poolRate → BONUS_POOL_RATE
  return key
    .split(/[.\-_]/)
    .filter(Boolean)
    .map((s) => s.toUpperCase())
    .join('_');
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

  const report = processPayload(payload);
  if (!report) process.exit(0);

  emitReport(report);
  process.exit(0); // WARN 级不阻断
}

main();