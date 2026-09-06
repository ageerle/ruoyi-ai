#!/usr/bin/env bash
# check-ipd-frontend-drift.sh
# IPD 前端工程漂移自检（CI / 手动 / 治理轮均可调用）
# 检查项:
#   1. api/ipd/*.ts 间是否存在同名导出（function / const / interface / type）
#   2. views/ipd/<domain>/<domain>-error.ts 类文件是否仍存在（应被 _shared 取代）
#   3. api/ipd/product-group.ts 是否仍存在（应被 api/ipd/product.ts 取代）
#   4. views/ipd/**/*.vue 中占位文案手写（"BackendPending" 文本未走 _shared 组件）
#
# 退出码:
#   0  = 无漂移
#   1  = 检测到漂移
#   2  = 脚本错误
#
# 用法:
#   ./scripts/check-ipd-frontend-drift.sh
#   ./scripts/check-ipd-frontend-drift.sh --strict   # 把警告也升级为错误

set -u  # 不开 -e：grep 找不到匹配时返回 1 是正常的

FRONTEND_ROOT="/Users/mac/Documents/ruoyi-ipd-web"
API_DIR="${FRONTEND_ROOT}/apps/web-antd/src/api/ipd"
VIEWS_DIR="${FRONTEND_ROOT}/apps/web-antd/src/views/ipd"
SHARED_ERROR="${VIEWS_DIR}/_shared/ipd-error-text.ts"

STRICT=0
[ "${1:-}" = "--strict" ] && STRICT=1

problems=0
warnings=0

if [ ! -d "$FRONTEND_ROOT" ]; then
  echo "[check-ipd-frontend-drift] ❌ 前端工程不存在: $FRONTEND_ROOT" >&2
  exit 2
fi

echo "==== IPD 前端漂移自检 ===="
echo "前端根: $FRONTEND_ROOT"
echo

# ---- 1. api/ipd/*.ts 同名导出 ----
echo "[1/4] 检查 api/ipd/*.ts 同名导出..."
TMPDIR_CHECK=$(mktemp -d)
trap 'rm -rf "$TMPDIR_CHECK"' EXIT

# 允许空匹配：set +e 局部开关
set +e
for f in "$API_DIR"/*.ts; do
  [ -f "$f" ] || continue
  fname=$(basename "$f")
  case "$fname" in
    *.test.ts) continue ;;
  esac
  # 提取所有 export 名称（function/const/interface/type）
  grep -oE 'export[[:space:]]+(async[[:space:]]+function|function|const|interface|type)[[:space:]]+[A-Za-z0-9_]+' "$f" 2>/dev/null \
    | awk '{print $NF}' \
    | sort -u > "$TMPDIR_CHECK/$fname.exports" 2>/dev/null
done

# 跨文件重复检测
all_exports=$(cat "$TMPDIR_CHECK"/*.exports 2>/dev/null | sort | uniq -c | sort -rn 2>/dev/null)
duplicates=$(echo "$all_exports" | awk '$1 > 1 {print $2}' 2>/dev/null)
set -e

if [ -n "$duplicates" ]; then
  echo "  ⛔ 重复导出的 Symbol:"
  for dup in $duplicates; do
    files=$(for fexp in "$TMPDIR_CHECK"/*.exports; do
      if grep -qx "$dup" "$fexp" 2>/dev/null; then
        echo -n "api/ipd/$(basename "$fexp" .exports) "
      fi
    done)
    echo "    - $dup  in  $files"
    problems=$((problems + 1))
  done
else
  echo "  ✅ 无重复导出"
fi
echo

# ---- 2. views/ipd/<domain>/<domain>-error.ts ----
echo "[2/4] 检查 views/ipd/<domain>/<domain>-error.ts..."
set +e
error_files=$(find "$VIEWS_DIR" -mindepth 2 -maxdepth 2 -name '*-error.ts' -not -path '*/_shared/*' 2>/dev/null)
real_error_files=""
if [ -n "$error_files" ]; then
  for ef in $error_files; do
    # 判定是否为纯 re-export：内容只含 import + export { ... } from，禁出现裸 function/const 实现
    if grep -qE '^(function|const|let|var|class)[[:space:]]' "$ef" 2>/dev/null; then
      real_error_files="$real_error_files $ef"
    fi
  done
fi
if [ -n "$real_error_files" ]; then
  echo "  ⛔ 发现分散错误码映射文件（应迁入 _shared/ipd-error-text.ts）:"
  for ef in $real_error_files; do
    rel=${ef#$FRONTEND_ROOT/}
    echo "    - $rel"
    problems=$((problems + 1))
  done
  if [ ! -f "$SHARED_ERROR" ]; then
    echo "  ⛔ 共享错误码文件不存在: $SHARED_ERROR"
    problems=$((problems + 1))
  fi
else
  if [ -n "$error_files" ]; then
    echo "  ✅ 无分散错误码映射（已全部转为 _shared 重导出）"
  else
    echo "  ✅ 无分散错误码映射文件"
  fi
fi
set -e
echo

# ---- 3. api/ipd/product-group.ts ----
echo "[3/4] 检查 api/ipd/product-group.ts..."
if [ -f "$API_DIR/product-group.ts" ]; then
  if grep -q '@deprecated' "$API_DIR/product-group.ts" 2>/dev/null; then
    echo "  ✅ product-group.ts 已标注 @deprecated（兼容层）"
  else
    echo "  ⛔ product-group.ts 仍存在但未标注 @deprecated，强制阻断"
    problems=$((problems + 1))
  fi
else
  echo "  ✅ product-group.ts 已清理"
fi
echo

# ---- 4. views/ipd/**/*.vue 中手写 BackendPending 文案（粗扫描）----
echo "[4/4] 检查手写 BackendPending 占位..."
# 精准匹配：仅看 .vue 模板里有没有 _shared/backend-pending.vue 的引用。
# 反向：存在手写"待后端"提示但未引用共享组件的页面。
# 排除 _shared/backend-pending.vue 自身（它就是共享组件，本就含这些字符串）。
ad_hoc_pending=$(grep -lE '待.*后端|后端.*未交付|等.*后端.*API|等.*后端.*端点' "$VIEWS_DIR" --include='*.vue' -r 2>/dev/null \
  | grep -v '_shared/backend-pending\.vue' \
  | xargs -I{} grep -L 'backend-pending' {} 2>/dev/null || true)
if [ -n "$ad_hoc_pending" ]; then
  echo "  ⚠️  发现手写占位文案但未引用 _shared/backend-pending.vue 的页面:"
  for vp in $ad_hoc_pending; do
    rel=${vp#$FRONTEND_ROOT/}
    echo "    - $rel"
    warnings=$((warnings + 1))
  done
  if [ "$STRICT" = "1" ]; then
    problems=$((problems + $(echo "$ad_hoc_pending" | wc -l | tr -d ' ')))
  fi
else
  echo "  ✅ 占位文案走共享组件"
fi
echo

echo "==== 总结 ===="
echo "  阻断性问题: $problems"
echo "  警告:        $warnings"

if [ "$problems" -gt 0 ]; then
  echo
  echo "❌ 检测到漂移。处置: 修复后重跑；或先在 _shared/ 注册再用。详见 docs/ipd-系统说明/前端架构规约-20260906.md"
  exit 1
fi

if [ "$warnings" -gt 0 ] && [ "$STRICT" = "1" ]; then
  echo
  echo "⚠️  --strict 模式下警告升级为错误"
  exit 1
fi

echo
echo "✅ 全部检查通过"
exit 0
