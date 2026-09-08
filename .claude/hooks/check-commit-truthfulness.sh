#!/usr/bin/env bash
# IPD commit-msg 真实性门禁（病根 2 根治：commit 治理无门禁）
#
# 安装：
#   cp .claude/hooks/check-commit-truthfulness.sh .git/hooks/commit-msg
#   chmod +x .git/hooks/commit-msg
#
# 拦截规则：
#   1. commit msg 写「feat」但只动 test 文件（无 Service/Controller/Mapper/Domain）
#   2. commit msg 写「整链验收」「真活」「E2E」但实际 Mockito Mock（无 SpringBootTest）
#   3. commit msg 写「全 done」「阶段收口」「22 张细卡全 done」但看板真实有 todo/inprogress
#   4. 翻 done commit 但验收报告缺失（docs/ipd-系统说明/验收/<卡号>-*.md）
#
# 用法：
#   .git/hooks/commit-msg <commit-msg-file>
#
# 退出码：
#   0 = PASS（允许 commit）
#   1 = FAIL（拦截 commit，输出失败原因）
#
# 边界：
#   - 只读探针，不改任何代码
#   - 不动兄弟会话 working tree
#   - 凭证不入版本库

set -euo pipefail

COMMIT_MSG_FILE="${1:-}"
if [[ -z "$COMMIT_MSG_FILE" ]]; then
    echo "用法: $0 <commit-msg-file>"
    exit 1
fi

COMMIT_MSG=$(cat "$COMMIT_MSG_FILE")
REPO_ROOT=$(git rev-parse --show-toplevel)

# 提取 commit type（feat / fix / test / docs / refactor / perf / chore）
COMMIT_TYPE=$(echo "$COMMIT_MSG" | head -1 | grep -oE '^[a-z]+' || echo "unknown")

# 提取卡号（P*-*.* 或 DEF-* 或 QA-* 等）
CARD_ID=$(echo "$COMMIT_MSG" | head -1 | grep -oE '(P[0-9]+-[0-9]+(\.[0-9]+)?|DEF-[0-9]+|QA-[0-9]+|SEC-[0-9]+|OPS-[0-9]+)' | head -1 || echo "")

# 获取 staged 文件列表
STAGED_FILES=$(git diff --cached --name-only --diff-filter=ACM)

# ============================================================================
# 拦截规则 1：commit msg 写「feat」但只动 test 文件（无 Service/Controller/Mapper/Domain）
# ============================================================================
if [[ "$COMMIT_TYPE" == "feat" ]]; then
    HAS_TEST=$(echo "$STAGED_FILES" | grep -cE 'Test\.java$|test/.*\.java$' || true)
    HAS_CODE=$(echo "$STAGED_FILES" | grep -cE 'Service\.java$|Controller\.java$|Mapper\.java$|/domain/.*\.java$|/entity/.*\.java$' || true)

    if [[ "$HAS_TEST" -gt 0 && "$HAS_CODE" -eq 0 ]]; then
        echo "❌ 拦截规则 1：commit msg 写「feat」但只动 test 文件（无 Service/Controller/Mapper/Domain）"
        echo ""
        echo "staged 文件："
        echo "$STAGED_FILES" | sed 's/^/  /'
        echo ""
        echo "根治：commit type 改为「test」（契约测试）或补充 Service/Controller/Mapper 真活代码"
        echo "见 .gitmessage 模板「病根 2 根治」段"
        exit 1
    fi
fi

# ============================================================================
# 拦截规则 2：commit msg 写「整链验收」「真活」「E2E」但实际 Mockito Mock（无 SpringBootTest）
# ============================================================================
if echo "$COMMIT_MSG" | grep -qE '整链验收|真活|E2E|端到端'; then
    HAS_SPRINGBOOT_TEST=$(echo "$STAGED_FILES" | xargs -I {} grep -l "@SpringBootTest" "$REPO_ROOT/{}" 2>/dev/null | wc -l || true)
    HAS_MOCKITO=$(echo "$STAGED_FILES" | xargs -I {} grep -l "MockitoExtension" "$REPO_ROOT/{}" 2>/dev/null | wc -l || true)

    if [[ "$HAS_MOCKITO" -gt 0 && "$HAS_SPRINGBOOT_TEST" -eq 0 ]]; then
        echo "❌ 拦截规则 2：commit msg 写「整链验收/真活/E2E」但实际 Mockito Mock（无 SpringBootTest）"
        echo ""
        echo "Mockito Mock 文件："
        echo "$STAGED_FILES" | xargs -I {} grep -l "MockitoExtension" "$REPO_ROOT/{}" 2>/dev/null | sed 's/^/  /'
        echo ""
        echo "根治：补充 SpringBootTest 真活集成测试（extends IpdIntegrationTestBase）"
        echo "见 ruoyi-modules/ruoyi-ipd/src/test/java/org/ruoyi/ipd/integration/IpdIntegrationTestBase.java"
        exit 1
    fi
fi

# ============================================================================
# 拦截规则 3：commit msg 写「全 done」「阶段收口」但看板真实有 todo/inprogress
# ============================================================================
if echo "$COMMIT_MSG" | grep -qE '全 done|全done|阶段收口|细卡全|全部 done'; then
    # fresh 拉看板验证（需要 python3 + 看板可达）
    if command -v python3 &>/dev/null; then
        BOARD_STATUS=$(python3 -c "
import json, urllib.request
try:
    BOARD='http://127.0.0.1:62250'
    PID='01dcf15c-86bb-4c7b-957c-8fe44bddd10d'
    tasks=json.loads(urllib.request.urlopen(f'{BOARD}/api/tasks?project_id={PID}', timeout=5).read())['data']
    from collections import Counter
    c=Counter(t.get('status','?') for t in tasks)
    print(f\"{c.get('todo',0)} {c.get('inprogress',0)} {c.get('inreview',0)}\")
except Exception as e:
    print('ERROR')
" 2>/dev/null || echo "ERROR")

        if [[ "$BOARD_STATUS" != "ERROR" ]]; then
            TODO_COUNT=$(echo "$BOARD_STATUS" | awk '{print $1}')
            INPROGRESS_COUNT=$(echo "$BOARD_STATUS" | awk '{print $2}')
            INREVIEW_COUNT=$(echo "$BOARD_STATUS" | awk '{print $3}')

            if [[ "$TODO_COUNT" -gt 0 || "$INPROGRESS_COUNT" -gt 0 || "$INREVIEW_COUNT" -gt 0 ]]; then
                echo "❌ 拦截规则 3：commit msg 写「全 done/阶段收口」但看板真实有 todo/inprogress/inreview"
                echo ""
                echo "看板 fresh 状态："
                echo "  todo:       $TODO_COUNT"
                echo "  inprogress: $INPROGRESS_COUNT"
                echo "  inreview:   $INREVIEW_COUNT"
                echo ""
                echo "根治：fresh 拉看板验证（scripts/check-mirror-vs-board.py）+ 修正 commit msg"
                exit 1
            fi
        fi
    fi
fi

# ============================================================================
# 拦截规则 4：翻 done commit 但验收报告缺失
# ============================================================================
if echo "$COMMIT_MSG" | grep -qE '翻 done|翻done|转 done|转done|收口.*done' && [[ -n "$CARD_ID" ]]; then
    # 检查验收报告是否存在
    REPORT_PATTERN_1="${REPO_ROOT}/docs/ipd-系统说明/验收/${CARD_ID}-*.md"
    REPORT_PATTERN_2="${REPO_ROOT}/docs/ipd-系统说明/验收/${CARD_ID,,}-*.md"
    REPORT_PATTERN_3="${REPO_ROOT}/docs/ipd-系统说明/验收/$(echo "$CARD_ID" | tr '.' '_')-*.md"

    REPORT_EXISTS=0
    for pat in "$REPORT_PATTERN_1" "$REPORT_PATTERN_2" "$REPORT_PATTERN_3"; do
        if compgen -G "$pat" > /dev/null; then
            REPORT_EXISTS=1
            break
        fi
    done

    if [[ "$REPORT_EXISTS" -eq 0 ]]; then
        echo "❌ 拦截规则 4：翻 done commit 但验收报告缺失"
        echo ""
        echo "卡号: $CARD_ID"
        echo "搜索模式:"
        echo "  $REPORT_PATTERN_1"
        echo "  $REPORT_PATTERN_2"
        echo "  $REPORT_PATTERN_3"
        echo ""
        echo "根治：补充验收报告 docs/ipd-系统说明/验收/${CARD_ID}-*.md"
        echo "见 scripts/check-done-gate.py 翻 done 硬门禁脚本"
        exit 1
    fi
fi

# ============================================================================
# 全部通过
# ============================================================================
echo "✓ commit-msg 真实性门禁 PASS"
echo "  type: $COMMIT_TYPE"
echo "  card: ${CARD_ID:-N/A}"
echo "  staged files: $(echo "$STAGED_FILES" | wc -l | tr -d ' ')"
exit 0
