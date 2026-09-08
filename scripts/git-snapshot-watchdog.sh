#!/bin/sh
# 分支回退看门狗：git reset 不触发任何 git hook（2026-09-08 git 2.50 实测），
# reference-transaction 只能覆盖 branch -f / update-ref / 删分支；本看门狗
# 每 N 秒核对受监视分支 tip 补上 reset 兜底：
#   1. tip 前进时：滚动更新 refs/backup/<分支>（单一 ref，永远指向最近一次核对的 tip）；
#   2. tip 回退/分叉移动时：把移动前 tip 额外快照到 refs/snapshot/<分支>-wd-<时间戳>。
# 被 reset 的 commit 因此保持可达、不被 GC，随时可查 refs/backup/ 或 refs/snapshot/ 恢复。
#
# 用法（每个 clone 建议常驻）：
#   nohup sh scripts/git-snapshot-watchdog.sh [间隔秒，默认20] >/tmp/git-watchdog.log 2>&1 &
# 受监视分支：.git/watchdog-branches 文件（每行一个短分支名，默认 main）。
set -u
REPO=$(git -C "$(pwd)" rev-parse --show-toplevel 2>/dev/null) || exit 1
cd "$REPO" || exit 1
INTERVAL="${1:-20}"
BRANCHES_FILE="$REPO/.git/watchdog-branches"
[ -f "$BRANCHES_FILE" ] || echo "main" > "$BRANCHES_FILE"
ZERO=0000000000000000000000000000000000000000
echo "$(date '+%F %T') watchdog 启动: repo=$REPO interval=${INTERVAL}s branches=$(tr '\n' ' ' < "$BRANCHES_FILE")"
while :; do
  while IFS= read -r br; do
    [ -n "$br" ] || continue
    ref="refs/heads/$br"
    tip=$(git rev-parse --verify -q "$ref" 2>/dev/null) || continue
    [ "$tip" != "$ZERO" ] || continue
    last=$(git config --get "watchdog.last.$br" 2>/dev/null) || last=""
    if [ -n "$last" ] && [ "$last" != "$ZERO" ] && [ "$last" != "$tip" ]; then
      if git merge-base --is-ancestor "$last" "$tip" 2>/dev/null; then
        # 正常前进：滚动备份 ref 更新到新 tip
        git update-ref "refs/backup/$br" "$tip" 2>/dev/null || true
      else
        # 回退/分叉移动：快照移动前 tip + 滚动备份同步
        ts=$(date +%Y%m%dT%H%M%S)
        snap="refs/snapshot/$br-wd-$ts"
        git update-ref "$snap" "$last" 2>/dev/null || true
        git update-ref "refs/backup/$br" "$tip" 2>/dev/null || true
        echo "$(date '+%F %T') [watchdog] $br 回退检测: $last -> $tip；移动前 tip 已快照 -> $snap" >&2
      fi
    elif [ -z "$last" ] || [ "$last" = "$ZERO" ]; then
      # 首次记录基线，同时建立滚动备份
      git update-ref "refs/backup/$br" "$tip" 2>/dev/null || true
    fi
    git config "watchdog.last.$br" "$tip"
  done < "$BRANCHES_FILE"
  sleep "$INTERVAL"
done
