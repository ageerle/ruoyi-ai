#!/bin/bash
docker run --rm --name build-ruoyi-ai-web -v /root/ruoyi-ai-docker/source-code/ruoyi-ai-web:/app -w /app node:20 bash -c "npm install -g pnpm && pnpm install && pnpm approve-builds && pnpm build"
rm -rf /root/ruoyi-ai-docker/build-docker-images/Dockerfile-Resources/ruoyi-ai-web/dist
cp -pr /root/ruoyi-ai-docker/source-code/ruoyi-ai-web/dist /root/ruoyi-ai-docker/build-docker-images/Dockerfile-Resources/ruoyi-ai-web/
cd /root/ruoyi-ai-docker/build-docker-images/Dockerfile-Resources/ruoyi-ai-web/
docker build -t ruoyi-ai-web:v2.0.5 .
