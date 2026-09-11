# OPS-09 强化提案：Java/yml 写入 mutex hook

## 问题
多会话并行开发，兄弟会话修改Java/yml导致本会话测试断言/注释修复被覆盖（循环失败）

## 证据
- commit 67b18014 (yml fix) → b25930e6 覆盖回HEAD
- P131: sibling insert→insertBatch重构，测试未同步 (15F+7E)
- P171/P1111/P112: security hardening注解新增→Mockito stub不匹配

## 提案
.claude/hooks/pre-java-yml-write.sh：检测同一文件并发写入，中止并提示请让路
允许 SKIP_CONCURRENT_WRITE=1 紧急绕过

## 成本
单shell脚本 + git status扫描，不影响构建性能

--- R9 2026-09-06