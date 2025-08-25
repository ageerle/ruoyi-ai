-- 默认开启本地minio
UPDATE `ruoyi-ai`.sys_oss_config t
SET t.status = '1'
WHERE t.oss_config_id = 4;

UPDATE `ruoyi-ai`.sys_oss_config t
SET t.status = '0'
WHERE t.oss_config_id = 1;