#!/usr/bin/env node
/**
 * ruflo-hook.cjs — cross-platform Node.js port of ruflo-hook.sh (#2132)
 *
 * Deployed to .claude/helpers/ during ruflo init. On Windows, the
 * generated .claude/settings.json hooks point here instead of the
 * plugin's bash-only ruflo-hook.sh.
 *
 * Always exits 0 — hook subcommands are best-effort telemetry and must
 * never block a Claude Code turn.
 */

'use strict';

const { spawnSync, execSync } = require('child_process');
const fs = require('fs');

function done() { process.exit(0); }

function commandExists(cmd) {
  try {
    const r = execSync(
      process.platform === 'win32' ? 'where ' + cmd : 'command -v ' + cmd,
      { encoding: 'utf8', stdio: ['ignore', 'pipe', 'ignore'] }
    );
    return r.trim().length > 0;
  } catch { return false; }
}

function invokeHook(bin, binArgs, hookArgs, stdinData) {
  const args = [...binArgs, ...hookArgs];
  const result = spawnSync(bin, args, {
    shell: process.platform === 'win32',
    input: stdinData || '',
    encoding: 'utf8',
    stdio: ['pipe', 'ignore', 'ignore'],
    timeout: 30_000,
  });
  return result.status === 0;
}

function main() {
  const args = process.argv.slice(2);
  if (args.length === 0) done();

  const [subcommand, ...rest] = args;

  let stdinData = '';
  try { stdinData = fs.readFileSync(0, 'utf8'); } catch { stdinData = ''; }

  const hookArgs = ['hooks', subcommand, ...rest];

  if (commandExists('ruflo')) { invokeHook('ruflo', [], hookArgs, stdinData); done(); }
  if (commandExists('claude-flow')) { invokeHook('claude-flow', [], hookArgs, stdinData); done(); }
  invokeHook('npx', ['--prefer-offline', '--yes', 'ruflo@latest'], hookArgs, stdinData);
  done();
}

main();
