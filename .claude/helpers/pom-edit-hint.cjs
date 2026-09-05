#!/usr/bin/env node
/**
 * pom-edit-hint.cjs
 * PostToolUse hook: Claude 写入 Maven pom.xml 时给出同步检查提示。
 *
 * 触发条件: Write | Edit | MultiEdit 工具命中 pom.xml
 * 行为: stderr 输出提示，不阻断（exit 0）。
 *
 * 提示内容（基于 ruoyi-ai 已知坑点）:
 *   - 多套 BOM 版本对齐（langchain4j stable/community/beta）
 *   - annotation processor 同步
 *   - grpc-bom 强制版本（Milvus SDK 兼容）
 *   - flatten-maven-plugin 影响 CI 版本号
 */

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

  const filePath = extractFilePath(payload.tool_input || payload.toolInput || {});
  if (!/pom\.xml$/.test(filePath)) process.exit(0);

  const content = extractContent(payload.tool_input || payload.toolInput || {});
  const hints = [];

  // 1. langchain4j 多 BOM 对齐
  if (/<langchain4j[._]?[a-z]*\.version>/i.test(content)) {
    hints.push(
      '• langchain4j 有 3 套 BOM（stable / community / beta），确认版本号同步对齐。',
    );
  }

  // 2. annotation processor 同步
  if (/<artifactId>maven-compiler-plugin<\/artifactId>/.test(content) &&
      /annotationProcessorPaths/.test(content)) {
    hints.push(
      '• 改 maven-compiler-plugin 时，确认 annotationProcessorPaths 里的 5 个处理器（therapi-runtime-javadoc-scribe / lombok / spring-boot-configuration-processor / mapstruct-plus-processor / lombok-mapstruct-binding）都在。',
    );
  }

  // 3. grpc 版本（Milvus SDK 兼容）
  if (/<grpc[._]?[a-z]*\.version>/i.test(content) ||
      /<artifactId>grpc-bom<\/artifactId>/.test(content)) {
    hints.push(
      '• grpc-bom 1.62.2 是为了兼容 Milvus SDK；升级前确认 Milvus 客户端已测过新版本。',
    );
  }

  // 4. flatten-maven-plugin
  if (/<artifactId>flatten-maven-plugin<\/artifactId>/.test(content)) {
    hints.push(
      '• flatten-maven-plugin 影响 CI 部署版本号（用 \${revision} 占位），改了 flattenMode 要重新发布。',
    );
  }

  // 5. surefire groups 过滤
  if (/<artifactId>maven-surefire-plugin<\/artifactId>/.test(content)) {
    hints.push(
      '• surefire 配了 <groups>\${profiles.active}</groups>——新增测试必须加 @Tag("dev") 才会跑。',
    );
  }

  if (hints.length === 0) process.exit(0);

  process.stderr.write(
    `\n[pom-edit-hint] 📝 ${filePath} 改动后的同步检查项:\n` +
      hints.join('\n') +
      '\n\n',
  );

  process.exit(0);
}

main();