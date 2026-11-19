START TRANSACTION;

INSERT INTO sys_dict_data
    (dict_code, tenant_id, dict_sort, dict_label, dict_value, dict_type, list_class, is_default, create_dept, create_by, create_time)
SELECT 2099111900010000202, dict.tenant_id, 6, '图片识字', 'ocr', dict.dict_type, 'blue', 'N', 103, 1, NOW()
FROM sys_dict_type AS dict
WHERE dict.tenant_id = '000000' AND dict.dict_type = 'chat_model_category'
  AND NOT EXISTS (
      SELECT 1 FROM sys_dict_data AS existing
      WHERE existing.tenant_id = dict.tenant_id AND existing.dict_type = dict.dict_type AND existing.dict_value = 'ocr'
  );

COMMIT;
