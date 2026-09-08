#!/bin/sh
# 启用仓库级 git 护栏（.githooks/）：拦截 main 直提 + 分支回退自动快照 + 拦 force push main。
# core.hooksPath 是 clone 本地配置，每个 clone 只需执行一次本脚本。
# 生效后 .git/hooks/ 整体失效，repowise post-commit 已搬迁至 .githooks/post-commit。
set -e
root=$(git rev-parse --show-toplevel)
git -C "$root" config core.hooksPath .githooks
chmod +x "$root"/.githooks/* 2>/dev/null || true
echo "core.hooksPath = $(git -C "$root" config core.hooksPath) —— 护栏已启用（本 clone 全部 git 客户端生效）"
