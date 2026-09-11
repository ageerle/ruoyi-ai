#!/bin/sh
# OPS-09 PostToolUse：记录本会话成功编辑的 java/yml 文件 mtime，供 pre 守卫区分「自己连续编辑」
set -u
D="${CLAUDE_PROJECT_DIR:-$(cd "$(dirname "$0")/../.." && pwd)}"
INPUT=$(cat 2>/dev/null || true); [ -n "$INPUT" ] || exit 0
FILE=$(printf '%s' "$INPUT" | sed -n 's/.*"file_path"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' | head -1)
[ -n "$FILE" ] || exit 0
case "$FILE" in *.java|*.yml|*.yaml) ;; *) exit 0 ;; esac
[ -f "$FILE" ] || exit 0
SESS=$(printf '%s' "$INPUT" | sed -n 's/.*"session_id"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' | head -1)
[ -n "$SESS" ] || SESS=unknown
STATE_DIR="$D/.claude/hooks/.ops09"; mkdir -p "$STATE_DIR" 2>/dev/null || exit 0
ABS=$(cd "$(dirname "$FILE")" && pwd)/$(basename "$FILE") || exit 0
MT=$(stat -f %m "$ABS" 2>/dev/null || stat -c %Y "$ABS" 2>/dev/null || echo 0)
grep -v "= $ABS" "$STATE_DIR/${SESS}.files" 2>/dev/null > "$STATE_DIR/${SESS}.files.tmp" && mv "$STATE_DIR/${SESS}.files.tmp" "$STATE_DIR/${SESS}.files"
echo "$MT = $ABS" >> "$STATE_DIR/${SESS}.files"
exit 0
