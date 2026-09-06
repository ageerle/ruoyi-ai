#!/bin/sh
# OPS-09 并发写守卫（R9-ops09-mutex-proposal 落地，R9c 2026-09-05）
# 目标：多会话共工同一工作树时，拦截「覆盖兄弟会话在途 Java/yml 编辑」的写操作。
# 协议：Claude Code PreToolUse hook，stdin=JSON{session_id,tool_input.file_path}；exit 2=阻断。
# 绕过：SKIP_CONCURRENT_WRITE=1（紧急通道，绕过行为请在 log.md 登记）。
set -u
D="${CLAUDE_PROJECT_DIR:-$(cd "$(dirname "$0")/../.." && pwd)}"
SKIP="${SKIP_CONCURRENT_WRITE:-0}"

INPUT=$(cat 2>/dev/null || true)
[ -n "$INPUT" ] || exit 0

FILE=$(printf '%s' "$INPUT" | sed -n 's/.*"file_path"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' | head -1)
[ -n "$FILE" ] || exit 0

case "$FILE" in
  *.java|*.yml|*.yaml) ;;
  *) exit 0 ;;
esac

[ -f "$FILE" ] || exit 0            # 新建文件不拦
[ "$SKIP" = "1" ] && exit 0

# 未纳入 git 管理的文件不拦（本地配置等）
git -C "$D" ls-files --error-unmatch -- "$FILE" >/dev/null 2>&1 || exit 0

# 未 modified 的文件不拦（干净文件的首个写入者天然获得锁）
git -C "$D" diff --quiet -- "$FILE" 2>/dev/null && exit 0

SESS=$(printf '%s' "$INPUT" | sed -n 's/.*"session_id"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' | head -1)
[ -n "$SESS" ] || SESS=unknown
STATE_DIR="$D/.claude/hooks/.ops09"
mkdir -p "$STATE_DIR" 2>/dev/null || exit 0
STATE="$STATE_DIR/${SESS}.files"
touch "$STATE" 2>/dev/null || exit 0

ABS=$(cd "$(dirname "$FILE")" 2>/dev/null && pwd)/$(basename "$FILE") || exit 0
KNOWN=$(grep -F "= $ABS" "$STATE" 2>/dev/null | tail -1 | cut -d= -f1)
if [ -n "$KNOWN" ]; then
  CUR=$(stat -f %m "$ABS" 2>/dev/null || stat -c %Y "$ABS" 2>/dev/null || echo 0)
  [ "$CUR" = "$KNOWN" ] && exit 0   # mtime 与本会话上次写入一致 → 是自己的连续编辑
fi

echo "OPS-09 并发写守卫：$FILE 已被其他会话修改（git modified 且非本会话 $SESS 的连续编辑）。" >&2
echo "请先 git diff 复核是否兄弟在途工作；确需覆盖：SKIP_CONCURRENT_WRITE=1 绕过并在 docs/ipd-系统说明/log.md 登记。" >&2
exit 2
