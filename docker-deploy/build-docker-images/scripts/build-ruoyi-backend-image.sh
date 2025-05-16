#!/bin/bash
rm -f /root/ruoyi-ai-docker/source-code/ruoyi-ai-backend/ruoyi-admin/src/main/resources/application-prod.yml
cp /root/ruoyi-ai-docker/build-docker-images/modify-source-code/ruoyi-ai-backend/application-prod.yml /root/ruoyi-ai-docker/source-code/ruoyi-ai-backend/ruoyi-admin/src/main/resources/application-prod.yml
docker run --rm --name build-ruoyi-ai-backend -v /root/ruoyi-ai-docker/source-code/ruoyi-ai-backend:/app -w /app maven:3.9.9-eclipse-temurin-17-alpine bash -c "mvn clean package -Pprod"
rm -f /root/ruoyi-ai-docker/build-docker-images/Dockerfile-Resources/ruoyi-ai-backend/ruoyi-admin.jar
cp /root/ruoyi-ai-docker/source-code/ruoyi-ai-backend/ruoyi-admin/target/ruoyi-admin.jar /root/ruoyi-ai-docker/build-docker-images/Dockerfile-Resources/ruoyi-ai-backend/
cd /root/ruoyi-ai-docker/build-docker-images/Dockerfile-Resources/ruoyi-ai-backend/
docker build -t ruoyi-ai-backend:v2.0.5 .
