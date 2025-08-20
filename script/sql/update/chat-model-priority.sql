alter table chat_model
    add priority int default 1 null comment '模型优先级(值越大优先级越高)';

UPDATE `ruoyi-ai`.chat_model t
SET t.priority = 3
WHERE t.id = 1782792839548735492;

UPDATE `ruoyi-ai`.chat_model t
SET t.priority = 6
WHERE t.id = 1859570229117022212;

UPDATE `ruoyi-ai`.chat_model t
SET t.priority = 5
WHERE t.id = 1859570229117022211;

UPDATE `ruoyi-ai`.chat_model t
SET t.priority = 4
WHERE t.id = 1782792839548735493;

UPDATE `ruoyi-ai`.chat_model t
SET t.priority = 2
WHERE t.id = 1828324413241466881;

UPDATE `ruoyi-ai`.chat_model t
SET t.priority = 2
WHERE t.id = 1782792839548735491;