#!/bin/bash
rm -f /root/ruoyi-ai-docker/deploy/mysql-init/*.sql
cp /root/ruoyi-ai-docker/source-code/ruoyi-ai-backend/script/sql/ruoyi-ai.sql /root/ruoyi-ai-docker/deploy/mysql-init/ruoyi-ai.sql

