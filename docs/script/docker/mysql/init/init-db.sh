#!/bin/bash
# 数据库初始化脚本
# 使用 --force 参数确保即使出错也继续执行

echo "开始初始化数据库..."

# 使用 --force 参数忽略错误继续执行
mysql -uroot -proot ruoyi-ai-agent --force < /docker-entrypoint-initdb.d/ruoyi-ai-v3_mysql8.sql

echo "数据库初始化完成"
