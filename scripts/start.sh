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
# =============================================================================
# 使用方式：
#   1) cd <ruoyi-ai 部署根目录>  （必须含 ruoyi-admin.jar 与 logs/ 子目录）
#   2) bash scripts/start.sh
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

mkdir -p ./logs

echo -e "${C_GRN}[INFO]$(date '+%F %T') 启动 ruoyi-admin.jar（G1GC / 2-4GB / OOM HeapDump）${C_RST}"

# exec 替换当前进程，让 SIGTERM 直接转发到 JVM（不再 fork 子 shell）
exec java \
  -Xms2g -Xmx4g \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -XX:+HeapDumpOnOutOfMemoryError \
  -XX:HeapDumpPath=./logs/ \
  -jar ruoyi-admin.jar \
  "$@"
