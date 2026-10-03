-- Run after the base ruoyi-ai.sql. Preserves existing scripts and provider keys.
DROP PROCEDURE IF EXISTS migrate_drama_preparation;
DELIMITER $$
CREATE PROCEDURE migrate_drama_preparation()
BEGIN
 IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='short_drama_script' AND column_name='creation_mode') THEN
  ALTER TABLE short_drama_script ADD creation_mode varchar(32) DEFAULT 'standard', ADD source_materials longtext, ADD worldbuilding longtext, ADD revision_notes longtext;
 END IF;
END$$
DELIMITER ;
CALL migrate_drama_preparation();
DROP PROCEDURE migrate_drama_preparation;

INSERT INTO chat_model (id,category,model_name,provider_code,model_describe,model_show,api_host,api_key,create_dept,create_by,create_time,update_by,update_time,remark,tenant_id)
SELECT 2106002500000000001,'video','bytedance/seedance-2.5/reference-to-video','atlas','Seedance 2.5 多模态参考生视频（字节跳动）','Y',
 COALESCE((SELECT api_host FROM (SELECT api_host FROM chat_model WHERE provider_code='atlas' AND category='video' AND tenant_id=0 ORDER BY id DESC LIMIT 1) h),'https://api.atlascloud.ai/api/v1'),
 COALESCE((SELECT api_key FROM (SELECT api_key FROM chat_model WHERE provider_code='atlas' AND category='video' AND tenant_id=0 ORDER BY id DESC LIMIT 1) k),''),
 103,1,NOW(),1,NOW(),'Atlas Seedance 2.5 reference-to-video; 4-30s, up to 30 images and 10 audio references',0
WHERE NOT EXISTS (SELECT 1 FROM chat_model WHERE model_name='bytedance/seedance-2.5/reference-to-video' AND tenant_id=0);
