#!/bin/bash
sed -i 's#127.0.0.1:6038#weaviate:8080#g' ./mysql-init/01_ruoyi-ai.sql
