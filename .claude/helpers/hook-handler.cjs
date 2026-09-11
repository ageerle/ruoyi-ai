#!/usr/bin/env node
/**
 * Minimal Cursor-compatible hook handler.
 * Emits valid JSON so PreToolUse/PostToolUse are not blocked.
 * Replaces the Claude Flow handler that printed plain text (invalid JSON).
 */
'use strict';

const command = process.argv[2] || '';

function emit(obj) {
  process.stdout.write(JSON.stringify(obj) + '\n');
}

// Dangerous-command gate for pre-bash (best-effort; stdin may be empty).
if (command === 'pre-bash') {
  let stdin = '';
  try {
    stdin = require('fs').readFileSync(0, 'utf8');
  } catch (_) { /* no stdin */ }
  const cmd = String(stdin || '').toLowerCase();
  const dangerous = ['rm -rf /', 'format c:', 'del /s /q c:\\', ':(){:|:&};:'];
  for (const d of dangerous) {
    if (cmd.includes(d)) {
      emit({
        permissionDecision: 'deny',
        permissionDecisionReason: `Dangerous command detected: ${d}`,
      });
      process.exit(2);
    }
  }
  emit({ permissionDecision: 'allow' });
  process.exit(0);
}

if (command === 'pre-edit' || command === 'post-edit') {
  // Human-readable logs must go to stderr; last stdout line must be JSON.
  console.error('[OK] edit hook:', command);
  emit({ continue: true, permissionDecision: 'allow' });
  process.exit(0);
}

// All other hook events: acknowledge with continue.
emit({ continue: true });
process.exit(0);
