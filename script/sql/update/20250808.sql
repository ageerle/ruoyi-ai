-- 聊天模型表添加模型能力字段
alter table chat_model
    add model_capability varchar(255) default '[]' not null comment '模型能力';
