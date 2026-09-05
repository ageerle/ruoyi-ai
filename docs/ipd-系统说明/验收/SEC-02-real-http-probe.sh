#!/usr/bin/env bash
# SEC-02 六身份矩阵真实 HTTP 探针（curl 16039）
# 用法：bash SEC-02-real-http-probe.sh
# 前置：16039 后端在 LISTEN；真实库种子已灌（person 表含以下 6 身份）
# 退出码：0=全部期望命中；非 0=有未关闭残差
set -u
BASE="${BASE:-http://127.0.0.1:16039}"
ROUNDS="${ROUNDS:-1}"
COOKIES="$(mktemp -d)"
trap "rm -rf $COOKIES" EXIT

IDENTITIES=(
  "self_market_pm_a|MARKET_PM_A|pwd"
  "self_market_pm_b|MARKET_PM_B|pwd"
  "self_rd_pm_a|RD_PM_A|pwd"
  "self_group_leader_a|GROUP_LEADER_A|pwd"
  "cross_super_admin|SUPER_ADMIN|pwd"
  "anonymous||"
)

RESOURCES=(
  "self|projects|200"
  "self|products|200"
  "self|stage-actions|200"
  "self|cert-templates|200"
  "self|gate-elements|200"
  "self|deletion-requests|200"
  "anon|projects|401"
  "anon|products|401"
  "anon|auth/me|401"
)

pass=0; fail=0; total=0
for round in $(seq 1 $ROUNDS); do
  echo "=== round $round ==="
  for id in "${IDENTITIES[@]}"; do
    IFS='|' read -r UNAME PWD _ <<<"$id"
    COOKIE="$COOKIES/${UNAME:-anon}_$round.cookie"
    if [ -n "$UNAME" ]; then
      code=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$BASE/api/v1/auth/login" \
        -H 'Content-Type: application/json' \
        -c "$COOKIE" \
        -d "{\"username\":\"$UNAME\",\"password\":\"$PWD\"}")
      if [ "$code" != "200" ]; then
        echo "  [SKIP] $UNAME login HTTP=$code"
        rm -f "$COOKIE"
        continue
      fi
    fi
    for r in "${RESOURCES[@]}"; do
      IFS='|' read -r SCOPE RESOURCE EXPECT _ <<<"$r"
      URL="$BASE/api/v1/$RESOURCE"
      if [ -f "$COOKIE" ]; then
        http=$(curl -s -o /dev/null -w '%{http_code}' -b "$COOKIE" "$URL")
      else
        http=$(curl -s -o /dev/null -w '%{http_code}' "$URL")
      fi
      total=$((total+1))
      if [ "$http" = "$EXPECT" ]; then
        pass=$((pass+1))
        printf '  [OK ] %-22s %-30s %s\n' "$UNAME" "$RESOURCE" "$http"
      else
        fail=$((fail+1))
        printf '  [ERR] %-22s %-30s got=%s expect=%s\n' "$UNAME" "$RESOURCE" "$http" "$EXPECT"
      fi
    done
    rm -f "$COOKIE"
  done
done

echo
echo "==== SEC-02 real HTTP summary: pass=$pass fail=$fail total=$total ===="
[ $fail = 0 ] && exit 0 || exit 1
