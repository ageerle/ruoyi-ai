#!/bin/bash
rm -f /root/ruoyi-ai-docker/source-code/ruoyi-ai-admin/apps/web-antd/.env.analyze
rm -f /root/ruoyi-ai-docker/source-code/ruoyi-ai-admin/apps/web-antd/.env.development
rm -f /root/ruoyi-ai-docker/source-code/ruoyi-ai-admin/apps/web-antd/.env.production
rm -f /root/ruoyi-ai-docker/source-code/ruoyi-ai-admin/apps/web-antd/.env.test
rm -f /root/ruoyi-ai-docker/source-code/ruoyi-ai-admin/apps/web-antd/vite.config.mts

cp /root/ruoyi-ai-docker/build-docker-images/modify-source-code/ruoyi-ai-admin/.env.analyze /root/ruoyi-ai-docker/source-code/ruoyi-ai-admin/apps/web-antd/
cp /root/ruoyi-ai-docker/build-docker-images/modify-source-code/ruoyi-ai-admin/.env.development /root/ruoyi-ai-docker/source-code/ruoyi-ai-admin/apps/web-antd/
cp /root/ruoyi-ai-docker/build-docker-images/modify-source-code/ruoyi-ai-admin/.env.production /root/ruoyi-ai-docker/source-code/ruoyi-ai-admin/apps/web-antd/
cp /root/ruoyi-ai-docker/build-docker-images/modify-source-code/ruoyi-ai-admin/.env.test /root/ruoyi-ai-docker/source-code/ruoyi-ai-admin/apps/web-antd/
cp /root/ruoyi-ai-docker/build-docker-images/modify-source-code/ruoyi-ai-admin/vite.config.mts /root/ruoyi-ai-docker/source-code/ruoyi-ai-admin/apps/web-antd/

docker run --rm --name build-ruoyi-ai-admin -v /root/ruoyi-ai-docker/source-code/ruoyi-ai-admin:/app -w /app node:20 bash -c "npm install -g pnpm && pnpm install && pnpm build"

rm -f /root/ruoyi-ai-docker/build-docker-images/Dockerfile-Resources/ruoyi-ai-admin/dist.zip
cp /root/ruoyi-ai-docker/source-code/ruoyi-ai-admin/apps/web-antd/dist.zip /root/ruoyi-ai-docker/build-docker-images/Dockerfile-Resources/ruoyi-ai-admin/
cd /root/ruoyi-ai-docker/build-docker-images/Dockerfile-Resources/ruoyi-ai-admin/
rm -rf dist
unzip dist.zip -d dist
rm -f dist.zip
docker build -t ruoyi-ai-admin:v2.0.5 .
