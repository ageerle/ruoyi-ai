#!/bin/bash

# RuoYi-AI Interactive Deployment Script
# This script helps configure and deploy the RuoYi-AI project with custom settings

set -e

echo "=================================================="
echo "   RuoYi-AI Interactive Deployment Script"
echo "=================================================="
echo ""
echo "This script will guide you through configuring and deploying RuoYi-AI."
echo "You'll be prompted for various configuration parameters."
echo ""

SCRIPT_DIR=${PWD}

# Prompt for deployment directory with default value
read -p "Enter deployment directory [${PWD}/ruoyi-ai-deploy]: " user_input
DEPLOY_DIR="${user_input:-${PWD}/ruoyi-ai-deploy}"

# Check if directory exists
if [ -d "$DEPLOY_DIR" ]; then
    echo "Warning: Directory $DEPLOY_DIR already exists!"
    read -p "Do you want to remove it? [y/N]: " delete_choice
    
    case "${delete_choice:-N}" in
        [Yy]* )
            echo "Removing existing directory..."
            rm -rf "$DEPLOY_DIR"
            mkdir -p "$DEPLOY_DIR"
            echo "Directory has been recreated."
            ;;
        * )
            echo "Keeping existing directory."
            ;;
    esac
else
    mkdir -p "$DEPLOY_DIR"
    echo "Directory created at $DEPLOY_DIR"
fi

echo "Selected deployment directory: $DEPLOY_DIR"

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

echo "=== General Configuration ==="
prompt_with_default "Timezone" "Asia/Shanghai" "TZ"

echo ""
echo "=== MySQL Configuration ==="
prompt_with_default "MySQL port" "3306" "MYSQL_PORT"
prompt_with_default "MySQL database name" "ruoyi-ai" "MYSQL_DATABASE"
prompt_for_password "MySQL root password" "root" "MYSQL_ROOT_PASSWORD"

echo ""
echo "=== Redis Configuration ==="
prompt_with_default "Redis port" "6379" "REDIS_PORT"
prompt_for_password "Redis password (leave empty for no password)" "" "REDIS_PASSWORD"
prompt_with_default "Redis database index" "0" "REDIS_DATABASE"
prompt_with_default "Redis connection timeout" "10s" "REDIS_TIMEOUT"

echo ""
echo "=== Backend Service Configuration ==="
prompt_with_default "Backend service port" "6039" "SERVER_PORT"
prompt_with_default "Backend service hostname" "ruoyi-backend" "BACKEND_HOST"
prompt_with_default "Database username" "root" "DB_USERNAME"
prompt_for_password "Database password" "root" "DB_PASSWORD"

echo ""
echo "=== Frontend Services Configuration ==="
prompt_with_default "Admin UI port" "8082" "ADMIN_PORT"
prompt_with_default "Web UI port" "8081" "WEB_PORT"

echo ""
echo "=== Weaviate Vector Database Configuration ==="
prompt_with_default "Weaviate HTTP port" "50050" "WEAVIATE_HTTP_PORT"
prompt_with_default "Weaviate gRPC port" "50051" "WEAVIATE_GRPC_PORT"
prompt_with_default "Weaviate query limit" "25" "WEAVIATE_QUERY_LIMIT"
prompt_with_default "Weaviate anonymous access" "true" "WEAVIATE_ANONYMOUS_ACCESS"
prompt_with_default "Weaviate data path" "/var/lib/weaviate" "WEAVIATE_DATA_PATH"
prompt_with_default "Weaviate vectorizer module" "none" "WEAVIATE_VECTORIZER_MODULE"
prompt_with_default "Weaviate modules" "text2vec-cohere,text2vec-huggingface,text2vec-palm,text2vec-openai,generative-openai,generative-cohere,generative-palm,ref2vec-centroid,reranker-cohere,qna-openai" "WEAVIATE_MODULES"
prompt_with_default "Weaviate cluster hostname" "node1" "WEAVIATE_CLUSTER_HOSTNAME"
prompt_with_default "Weaviate protocol" "http" "WEAVIATE_PROTOCOL"
prompt_with_default "Weaviate class name" "LocalKnowledge" "WEAVIATE_CLASSNAME"

echo ""
echo "=== Production Environment Configuration ==="
prompt_with_default "Production database URL" "jdbc:mysql://mysql:3306/ruoyi-ai?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=true&serverTimezone=GMT%2B8&autoReconnect=true&rewriteBatchedStatements=true" "PROD_DB_URL"
prompt_with_default "Production database username" "root" "PROD_DB_USERNAME"
prompt_for_password "Production database password" "root" "PROD_DB_PASSWORD"
prompt_with_default "Production Redis host" "redis" "PROD_REDIS_HOST"
prompt_with_default "Production Redis port" "6379" "PROD_REDIS_PORT"
prompt_with_default "Production Redis database" "0" "PROD_REDIS_DATABASE"
prompt_for_password "Production Redis password (leave empty for no password)" "" "PROD_REDIS_PASSWORD"
prompt_with_default "Production Redis timeout" "10s" "PROD_REDIS_TIMEOUT"

echo ""
echo "=== Frontend Configuration ==="
prompt_with_default "Backend API base URL for frontend" "http://${BACKEND_HOST}:${SERVER_PORT}" "FRONTEND_API_BASE_URL"
prompt_with_default "Frontend development server port" "3000" "FRONTEND_DEV_PORT"

# Copy template files
cp ${SCRIPT_DIR}/template/.env.template ${DEPLOY_DIR}/.env
cp ${SCRIPT_DIR}/template/docker-compose.yaml.template ${DEPLOY_DIR}/docker-compose.yaml

echo "Copied template files to deployment directory."

# Replace placeholders in .env file
echo "Updating .env file with your configurations..."
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

echo "Updated .env file with your configurations."

# Replace placeholders in docker-compose.yaml file
echo "Updating docker-compose.yaml file with your configurations..."

# Determine Redis command arguments based on password
if [ -n "${REDIS_PASSWORD}" ]; then
    REDIS_COMMAND_ARGS="--requirepass $(escape_sed_replacement_string "${REDIS_PASSWORD}")"
else
    REDIS_COMMAND_ARGS=""
fi

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

echo "Updated docker-compose.yaml file with your configurations."

echo ""
echo "=== Build or Deploy Option ==="
read -p "Do you want to build new images (B) or deploy directly using existing images (D)? [B/d]: " build_or_deploy_choice
BUILD_CHOICE="${build_or_deploy_choice:-B}" # Default to Build

if [[ "${BUILD_CHOICE}" == [Bb]* ]]; then
    echo "Proceeding with image build process..."

    # Clone ruoyi-ai-backend repositories
    if [ -d "${DEPLOY_DIR}/ruoyi-ai" ]; then
        echo "Directory ${DEPLOY_DIR}/ruoyi-ai already exists."
        read -p "Do you want to remove it and clone a fresh copy? [Y/n]: " answer
        case ${answer:-Y} in
            [Yy]* )
                echo "Removing existing directory..."
                rm -rf ${DEPLOY_DIR}/ruoyi-ai
                echo "Cloning ruoyi-ai-backend repository..."
                cd ${DEPLOY_DIR} && git clone https://github.com/ageerle/ruoyi-ai
            ;;
            [Nn]* )
                echo "Skipping clone operation."
            ;;
            * )
                echo "Invalid input. Skipping clone operation."
            ;;
        esac
    else
        echo "Cloning ruoyi-ai-backend repository..."
        cd ${DEPLOY_DIR} && git clone https://github.com/ageerle/ruoyi-ai
    fi

    # Clone ruoyi-ai-admin repositories
    if [ -d "${DEPLOY_DIR}/ruoyi-admin" ]; then
        echo "Directory ${DEPLOY_DIR}/ruoyi-admin already exists."
        read -p "Do you want to remove it and clone a fresh copy? [Y/n]: " answer
        case ${answer:-Y} in
            [Yy]* )
                echo "Removing existing directory..."
                rm -rf ${DEPLOY_DIR}/ruoyi-admin
                echo "Cloning ruoyi-admin repository..."
                cd ${DEPLOY_DIR} && git clone https://github.com/ageerle/ruoyi-admin
            ;;
            [Nn]* )
                echo "Skipping clone operation."
            ;;
            * )
                echo "Invalid input. Skipping clone operation."
            ;;
        esac
    else
        echo "Cloning ruoyi-ai-admin repository..."
        cd ${DEPLOY_DIR} && git clone https://github.com/ageerle/ruoyi-admin
    fi

    # Clone ruoyi-ai-web repositories
    if [ -d "${DEPLOY_DIR}/ruoyi-web" ]; then
        echo "Directory ${DEPLOY_DIR}/ruoyi-web already exists."
        read -p "Do you want to remove it and clone a fresh copy? [Y/n]: " answer
        case ${answer:-Y} in
            [Yy]* )
                echo "Removing existing directory..."
                rm -rf ${DEPLOY_DIR}/ruoyi-web
                echo "Cloning ruoyi-ai-web repository..."
                cd ${DEPLOY_DIR} && git clone https://github.com/ageerle/ruoyi-web
            ;;
            [Nn]* )
                echo "Skipping clone operation."
            ;;
            * )
                echo "Invalid input. Skipping clone operation."
            ;;
        esac
    else
        echo "Cloning ruoyi-ai-web repository..."
        cd ${DEPLOY_DIR} && git clone https://github.com/ageerle/ruoyi-web
    fi

    # Update application-prod.yml file
    echo "Updating application-prod.yml with your configurations..."
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

    # Update vite.config.mts file
    echo "Updating vite.config.mts with your configurations..."
    sed -i "s|http://127.0.0.1:6039|${FRONTEND_API_BASE_URL}|g" ${DEPLOY_DIR}/ruoyi-admin/apps/web-antd/vite.config.mts

    # Create Nginx configuration files for frontend services
    echo "Copying Nginx configuration template for admin UI to temporary location..."
    cp ${SCRIPT_DIR}/template/nginx.admin.conf.template ${DEPLOY_DIR}/nginx.admin.conf.tmp

    echo "Updating Nginx configuration for admin UI in temporary file..."
    sed -i "s|{{BACKEND_HOST}}|$(escape_sed_replacement_string "${BACKEND_HOST}")|g" ${DEPLOY_DIR}/nginx.admin.conf.tmp
    sed -i "s|{{SERVER_PORT}}|$(escape_sed_replacement_string "${SERVER_PORT}")|g" ${DEPLOY_DIR}/nginx.admin.conf.tmp

    echo "Moving updated Nginx configuration for admin UI to final location..."
    mv ${DEPLOY_DIR}/nginx.admin.conf.tmp ${DEPLOY_DIR}/ruoyi-admin/nginx.conf

    echo "Copying Nginx configuration template for web UI to temporary location..."
    cp ${SCRIPT_DIR}/template/nginx.web.conf.template ${DEPLOY_DIR}/nginx.web.conf.tmp

    echo "Updating Nginx configuration for web UI in temporary file..."
    sed -i "s|{{BACKEND_HOST}}|$(escape_sed_replacement_string "${BACKEND_HOST}")|g" ${DEPLOY_DIR}/nginx.web.conf.tmp
    sed -i "s|{{SERVER_PORT}}|$(escape_sed_replacement_string "${SERVER_PORT}")|g" ${DEPLOY_DIR}/nginx.web.conf.tmp

    echo "Moving updated Nginx configuration for web UI to final location..."
    mv ${DEPLOY_DIR}/nginx.web.conf.tmp ${DEPLOY_DIR}/ruoyi-web/nginx.conf

    # Create Dockerfiles for frontend services
    echo "Creating Dockerfile for admin UI..."
    cat > ${DEPLOY_DIR}/ruoyi-admin/Dockerfile << EOF
FROM nginx:1.25-alpine

COPY dist/ /usr/share/nginx/html/
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
EOF

    echo "Creating Dockerfile for web UI..."
    cat > ${DEPLOY_DIR}/ruoyi-web/Dockerfile << EOF
FROM nginx:1.25-alpine

COPY dist/ /usr/share/nginx/html/
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
EOF

    # Build backend service
    echo "Building Ruoyi-AI backend service..."
    cd ${DEPLOY_DIR}/ruoyi-ai
    docker run -it --rm --name build-ruoyi-ai-backend -v ${DEPLOY_DIR}/ruoyi-ai:/code --entrypoint=/bin/bash maven:3.9.9-eclipse-temurin-17-alpine -c "cd /code && mvn clean package -P prod"

    # Build frontend admin service
    echo "Building Ruoyi-AI frontend admin service..."
    cd ${DEPLOY_DIR}/ruoyi-admin
    docker run -it --rm --name build-ruoyi-ai-admin -v ${DEPLOY_DIR}/ruoyi-admin:/app -w /app node:20 sh -c "npm install -g pnpm && pnpm install && pnpm build"

    # Build frontend web service
    echo "Building Ruoyi-AI frontend web service..."
    cd ${DEPLOY_DIR}/ruoyi-web
    docker run -it --rm --name build-ruoyi-ai-web -v ${DEPLOY_DIR}/ruoyi-web:/app -w /app node:20 sh -c "npm install -g pnpm && pnpm install && pnpm build"

    # Build Docker images
    echo "Building Ruoyi-AI Backend Docker images..."
    cd ${DEPLOY_DIR}/ruoyi-ai
    cp ./ruoyi-admin/target/ruoyi-admin.jar ${DEPLOY_DIR}/
    cd ${DEPLOY_DIR}
    cat > Dockerfile << EOF
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY ruoyi-admin.jar /app/ruoyi-admin.jar
EXPOSE ${SERVER_PORT}
ENTRYPOINT ["java","-jar","ruoyi-admin.jar","--spring.profiles.active=prod"]
EOF
    docker build -t ruoyi-ai-backend:latest .

    echo "Building Ruoyi-AI Admin Docker images..."
    cd ${DEPLOY_DIR}/ruoyi-admin
    rm -rf temp
    mkdir temp
    cp ./apps/web-antd/dist.zip temp/
    cp Dockerfile temp/
    cp nginx.conf temp/
    cd temp
    unzip dist.zip -d dist
    rm -f dist.zip
    docker build -t ruoyi-admin:latest .

    echo "Building Ruoyi-AI Web Docker images..."
    cd ${DEPLOY_DIR}/ruoyi-web
    docker build -t ruoyi-web:latest .
else
    echo "Skipping image build process. Proceeding directly to deployment..."
fi

# Copy SQL file
rm -rf ${DEPLOY_DIR}/mysql-init
cp -pr ${SCRIPT_DIR}/mysql-init ${DEPLOY_DIR}/

# Update SQL file with configuration values
echo "Updating SQL configuration values..."
sed -i "s|'weaviate', 'host', '127.0.0.1:6038'|'weaviate', 'host', 'weaviate:8080'|g" ${DEPLOY_DIR}/mysql-init/01_ruoyi-ai.sql
sed -i "s|'weaviate', 'protocol', 'http'|'weaviate', 'protocol', '${WEAVIATE_PROTOCOL}'|g" ${DEPLOY_DIR}/mysql-init/01_ruoyi-ai.sql
sed -i "s|'weaviate', 'classname', 'LocalKnowledge'|'weaviate', 'classname', '${WEAVIATE_CLASSNAME}'|g" ${DEPLOY_DIR}/mysql-init/01_ruoyi-ai.sql

# Deploy with Docker Compose
echo "Deploying with Docker Compose..."
cd ${DEPLOY_DIR}
docker-compose down
docker-compose up -d

echo "=================================================="
echo "   RuoYi-AI Deployment Complete"
echo "=================================================="
echo ""
echo "Your RuoYi-AI system has been deployed with the following services:"
echo "- Backend API: http://localhost:${SERVER_PORT}"
echo "- Admin UI: http://localhost:${ADMIN_PORT}"
echo "- Web UI: http://localhost:${WEB_PORT}"
echo "- Weaviate: http://localhost:${WEAVIATE_HTTP_PORT}"
echo ""
echo "All configurations have been customized according to your inputs."
echo "Configuration files have been updated to use environment variables."
echo ""
echo "Thank you for using the RuoYi-AI Interactive Deployment Script!"
