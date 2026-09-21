-- 知识库检索工作流节点注册
-- 为新安装和已有环境提供幂等的节点注册

INSERT INTO `t_workflow_component`
    (`uuid`, `name`, `title`, `remark`, `display_order`, `is_enable`,
     `create_time`, `update_time`, `is_deleted`, `tenant_id`)
SELECT
    'b8e3c1d55f6a4d92b0e7f214d3c58a29',
    'KnowledgeRetrieval',
    '知识库检索',
    '从知识库中检索相关内容，支持向量检索和混合检索',
    30,
    1,
    NOW(),
    NOW(),
    0,
    '000000'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1
    FROM `t_workflow_component`
    WHERE `name` = 'KnowledgeRetrieval'
      AND `tenant_id` = '000000'
);
