#!/usr/bin/env node
/**
 * sensitive-field-guard.cjs
 * PreToolUse hook: 拦截 Claude 写入敏感字段。
 *
 * 触发条件: Write | Edit | MultiEdit 工具
 * 阻断目标:
 *   1. .env / .env.* 文件（任何环境）
 *   2. application-prod.yml（生产环境配置）
 *   3. 内容里含明文 password / privateKey / jwt-secret-key 字面量（开发环境也警告）
 *   4. 任何 PEM 私钥块
 *
 * 退出码:
 *   0  = 允许
 *   2  = 阻断，stderr 输出阻断原因
 *   其他 = 错误（不阻断）
 */

const path = require('path');

// 敏感文件路径模式（绝对/相对都拦）
const SENSITIVE_PATHS = [
  /(^|\/)\.env(\.|$)/,
  /(^|\/)\.env\.[a-z0-9_-]+/i,
  /(^|\/)application-prod\.ya?ml$/i,
  /(^|\/)application-prod\.properties$/i,
];

// 敏感内容模式（仅 stderr 提醒，不阻断——让 dev 配置改起来顺）
const SENSITIVE_CONTENT_WARN = [
  /jwt[-_]?secret[-_]?key\s*[:=]\s*['"]?[a-zA-Z0-9]{8,}/i,
  /password\s*[:=]\s*['"]?(?!\$\{)[a-zA-Z0-9_!@#$%^&*()-+=]{6,}/i,
  /privateKey\s*[:=]\s*['"]?MII[A-Za-z0-9+/=]{20,}/i,
  /-----BEGIN (RSA |EC |DSA |OPENSSH |)PRIVATE KEY-----/,
];

// 阻断性内容（命中即 exit 2）
const SENSITIVE_CONTENT_BLOCK = [
  /-----BEGIN (RSA |EC |DSA |OPENSSH |)PRIVATE KEY-----[\s\S]*?-----END/,
];

function readStdinSync() {
  try {
    // 直接从 fd 0 读 stdin，避开 process.stdin.read() 在同步上下文里的不可靠性
    const fs = require('fs');
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

function main() {
  const raw = readStdinSync();
  if (!raw.trim()) process.exit(0);

  let payload;
  try {
    payload = JSON.parse(raw);
  } catch (e) {
    // 不是 JSON，不阻断
    process.exit(0);
  }

  const toolName = payload.tool_name || payload.toolName || '';
  if (!/^(Write|Edit|MultiEdit)$/.test(toolName)) process.exit(0);

  const filePath = extractFilePath(payload.tool_input || payload.toolInput || {});
  const content = extractContent(payload.tool_input || payload.toolInput || {});

  // 1. 敏感路径
  for (const re of SENSITIVE_PATHS) {
    if (re.test(filePath)) {
      process.stderr.write(
        `\n[sensitive-field-guard] ⛔ 阻断写入敏感文件: ${filePath}\n` +
        `  理由: 匹配敏感路径模式 ${re}\n` +
        `  处置: 如需写入生产配置，请用户手动确认或使用部署工具。\n\n`,
      );
      process.exit(2);
    }
  }

  // 2. 阻断性内容
  for (const re of SENSITIVE_CONTENT_BLOCK) {
    if (re.test(content)) {
      process.stderr.write(
        `\n[sensitive-field-guard] ⛔ 阻断写入含私钥内容\n` +
        `  文件: ${filePath}\n` +
        `  理由: 匹配模式 ${re}\n` +
        `  处置: 请用环境变量 / 密钥管理服务注入，绝不写入仓库。\n\n`,
      );
      process.exit(2);
    }
  }

  // 3. 警告性内容（不阻断）
  for (const re of SENSITIVE_CONTENT_WARN) {
    if (re.test(content)) {
      process.stderr.write(
        `\n[sensitive-field-guard] ⚠️  警告: ${filePath} 含疑似敏感字面量\n` +
        `  模式: ${re}\n` +
        `  建议: 改用环境变量或密钥管理服务。dev 环境可忽略，生产部署前必须替换。\n\n`,
      );
      // 不阻断，继续
      break;
    }
  }

  process.exit(0);
}

main();