-- 更新开源版的知识库检索节点说明。
UPDATE t_workflow_component
SET remark = '根据输入在知识库中检索相关片段。'
WHERE uuid = 'b8e3c1d55f6a4d92b0e7f214d3c58a29'
  AND name = 'KnowledgeRetrieval'
  AND tenant_id = '000000'
  AND is_deleted = 0
  AND remark = '从知识库中检索相关内容，支持向量检索和混合检索';
