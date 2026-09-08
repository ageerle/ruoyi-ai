#!/bin/bash
# =============================================================================
# IPD 产品经理管理系统 — 生产环境启动脚本
# =============================================================================
# 选型说明（JVM GC 与堆参数）：
#   - 垃圾回收器：G1GC（Garbage-First）
#     * 选择原因：业务节奏以 200ms 为最大停顿目标（MaxGCPauseMillis=200），
#       适配 ruoyi-admin 的 IPD 工作流 / KPI / 奖金池核算等中延迟场景；
#       G1 在 2-8GB 堆区间综合吞吐与延迟最优，且支持 region 化预测模型。
#   - 备选方案（owner 决策触发）：
#       1) ZGC：当堆 > 32GB 或对停顿 < 10ms 有强需求时切换，命令：
#            -XX:+UseZGC -XX:+ZGenerational
#       2) ParallelGC：批处理 / 离线计算场景，吞吐优先，停顿可放宽至秒级。
#   - 堆大小：Xms=Xmx 固定 2GB-4GB 区间（与 application-prod.yml hikariCP
#              maxPoolSize=80 + 平均连接持有 < 1s 的工作负载对齐）。
#   - OOM 兜底：HeapDumpOnOutOfMemoryError + HeapDumpPath=./logs/，
#              OOM 时直接落盘 hprof，便于事后 MAT 分析内存泄漏根因。
#              ⚠️ 仅 dev profile 启用 OOM dump；prod profile 依赖监控告警（参见 OPS-06），
#              避免 hprof 泄露应用内存快照（含潜在敏感数据）。
# =============================================================================
# 使用方式：
#   1) cd <ruoyi-ai 部署根目录>  （必须含 ruoyi-admin.jar）
#   2) SPRING_PROFILES_ACTIVE=prod bash scripts/start.sh
#   3) SIGTERM 优雅退出；OOM 会自动写 ./logs/java_*.hprof 后退出非零
# =============================================================================

set -euo pipefail

# 颜色输出（容器环境无 tty 时退化为纯文本）
if [ -t 1 ]; then
  C_RED='\033[0;31m'; C_YEL='\033[1;33m'; C_GRN='\033[0;32m'; C_RST='\033[0m'
else
  C_RED=''; C_YEL=''; C_GRN=''; C_RST=''
fi

# 部署前轻量校验
if [ ! -f "./ruoyi-admin.jar" ]; then
  echo -e "${C_RED}[FATAL] 未发现 ruoyi-admin.jar，请先 mvn package${C_RST}" >&2
  exit 1
fi

# === 安全加固（fix security review） ===
# 1. umask 077 —— mkdir 出来的 dir 默认权限 0700，防止 dump / log 被他账号读
umask 077

# 2. 防御 symlink 攻击：若 ./logs 是符号链接，拒绝启动（防止 log/dump 写到攻击者控制路径）
if [ -L ./logs ]; then
  echo -e "${C_RED}[FATAL] ./logs 是符号链接，存在 path-traversal 风险，拒绝启动${C_RST}" >&2
  exit 70
fi

# 3. install -d -m 0700 等价于 mkdir -p 且强制权限 0700（覆盖已存在 dir 的权限）
install -d -m 0700 ./logs

# 4. 磁盘空间守卫 —— OOM dump 单文件最大可至 4GB（Xmx=4g），需预留 ≥ 8GB 避免填满磁盘
REQUIRED_FREE_MB=8192
AVAIL_FREE_MB=$(df -m ./logs 2>/dev/null | awk 'NR==2 {print $4}')
if [ -z "${AVAIL_FREE_MB:-}" ] || [ "${AVAIL_FREE_MB:-0}" -lt "${REQUIRED_FREE_MB}" ]; then
  echo -e "${C_RED}[FATAL] ./logs 可用空间不足 ${REQUIRED_FREE_MB}MB（当前 ${AVAIL_FREE_MB:-未知}MB），OOM dump 可能填满磁盘${C_RST}" >&2
  exit 71
fi

# 5. hprof 清理策略 —— 仅保留最近 3 个 dump（最大 12GB 上限），避免累积
#    prod 环境应配套外部 logrotate / 监控告警（参见 OPS-06）
find ./logs -maxdepth 1 -name 'java_*.hprof' -type f -printf '%T@ %p\n' 2>/dev/null \
  | sort -rn \
  | tail -n +4 \
  | awk '{print $2}' \
  | while read -r OLD_DUMP; do
      echo -e "${C_YEL}[WARN] 删除过期 OOM dump: ${OLD_DUMP}${C_RST}" >&2
      rm -f "${OLD_DUMP}"
    done

# 6. profile 判定 —— prod profile 不写 hprof（避免泄露内存快照）
SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-prod}"
OOM_FLAGS=""
if [ "${SPRING_PROFILES_ACTIVE}" != "prod" ]; then
  OOM_FLAGS="-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=./logs/"
fi

# 7. env -i 最小化环境变量 —— 仅透传 PATH / JAVA_HOME / LANG / TZ / Spring profile
#    避免 JVM process env 泄露生产凭证（如数据库密码 / API Key / OAuth Secret）
#    Spring profile 仍可通过命令行或 SPRING_PROFILES_ACTIVE 注入
PRESERVED_ENV="PATH=${PATH:-/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin}"
[ -n "${JAVA_HOME:-}" ] && PRESERVED_ENV="${PRESERVED_ENV}:JAVA_HOME=${JAVA_HOME}"
PRESERVED_ENV="${PRESERVED_ENV}:LANG=${LANG:-C.UTF-8}:TZ=${TZ:-UTC}:SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE}"

echo -e "${C_GRN}[INFO]$(date '+%F %T') 启动 ruoyi-admin.jar（G1GC / 2-4GB / profile=${SPRING_PROFILES_ACTIVE} / OOM=${OOM_FLAGS:+ENABLED}${OOM_FLAGS:-DISABLED}）${C_RST}"

# exec 替换当前进程，让 SIGTERM 直接转发到 JVM（不再 fork 子 shell）
# env -i 注入最小环境变量集，避免父进程 env 泄露给 JVM
exec env -i ${PRESERVED_ENV} \
  java \
    -Xms2g -Xmx4g \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=200 \
    ${OOM_FLAGS} \
    -jar ruoyi-admin.jar \
    "$@"