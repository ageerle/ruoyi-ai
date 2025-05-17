#!/bin/bash

# RuoYi-AI Interactive Deployment Script
# This script helps configure and deploy the RuoYi-AI project with custom settings

set -e

echo "=================================================="
echo "   RuoYi-AI 交互式部署脚本"
echo "=================================================="
echo ""
echo "此脚本将引导您完成 RuoYi-AI 的配置和部署。"
echo "系统将提示您输入各种配置参数。"
echo ""

SCRIPT_DIR=${PWD}

# 提示输入部署目录，带有默认值
read -p "请输入部署目录 [${PWD}/ruoyi-ai-deploy]: " user_input
DEPLOY_DIR="${user_input:-${PWD}/ruoyi-ai-deploy}"

# 检查目录是否存在
if [ -d "$DEPLOY_DIR" ]; then
    echo "警告：目录 $DEPLOY_DIR 已存在！"
    read -p "您想删除它吗？[y/N]: " delete_choice
    
    case "${delete_choice:-N}" in
        [Yy]* )
            echo "正在删除现有目录..."
            rm -rf "$DEPLOY_DIR"
            mkdir -p "$DEPLOY_DIR"
            echo "目录已重新创建。"
            ;;
        * )
            echo "保留现有目录。"
            ;;
    esac
else
    mkdir -p "$DEPLOY_DIR"
    echo "目录已创建于 $DEPLOY_DIR"
fi

echo "选定的部署目录: $DEPLOY_DIR"

mkdir -p ${DEPLOY_DIR}/{data/mysql,data/redis,data/logs,data/weaviate}
cd ${DEPLOY_DIR}

# Function to prompt for a value with a default
prompt_with_default() {
    local prompt=$1
    local default=$2
    local var_name=$3

    read -p "${prompt} [${default}]: " input
    if [ -z "$input" ]; then
        eval "${var_name}=\"${default}\""
    else
        eval "${var_name}=\"${input}\""
    fi
}

# Function to prompt for a password with masking
prompt_for_password() {
    local prompt=$1
    local default=$2
    local var_name=$3

    read -sp "${prompt} [default: ${default}]: " input
    echo ""
    if [ -z "$input" ]; then
        eval "${var_name}=\"${default}\""
    else
        eval "${var_name}=\"${input}\""
    fi
}

# Function to escape special characters for sed replacement string
escape_sed_replacement_string() {
    # Escape &, \, and the delimiter | for the sed replacement string
    echo "$1" | sed -e 's/[&\\|]/\\&/g'
}

echo "=== 常规配置 ==="
prompt_with_default "时区" "Asia/Shanghai" "TZ"

echo ""
echo "=== MySQL 配置 ==="
prompt_with_default "MySQL 端口" "3306" "MYSQL_PORT"
prompt_with_default "MySQL 数据库名称" "ruoyi-ai" "MYSQL_DATABASE"
prompt_for_password "MySQL root 密码" "root" "MYSQL_ROOT_PASSWORD"

echo ""
echo "=== Redis 配置 ==="
prompt_with_default "Redis 端口" "6379" "REDIS_PORT"
prompt_for_password "Redis 密码 (留空则无密码)" "" "REDIS_PASSWORD"
prompt_with_default "Redis 数据库索引" "0" "REDIS_DATABASE"
prompt_with_default "Redis 连接超时时间" "10s" "REDIS_TIMEOUT"

echo ""
echo "=== 后端服务配置 ==="
prompt_with_default "后端服务端口" "6039" "SERVER_PORT"
prompt_with_default "后端服务主机名" "ruoyi-backend" "BACKEND_HOST"
prompt_with_default "数据库用户名" "root" "DB_USERNAME"
prompt_for_password "数据库密码" "root" "DB_PASSWORD"

echo ""
echo "=== 前端服务配置 ==="
prompt_with_default "Admin UI 端口" "8082" "ADMIN_PORT"
prompt_with_default "Web UI 端口" "8081" "WEB_PORT"

echo ""
echo "=== Weaviate 向量数据库配置 ==="
prompt_with_default "Weaviate HTTP 端口" "50050" "WEAVIATE_HTTP_PORT"
prompt_with_default "Weaviate gRPC 端口" "50051" "WEAVIATE_GRPC_PORT"
prompt_with_default "Weaviate 查询限制" "25" "WEAVIATE_QUERY_LIMIT"
prompt_with_default "Weaviate 匿名访问" "true" "WEAVIATE_ANONYMOUS_ACCESS"
prompt_with_default "Weaviate 数据路径" "/var/lib/weaviate" "WEAVIATE_DATA_PATH"
prompt_with_default "Weaviate 向量化模块" "none" "WEAVIATE_VECTORIZER_MODULE"
prompt_with_default "Weaviate 模块" "text2vec-cohere,text2vec-huggingface,text2vec-palm,text2vec-openai,generative-openai,generative-cohere,generative-palm,ref2vec-centroid,reranker-cohere,qna-openai" "WEAVIATE_MODULES"
prompt_with_default "Weaviate 集群主机名" "node1" "WEAVIATE_CLUSTER_HOSTNAME"
prompt_with_default "Weaviate 协议" "http" "WEAVIATE_PROTOCOL"
prompt_with_default "Weaviate 类名" "LocalKnowledge" "WEAVIATE_CLASSNAME"

echo ""
echo "=== 生产环境配置 ==="
prompt_with_default "生产环境数据库 URL" "jdbc:mysql://mysql:3306/ruoyi-ai?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=true&serverTimezone=GMT%2B8&autoReconnect=true&rewriteBatchedStatements=true" "PROD_DB_URL"
prompt_with_default "生产环境数据库用户名" "root" "PROD_DB_USERNAME"
prompt_for_password "生产环境数据库密码" "root" "PROD_DB_PASSWORD"
prompt_with_default "生产环境 Redis 主机" "redis" "PROD_REDIS_HOST"
prompt_with_default "生产环境 Redis 端口" "6379" "PROD_REDIS_PORT"
prompt_with_default "生产环境 Redis 数据库" "0" "PROD_REDIS_DATABASE"
prompt_for_password "生产环境 Redis 密码 (留空则无密码)" "" "PROD_REDIS_PASSWORD"
prompt_with_default "生产环境 Redis 超时时间" "10s" "PROD_REDIS_TIMEOUT"

echo ""
echo "=== 前端配置 ==="
prompt_with_default "前端后端 API 基础 URL" "http://${BACKEND_HOST}:${SERVER_PORT}" "FRONTEND_API_BASE_URL"
prompt_with_default "前端开发服务器端口" "3000" "FRONTEND_DEV_PORT"

# Copy template files
cp ${SCRIPT_DIR}/template/.env.template ${DEPLOY_DIR}/.env
cp ${SCRIPT_DIR}/template/docker-compose.yaml.template ${DEPLOY_DIR}/docker-compose.yaml

echo "已将模板文件复制到部署目录。"

# 替换 .env 文件中的占位符
echo "正在使用您的配置更新 .env 文件..."
sed -i "s|{{TZ}}|$(escape_sed_replacement_string "${TZ}")|g" ${DEPLOY_DIR}/.env
sed -i "s|{{MYSQL_ROOT_PASSWORD}}|$(escape_sed_replacement_string "${MYSQL_ROOT_PASSWORD}")|g" ${DEPLOY_DIR}/.env
sed -i "s|{{MYSQL_DATABASE}}|$(escape_sed_replacement_string "${MYSQL_DATABASE}")|g" ${DEPLOY_DIR}/.env
sed -i "s|{{MYSQL_PORT}}|$(escape_sed_replacement_string "${MYSQL_PORT}")|g" ${DEPLOY_DIR}/.env
sed -i "s|{{REDIS_PORT}}|$(escape_sed_replacement_string "${REDIS_PORT}")|g" ${DEPLOY_DIR}/.env
sed -i "s|{{REDIS_PASSWORD}}|$(escape_sed_replacement_string "${REDIS_PASSWORD}")|g" ${DEPLOY_DIR}/.env
sed -i "s|{{REDIS_DATABASE}}|$(escape_sed_replacement_string "${REDIS_DATABASE}")|g" ${DEPLOY_DIR}/.env
sed -i "s|{{REDIS_TIMEOUT}}|$(escape_sed_replacement_string "${REDIS_TIMEOUT}")|g" ${DEPLOY_DIR}/.env
sed -i "s|{{SERVER_PORT}}|$(escape_sed_replacement_string "${SERVER_PORT}")|g" ${DEPLOY_DIR}/.env
sed -i "s|{{DB_URL}}|$(escape_sed_replacement_string "jdbc:mysql://mysql:3306/${MYSQL_DATABASE}?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=true&serverTimezone=GMT%2B8&autoReconnect=true&rewriteBatchedStatements=true")|g" ${DEPLOY_DIR}/.env
sed -i "s|{{DB_USERNAME}}|$(escape_sed_replacement_string "${DB_USERNAME}")|g" ${DEPLOY_DIR}/.env
sed -i "s|{{DB_PASSWORD}}|$(escape_sed_replacement_string "${DB_PASSWORD}")|g" ${DEPLOY_DIR}/.env
sed -i "s|{{BACKEND_HOST}}|$(escape_sed_replacement_string "${BACKEND_HOST}")|g" ${DEPLOY_DIR}/.env
sed -i "s|{{ADMIN_PORT}}|$(escape_sed_replacement_string "${ADMIN_PORT}")|g" ${DEPLOY_DIR}/.env
sed -i "s|{{WEB_PORT}}|$(escape_sed_replacement_string "${WEB_PORT}")|g" ${DEPLOY_DIR}/.env
sed -i "s|{{FRONTEND_API_BASE_URL}}|$(escape_sed_replacement_string "${FRONTEND_API_BASE_URL}")|g" ${DEPLOY_DIR}/.env
sed -i "s|{{FRONTEND_DEV_PORT}}|$(escape_sed_replacement_string "${FRONTEND_DEV_PORT}")|g" ${DEPLOY_DIR}/.env
sed -i "s|{{WEAVIATE_HTTP_PORT}}|$(escape_sed_replacement_string "${WEAVIATE_HTTP_PORT}")|g" ${DEPLOY_DIR}/.env
sed -i "s|{{WEAVIATE_GRPC_PORT}}|$(escape_sed_replacement_string "${WEAVIATE_GRPC_PORT}")|g" ${DEPLOY_DIR}/.env
sed -i "s|{{WEAVIATE_QUERY_LIMIT}}|$(escape_sed_replacement_string "${WEAVIATE_QUERY_LIMIT}")|g" ${DEPLOY_DIR}/.env
sed -i "s|{{WEAVIATE_ANONYMOUS_ACCESS}}|$(escape_sed_replacement_string "${WEAVIATE_ANONYMOUS_ACCESS}")|g" ${DEPLOY_DIR}/.env
sed -i "s|{{WEAVIATE_DATA_PATH}}|$(escape_sed_replacement_string "${WEAVIATE_DATA_PATH}")|g" ${DEPLOY_DIR}/.env
sed -i "s|{{WEAVIATE_VECTORIZER_MODULE}}|$(escape_sed_replacement_string "${WEAVIATE_VECTORIZER_MODULE}")|g" ${DEPLOY_DIR}/.env
sed -i "s|{{WEAVIATE_MODULES}}|$(escape_sed_replacement_string "${WEAVIATE_MODULES}")|g" ${DEPLOY_DIR}/.env
sed -i "s|{{WEAVIATE_CLUSTER_HOSTNAME}}|$(escape_sed_replacement_string "${WEAVIATE_CLUSTER_HOSTNAME}")|g" ${DEPLOY_DIR}/.env
sed -i "s|{{WEAVIATE_PROTOCOL}}|$(escape_sed_replacement_string "${WEAVIATE_PROTOCOL}")|g" ${DEPLOY_DIR}/.env
sed -i "s|{{WEAVIATE_CLASSNAME}}|$(escape_sed_replacement_string "${WEAVIATE_CLASSNAME}")|g" ${DEPLOY_DIR}/.env
sed -i "s|{{PROD_DB_URL}}|$(escape_sed_replacement_string "${PROD_DB_URL}")|g" ${DEPLOY_DIR}/.env
sed -i "s|{{PROD_DB_USERNAME}}|$(escape_sed_replacement_string "${PROD_DB_USERNAME}")|g" ${DEPLOY_DIR}/.env
sed -i "s|{{PROD_DB_PASSWORD}}|$(escape_sed_replacement_string "${PROD_DB_PASSWORD}")|g" ${DEPLOY_DIR}/.env
sed -i "s|{{PROD_REDIS_HOST}}|$(escape_sed_replacement_string "${PROD_REDIS_HOST}")|g" ${DEPLOY_DIR}/.env
sed -i "s|{{PROD_REDIS_PORT}}|$(escape_sed_replacement_string "${PROD_REDIS_PORT}")|g" ${DEPLOY_DIR}/.env
sed -i "s|{{PROD_REDIS_DATABASE}}|$(escape_sed_replacement_string "${PROD_REDIS_DATABASE}")|g" ${DEPLOY_DIR}/.env
sed -i "s|{{PROD_REDIS_PASSWORD}}|$(escape_sed_replacement_string "${PROD_REDIS_PASSWORD}")|g" ${DEPLOY_DIR}/.env
sed -i "s|{{PROD_REDIS_TIMEOUT}}|$(escape_sed_replacement_string "${PROD_REDIS_TIMEOUT}")|g" ${DEPLOY_DIR}/.env

echo "已使用您的配置更新 .env 文件。"

# 替换 docker-compose.yaml 文件中的占位符
echo "正在使用您的配置更新 docker-compose.yaml 文件..."

# Determine Redis command arguments based on password
#if [ -n "${REDIS_PASSWORD}" ]; then
#    REDIS_COMMAND_ARGS="--requirepass $(escape_sed_replacement_string "${REDIS_PASSWORD}")"
#else
#    REDIS_COMMAND_ARGS=""
#fi

sed -i "s|{{MYSQL_ROOT_PASSWORD}}|$(escape_sed_replacement_string "${MYSQL_ROOT_PASSWORD}")|g" ${DEPLOY_DIR}/docker-compose.yaml
sed -i "s|{{MYSQL_DATABASE}}|$(escape_sed_replacement_string "${MYSQL_DATABASE}")|g" ${DEPLOY_DIR}/docker-compose.yaml
sed -i "s|{{MYSQL_PORT}}|$(escape_sed_replacement_string "${MYSQL_PORT}")|g" ${DEPLOY_DIR}/docker-compose.yaml
sed -i "s|{{REDIS_PORT}}|$(escape_sed_replacement_string "${REDIS_PORT}")|g" ${DEPLOY_DIR}/docker-compose.yaml
sed -i "s|{{REDIS_COMMAND_ARGS}}|$(escape_sed_replacement_string "${REDIS_COMMAND_ARGS}")|g" ${DEPLOY_DIR}/docker-compose.yaml
sed -i "s|{{WEAVIATE_HTTP_PORT}}|$(escape_sed_replacement_string "${WEAVIATE_HTTP_PORT}")|g" ${DEPLOY_DIR}/docker-compose.yaml
sed -i "s|{{WEAVIATE_GRPC_PORT}}|$(escape_sed_replacement_string "${WEAVIATE_GRPC_PORT}")|g" ${DEPLOY_DIR}/docker-compose.yaml
sed -i "s|{{WEAVIATE_QUERY_LIMIT}}|$(escape_sed_replacement_string "${WEAVIATE_QUERY_LIMIT}")|g" ${DEPLOY_DIR}/docker-compose.yaml
sed -i "s|{{WEAVIATE_ANONYMOUS_ACCESS}}|$(escape_sed_replacement_string "${WEAVIATE_ANONYMOUS_ACCESS}")|g" ${DEPLOY_DIR}/docker-compose.yaml
sed -i "s|{{WEAVIATE_DATA_PATH}}|$(escape_sed_replacement_string "${WEAVIATE_DATA_PATH}")|g" ${DEPLOY_DIR}/docker-compose.yaml
sed -i "s|{{WEAVIATE_VECTORIZER_MODULE}}|$(escape_sed_replacement_string "${WEAVIATE_VECTORIZER_MODULE}")|g" ${DEPLOY_DIR}/docker-compose.yaml
sed -i "s|{{WEAVIATE_MODULES}}|$(escape_sed_replacement_string "${WEAVIATE_MODULES}")|g" ${DEPLOY_DIR}/docker-compose.yaml
sed -i "s|{{WEAVIATE_CLUSTER_HOSTNAME}}|$(escape_sed_replacement_string "${WEAVIATE_CLUSTER_HOSTNAME}")|g" ${DEPLOY_DIR}/docker-compose.yaml
sed -i "s|{{SERVER_PORT}}|$(escape_sed_replacement_string "${SERVER_PORT}")|g" ${DEPLOY_DIR}/docker-compose.yaml
sed -i "s|{{DB_URL}}|$(escape_sed_replacement_string "jdbc:mysql://mysql:3306/${MYSQL_DATABASE}?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=true&serverTimezone=GMT%2B8&autoReconnect=true&rewriteBatchedStatements=true")|g" ${DEPLOY_DIR}/docker-compose.yaml
sed -i "s|{{DB_USERNAME}}|$(escape_sed_replacement_string "${DB_USERNAME}")|g" ${DEPLOY_DIR}/docker-compose.yaml
sed -i "s|{{DB_PASSWORD}}|$(escape_sed_replacement_string "${DB_PASSWORD}")|g" ${DEPLOY_DIR}/docker-compose.yaml
sed -i "s|{{REDIS_HOST}}|redis|g" ${DEPLOY_DIR}/docker-compose.yaml # REDIS_HOST is hardcoded to 'redis' in docker-compose
sed -i "s|{{REDIS_DATABASE}}|$(escape_sed_replacement_string "${REDIS_DATABASE}")|g" ${DEPLOY_DIR}/docker-compose.yaml
sed -i "s|{{REDIS_PASSWORD}}|$(escape_sed_replacement_string "${REDIS_PASSWORD}")|g" ${DEPLOY_DIR}/docker-compose.yaml
sed -i "s|{{REDIS_TIMEOUT}}|$(escape_sed_replacement_string "${REDIS_TIMEOUT}")|g" ${DEPLOY_DIR}/docker-compose.yaml
sed -i "s|{{TZ}}|$(escape_sed_replacement_string "${TZ}")|g" ${DEPLOY_DIR}/docker-compose.yaml
sed -i "s|{{ADMIN_PORT}}|$(escape_sed_replacement_string "${ADMIN_PORT}")|g" ${DEPLOY_DIR}/docker-compose.yaml
sed -i "s|{{WEB_PORT}}|$(escape_sed_replacement_string "${WEB_PORT}")|g" ${DEPLOY_DIR}/docker-compose.yaml

sed -i "s|ruoyi-ai-backend:latest|ruoyi-ai-backend:${RUOYI_AI_BRANCH}|g" ${DEPLOY_DIR}/docker-compose.yaml
sed -i "s|ruoyi-ai-admin:latest|ruoyi-ai-admin:${RUOYI_ADMIN_BRANCH}|g" ${DEPLOY_DIR}/docker-compose.yaml
sed -i "s|ruoyi-ai-web:latest|ruoyi-ai-web:${RUOYI_WEB_BRANCH}|g" ${DEPLOY_DIR}/docker-compose.yaml

echo "已使用您的配置更新 docker-compose.yaml 文件。"

echo ""
echo "=== 构建或部署选项 ==="
read -p "您想构建新镜像 (B) 还是直接使用现有镜像部署 (D)？[B/d]: " build_or_deploy_choice
BUILD_CHOICE="${build_or_deploy_choice:-B}" # Default to Build

if [[ "${BUILD_CHOICE}" == [Bb]* ]]; then
    echo "正在进行镜像构建过程..."

    # Clone ruoyi-ai-backend repositories
    if [ -d "${DEPLOY_DIR}/ruoyi-ai" ]; then
        echo "目录 ${DEPLOY_DIR}/ruoyi-ai 已存在。"
        read -p "您想删除它并克隆一个新的副本吗？[Y/n]: " answer
        case ${answer:-Y} in
            [Yy]* )
                echo "正在删除现有目录..."
                rm -rf ${DEPLOY_DIR}/ruoyi-ai
                echo "正在克隆 ruoyi-ai-backend 仓库..."
                cd ${DEPLOY_DIR} && git clone https://github.com/ageerle/ruoyi-ai
                
                # 提示选择分支
                read -p "请输入 ruoyi-ai 仓库的分支名称 [main]: " RUOYI_AI_BRANCH
                RUOYI_AI_BRANCH="${RUOYI_AI_BRANCH:-main}"
                echo "正在切换到分支: ${RUOYI_AI_BRANCH}"
                cd ${DEPLOY_DIR}/ruoyi-ai && git checkout ${RUOYI_AI_BRANCH}
                cd ..
            ;;
            [Nn]* )
                echo "跳过克隆操作。"
            ;;
            * )
                echo "无效输入。跳过克隆操作。"
            ;;
        esac
    else
        echo "正在克隆 ruoyi-ai-backend 仓库..."
        cd ${DEPLOY_DIR} && git clone https://github.com/ageerle/ruoyi-ai
        
        # 提示选择分支
        read -p "请输入 ruoyi-ai 仓库的分支名称 [main]: " RUOYI_AI_BRANCH
        RUOYI_AI_BRANCH="${RUOYI_AI_BRANCH:-main}"
        echo "正在切换到分支: ${RUOYI_AI_BRANCH}"
        cd ${DEPLOY_DIR}/ruoyi-ai && git checkout ${RUOYI_AI_BRANCH}
        cd ..
    fi

    # Clone ruoyi-ai-admin repositories
    if [ -d "${DEPLOY_DIR}/ruoyi-admin" ]; then
        echo "目录 ${DEPLOY_DIR}/ruoyi-admin 已存在。"
        read -p "您想删除它并克隆一个新的副本吗？[Y/n]: " answer
        case ${answer:-Y} in
            [Yy]* )
                echo "正在删除现有目录..."
                rm -rf ${DEPLOY_DIR}/ruoyi-admin
                echo "正在克隆 ruoyi-admin 仓库..."
                cd ${DEPLOY_DIR} && git clone https://github.com/ageerle/ruoyi-admin
                
                # 提示选择分支
                read -p "请输入 ruoyi-admin 仓库的分支名称 [main]: " RUOYI_ADMIN_BRANCH
                RUOYI_ADMIN_BRANCH="${RUOYI_ADMIN_BRANCH:-main}"
                echo "正在切换到分支: ${RUOYI_ADMIN_BRANCH}"
                cd ${DEPLOY_DIR}/ruoyi-admin && git checkout ${RUOYI_ADMIN_BRANCH}
                cd ..
            ;;
            [Nn]* )
                echo "跳过克隆操作。"
            ;;
            * )
                echo "无效输入。跳过克隆操作。"
            ;;
        esac
    else
        echo "正在克隆 ruoyi-ai-admin 仓库..."
        cd ${DEPLOY_DIR} && git clone https://github.com/ageerle/ruoyi-admin
        
        # 提示选择分支
        read -p "请输入 ruoyi-admin 仓库的分支名称 [main]: " RUOYI_ADMIN_BRANCH
        RUOYI_ADMIN_BRANCH="${RUOYI_ADMIN_BRANCH:-main}"
        echo "正在切换到分支: ${RUOYI_ADMIN_BRANCH}"
        cd ${DEPLOY_DIR}/ruoyi-admin && git checkout ${RUOYI_ADMIN_BRANCH}
        cd ..
    fi

    # Clone ruoyi-ai-web repositories
    if [ -d "${DEPLOY_DIR}/ruoyi-web" ]; then
        echo "目录 ${DEPLOY_DIR}/ruoyi-web 已存在。"
        read -p "您想删除它并克隆一个新的副本吗？[Y/n]: " answer
        case ${answer:-Y} in
            [Yy]* )
                echo "正在删除现有目录..."
                rm -rf ${DEPLOY_DIR}/ruoyi-web
                echo "正在克隆 ruoyi-ai-web 仓库..."
                cd ${DEPLOY_DIR} && git clone https://github.com/ageerle/ruoyi-web
                
                # 提示选择分支
                read -p "请输入 ruoyi-web 仓库的分支名称 [main]: " RUOYI_WEB_BRANCH
                RUOYI_WEB_BRANCH="${RUOYI_WEB_BRANCH:-main}"
                echo "正在切换到分支: ${RUOYI_WEB_BRANCH}"
                cd ${DEPLOY_DIR}/ruoyi-web && git checkout ${RUOYI_WEB_BRANCH}
                cd ..
            ;;
            [Nn]* )
                echo "跳过克隆操作。"
            ;;
            * )
                echo "无效输入。跳过克隆操作。"
            ;;
        esac
    else
        echo "正在克隆 ruoyi-ai-web 仓库..."
        cd ${DEPLOY_DIR} && git clone https://github.com/ageerle/ruoyi-web
        
        # 提示选择分支
        read -p "请输入 ruoyi-web 仓库的分支名称 [main]: " RUOYI_WEB_BRANCH
        RUOYI_WEB_BRANCH="${RUOYI_WEB_BRANCH:-main}"
        echo "正在切换到分支: ${RUOYI_WEB_BRANCH}"
        cd ${DEPLOY_DIR}/ruoyi-web && git checkout ${RUOYI_WEB_BRANCH}
        cd ..
    fi

    # 更新 application-prod.yml 文件
    echo "正在使用您的配置更新 application-prod.yml 文件..."
    # Copy application-prod.yml template
    cp ${SCRIPT_DIR}/template/application-prod.yml.template ${DEPLOY_DIR}/ruoyi-ai/ruoyi-admin/src/main/resources/application-prod.yml

    # Replace placeholders in application-prod.yml
    sed -i "s|{{PROD_DB_URL}}|$(escape_sed_replacement_string "${PROD_DB_URL}")|g" ${DEPLOY_DIR}/ruoyi-ai/ruoyi-admin/src/main/resources/application-prod.yml
    sed -i "s|{{PROD_DB_USERNAME}}|$(escape_sed_replacement_string "${PROD_DB_USERNAME}")|g" ${DEPLOY_DIR}/ruoyi-ai/ruoyi-admin/src/main/resources/application-prod.yml
    sed -i "s|{{PROD_DB_PASSWORD}}|$(escape_sed_replacement_string "${PROD_DB_PASSWORD}")|g" ${DEPLOY_DIR}/ruoyi-ai/ruoyi-admin/src/main/resources/application-prod.yml
    sed -i "s|{{PROD_REDIS_HOST}}|$(escape_sed_replacement_string "${PROD_REDIS_HOST}")|g" ${DEPLOY_DIR}/ruoyi-ai/ruoyi-admin/src/main/resources/application-prod.yml
    sed -i "s|{{PROD_REDIS_PORT}}|$(escape_sed_replacement_string "${PROD_REDIS_PORT}")|g" ${DEPLOY_DIR}/ruoyi-ai/ruoyi-admin/src/main/resources/application-prod.yml
    sed -i "s|{{PROD_REDIS_DATABASE}}|$(escape_sed_replacement_string "${PROD_REDIS_DATABASE}")|g" ${DEPLOY_DIR}/ruoyi-ai/ruoyi-admin/src/main/resources/application-prod.yml
    sed -i "s|{{PROD_REDIS_PASSWORD}}|$(escape_sed_replacement_string "${PROD_REDIS_PASSWORD}")|g" ${DEPLOY_DIR}/ruoyi-ai/ruoyi-admin/src/main/resources/application-prod.yml
    sed -i "s|{{PROD_REDIS_TIMEOUT}}|$(escape_sed_replacement_string "${PROD_REDIS_TIMEOUT}")|g" ${DEPLOY_DIR}/ruoyi-ai/ruoyi-admin/src/main/resources/application-prod.yml

    # 更新 vite.config.mts 文件
    echo "正在使用您的配置更新 vite.config.mts 文件..."
    sed -i "s|http://127.0.0.1:6039|${FRONTEND_API_BASE_URL}|g" ${DEPLOY_DIR}/ruoyi-admin/apps/web-antd/vite.config.mts

    # Create Nginx configuration files for frontend services
    echo "正在将 Admin UI 的 Nginx 配置模板复制到临时位置..."
    cp ${SCRIPT_DIR}/template/nginx.admin.conf.template ${DEPLOY_DIR}/nginx.admin.conf.tmp

    echo "正在临时文件中更新 Admin UI 的 Nginx 配置..."
    sed -i "s|{{BACKEND_HOST}}|$(escape_sed_replacement_string "${BACKEND_HOST}")|g" ${DEPLOY_DIR}/nginx.admin.conf.tmp
    sed -i "s|{{SERVER_PORT}}|$(escape_sed_replacement_string "${SERVER_PORT}")|g" ${DEPLOY_DIR}/nginx.admin.conf.tmp

    echo "正在将更新后的 Admin UI Nginx 配置移动到最终位置..."
    mv ${DEPLOY_DIR}/nginx.admin.conf.tmp ${DEPLOY_DIR}/ruoyi-admin/nginx.conf

    echo "正在将 Web UI 的 Nginx 配置模板复制到临时位置..."
    cp ${SCRIPT_DIR}/template/nginx.web.conf.template ${DEPLOY_DIR}/nginx.web.conf.tmp

    echo "正在临时文件中更新 Web UI 的 Nginx 配置..."
    sed -i "s|{{BACKEND_HOST}}|$(escape_sed_replacement_string "${BACKEND_HOST}")|g" ${DEPLOY_DIR}/nginx.web.conf.tmp
    sed -i "s|{{SERVER_PORT}}|$(escape_sed_replacement_string "${SERVER_PORT}")|g" ${DEPLOY_DIR}/nginx.web.conf.tmp

    echo "正在将更新后的 Web UI Nginx 配置移动到最终位置..."
    mv ${DEPLOY_DIR}/nginx.web.conf.tmp ${DEPLOY_DIR}/ruoyi-web/nginx.conf

    # 为前端服务创建 Dockerfile
    echo "正在为 Admin UI 创建 Dockerfile..."
    cat > ${DEPLOY_DIR}/ruoyi-admin/Dockerfile << EOF
FROM nginx:1.25-alpine

COPY dist/ /usr/share/nginx/html/
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
EOF

    echo "正在为 Web UI 创建 Dockerfile..."
    cat > ${DEPLOY_DIR}/ruoyi-web/Dockerfile << EOF
FROM nginx:1.25-alpine

COPY dist/ /usr/share/nginx/html/
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
EOF

    # 构建后端服务
    echo "正在构建 Ruoyi-AI 后端服务..."
    cd ${DEPLOY_DIR}/ruoyi-ai
    docker run -it --rm --name build-ruoyi-ai-backend -v ${DEPLOY_DIR}/ruoyi-ai:/code --entrypoint=/bin/bash maven:3.9.9-eclipse-temurin-17-alpine -c "cd /code && mvn clean package -P prod"

    # 构建前端 Admin 服务
    echo "正在构建 Ruoyi-AI 前端 Admin 服务..."
    cd ${DEPLOY_DIR}/ruoyi-admin
    docker run -it --rm --name build-ruoyi-ai-admin -v ${DEPLOY_DIR}/ruoyi-admin:/app -w /app node:20 sh -c "npm install -g pnpm && pnpm install && pnpm build"

    # 构建前端 Web 服务
    echo "正在构建 Ruoyi-AI 前端 Web 服务..."
    cd ${DEPLOY_DIR}/ruoyi-web
    docker run -it --rm --name build-ruoyi-ai-web -v ${DEPLOY_DIR}/ruoyi-web:/app -w /app node:20 sh -c "npm install -g pnpm && pnpm install && pnpm build"

    # Build Docker images
    echo "Building Ruoyi-AI Backend Docker images..."
    cd ${DEPLOY_DIR}/ruoyi-ai
    rm -rf temp
    mkdir temp
    cp ./ruoyi-admin/target/ruoyi-admin.jar temp/
    cd temp/
    cat > Dockerfile << EOF
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY ruoyi-admin.jar /app/ruoyi-admin.jar
EXPOSE ${SERVER_PORT}
ENTRYPOINT ["java","-jar","ruoyi-admin.jar","--spring.profiles.active=prod"]
EOF
    docker build -t ruoyi-ai-backend:${RUOYI_AI_BRANCH} .
    cd ..

    echo "Building Ruoyi-AI Admin Docker images..."
    cd ${DEPLOY_DIR}/ruoyi-admin
    rm -rf temp
    mkdir temp
    cp ./apps/web-antd/dist.zip temp/
    cp Dockerfile temp/
    cp nginx.conf temp/
    cd temp/
    unzip dist.zip -d dist
    rm -f dist.zip
    docker build -t ruoyi-admin:${RUOYI_ADMIN_BRANCH} .
    cd ..

    echo "Building Ruoyi-AI Web Docker images..."
    cd ${DEPLOY_DIR}/ruoyi-web
    rm -rf temp
    mkdir temp
    cp -pr ${DEPLOY_DIR}/ruoyi-web/dist temp/
    cp Dockerfile temp/
    cd temp/
    docker build -t ruoyi-web:${RUOYI_WEB_BRANCH} .
    cd ..
else
    echo "跳过镜像构建过程。正在使用现有镜像直接部署..."
fi

# Copy SQL file
rm -rf ${DEPLOY_DIR}/mysql-init
cp -pr ${SCRIPT_DIR}/mysql-init ${DEPLOY_DIR}/

# 使用配置值更新 SQL 文件
echo "正在更新 SQL 配置值..."
sed -i "s|'weaviate', 'host', '127.0.0.1:6038'|'weaviate', 'host', 'weaviate:8080'|g" ${DEPLOY_DIR}/mysql-init/01_ruoyi-ai.sql
sed -i "s|'weaviate', 'protocol', 'http'|'weaviate', 'protocol', '${WEAVIATE_PROTOCOL}'|g" ${DEPLOY_DIR}/mysql-init/01_ruoyi-ai.sql
sed -i "s|'weaviate', 'classname', 'LocalKnowledge'|'weaviate', 'classname', '${WEAVIATE_CLASSNAME}'|g" ${DEPLOY_DIR}/mysql-init/01_ruoyi-ai.sql

# 使用 Docker Compose 部署
echo "正在使用 Docker Compose 进行部署..."
cd ${DEPLOY_DIR}
docker-compose down
docker-compose up -d

echo "=================================================="
echo "   RuoYi-AI 部署完成"
echo "=================================================="
echo ""
echo "您的 RuoYi-AI 系统已部署以下服务:"
echo "- 后端 API: http://localhost:${SERVER_PORT}"
echo "- Admin UI: http://localhost:${ADMIN_PORT}"
echo "- Web UI: http://localhost:${WEB_PORT}"
echo "- Weaviate: http://localhost:${WEAVIATE_HTTP_PORT}"
echo ""
echo "所有配置均已根据您的输入进行自定义。"
echo "配置文件已更新为使用环境变量。"
echo ""
echo "感谢您使用 RuoYi-AI 交互式部署脚本！"
