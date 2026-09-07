#!/usr/bin/env node
/**
 * acceptance-matrix-validate.cjs
 * 双向校验 acceptance-matrix.json：
 *   正向：matrix 中的 unitTestClass 在 src/test/java 下能找到对应 .java 文件
 *         matrix 中的 docRef 在 docs/ 下能找到对应 .md 文件
 *         status=covered 的 AC 必须有 linkedCommits 且长度 ≥ 1
 *         ac_id 必须符合 ^AC-(INC|EXT|MIN)-\d+[a-z]?$
 *   反向：src/test/java 下的测试类若 @DisplayName 含 AC-INC-* 编号，必须在 matrix 中
 *         出现（owner OD-AM-01 决策前用「弱反向」：仅 WARN 不阻断）
 *
 * 触发：本地 `node .claude/helpers/acceptance-matrix-validate.cjs`
 *      CI：.github/workflows/docs-link-check.yml 周日 02:00 + PR 触发
 * 退出码：0 = 全绿，1 = 硬错误，2 = 软警告
 */

const fs = require('fs');
const path = require('path');

const ROOT = path.resolve(__dirname, '..', '..');
const MATRIX_PATH = path.join(ROOT, 'docs/ipd-系统说明/治理/acceptance-matrix.json');
const SCHEMA_PATH = path.join(ROOT, 'docs/ipd-系统说明/治理/acceptance-matrix.schema.json');
const TEST_DIR = path.join(ROOT, 'ruoyi-modules/ruoyi-ipd/src/test/java');
const DOCS_DIR = path.join(ROOT, 'docs');

let exitCode = 0;
const errors = [];
const warnings = [];

function log(level, msg) {
  if (level === 'ERROR') {
    errors.push(msg);
    console.error('[ERROR] ' + msg);
  } else if (level === 'WARN') {
    warnings.push(msg);
    console.error('[WARN]  ' + msg);
  } else {
    console.log(msg);
  }
}

function readJson(p) {
  return JSON.parse(fs.readFileSync(p, 'utf8'));
}

function listFiles(dir, suffix) {
  const out = [];
  if (!fs.existsSync(dir)) return out;
  const walk = (d) => {
    for (const ent of fs.readdirSync(d, { withFileTypes: true })) {
      const fp = path.join(d, ent.name);
      if (ent.isDirectory()) walk(fp);
      else if (ent.name.endsWith(suffix)) out.push(fp);
    }
  };
  walk(dir);
  return out;
}

function classFqnToPath(fqn) {
  // org.ruoyi.ipd.service.X#testY -> .../ruoyi-ipd/src/test/java/org/ruoyi/ipd/service/X.java
  // 去掉 #methodName 后缀
  const classOnly = fqn.split('#')[0];
  const parts = classOnly.split('.');
  const cls = parts.pop();
  return path.join(TEST_DIR, ...parts, cls + '.java');
}

function checkMatrix(matrix) {
  const acIds = new Set();
  for (const row of matrix.rows || []) {
    // 1. ac_id 唯一
    if (acIds.has(row.ac_id)) {
      log('ERROR', `重复 ac_id: ${row.ac_id}`);
      continue;
    }
    acIds.add(row.ac_id);

    // 2. ac_id 格式
    if (!/^AC-(INC|EXT|MIN|AUTH|AUD|ENV|GATE|GLB|CFG|PROD)-\d+[a-z]?$/.test(row.ac_id)) {
      log('ERROR', `ac_id 格式不合规: ${row.ac_id}`);
    }

    // 3. category 与 ac_id 前缀一致
    const prefix = row.ac_id.split('-')[1];
    if (row.category && row.category !== prefix) {
      log('ERROR', `category=${row.category} 与 ac_id=${row.ac_id} 前缀不一致`);
    }

    // 4. unitTestClass 存在
    if (row.unitTestClass) {
      const tp = classFqnToPath(row.unitTestClass);
      if (!fs.existsSync(tp)) {
        log('ERROR', `unitTestClass 文件不存在: ${row.ac_id} → ${row.unitTestClass} → ${tp}`);
      }
    } else if (row.status === 'covered') {
      log('ERROR', `status=covered 但 unitTestClass 为空: ${row.ac_id}`);
    }

    // 5. docRef 存在
    if (row.docRef) {
      const dp = path.join(ROOT, row.docRef);
      if (!fs.existsSync(dp)) {
        log('ERROR', `docRef 文件不存在: ${row.ac_id} → ${row.docRef}`);
      }
    } else {
      log('WARN', `docRef 缺失: ${row.ac_id}`);
    }

    // 6. status=covered 必须有 linkedCommits ≥ 1
    if (row.status === 'covered' && (!row.linkedCommits || row.linkedCommits.length < 1)) {
      log('ERROR', `status=covered 但 linkedCommits 为空: ${row.ac_id}`);
    }

    // 7. linkedCommits 格式
    for (const c of row.linkedCommits || []) {
      if (!/^[0-9a-f]{7,40}$/.test(c)) {
        log('ERROR', `linkedCommits 格式错: ${row.ac_id} → ${c}`);
      }
    }
  }
  return acIds;
}

function checkReverse(matrix, acIds) {
  // 软反向：扫描测试类 @DisplayName 含 AC-INC-* 编号但不在 matrix 中 → WARN
  const javaFiles = listFiles(TEST_DIR, '.java');
  const referencedInDisplayName = new Set();
  const dispRe = /@DisplayName\s*\(\s*"([^"]*)"\s*\)/g;
  const acRe = /AC-(INC|EXT|MIN)-\d+[a-z]?/g;
  for (const f of javaFiles) {
    const src = fs.readFileSync(f, 'utf8');
    let m;
    while ((m = dispRe.exec(src)) !== null) {
      let am;
      while ((am = acRe.exec(m[1])) !== null) {
        referencedInDisplayName.add(am[0]);
      }
    }
  }
  for (const ac of referencedInDisplayName) {
    if (!acIds.has(ac)) {
      log('WARN', `@DisplayName 引用 ${ac} 但 acceptance-matrix.json 未收录（OD-AM-01 待 owner 决策）`);
    }
  }
}

function main() {
  if (!fs.existsSync(MATRIX_PATH)) {
    log('ERROR', `acceptance-matrix.json 不存在: ${MATRIX_PATH}`);
    process.exit(1);
  }
  let matrix;
  try {
    matrix = readJson(MATRIX_PATH);
  } catch (e) {
    log('ERROR', `acceptance-matrix.json JSON 解析失败: ${e.message}`);
    process.exit(1);
  }

  if (fs.existsSync(SCHEMA_PATH)) {
    log('INFO', `schema 存在: ${path.relative(ROOT, SCHEMA_PATH)}（CI 阶段用 ajv 强校验）`);
  } else {
    log('WARN', `schema 缺失: ${path.relative(ROOT, SCHEMA_PATH)}`);
  }

  log('INFO', `acceptance-matrix.json 加载：${(matrix.rows || []).length} 行（version=${matrix.version}）`);

  const acIds = checkMatrix(matrix);
  checkReverse(matrix, acIds);

  console.log('');
  console.log('=== acceptance-matrix-validate 总结 ===');
  console.log(`  rows:        ${(matrix.rows || []).length}`);
  console.log(`  covered:     ${(matrix.rows || []).filter(r => r.status === 'covered').length}`);
  console.log(`  partial:     ${(matrix.rows || []).filter(r => r.status === 'partial').length}`);
  console.log(`  blocked:     ${(matrix.rows || []).filter(r => r.status === 'blocked').length}`);
  console.log(`  manual:      ${(matrix.rows || []).filter(r => r.status === 'manual').length}`);
  console.log(`  deprecated:  ${(matrix.rows || []).filter(r => r.status === 'deprecated').length}`);
  console.log(`  errors:      ${errors.length}`);
  console.log(`  warnings:    ${warnings.length}`);

  if (errors.length > 0) exitCode = 1;
  else if (warnings.length > 0) exitCode = 2;
  process.exit(exitCode);
}

main();
