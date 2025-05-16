#!/bin/bash
rm -f /root/ruoyi-ai-docker/deploy/mysql-init/*.sql
cp /root/ruoyi-ai-docker/source-code/ruoyi-ai-backend/script/sql/ruoyi-ai.sql /root/ruoyi-ai-docker/deploy/mysql-init/01_ruoyi-ai.sql
cp /root/ruoyi-ai-docker/source-code/ruoyi-ai-backend/script/sql/update/20250407.sql /root/ruoyi-ai-docker/deploy/mysql-init/02_update_20250407.sql
cp /root/ruoyi-ai-docker/source-code/ruoyi-ai-backend/script/sql/update/20250505.sql /root/ruoyi-ai-docker/deploy/mysql-init/03_update_20250505.sql
cp /root/ruoyi-ai-docker/source-code/ruoyi-ai-backend/script/sql/update/20250509.sql /root/ruoyi-ai-docker/deploy/mysql-init/04_update_20250509.sql
