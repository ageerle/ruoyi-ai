START TRANSACTION;

INSERT INTO sys_dict_data
    (dict_code, tenant_id, dict_sort, dict_label, dict_value, dict_type, list_class, is_default, create_dept, create_by, create_time)
SELECT option_value.dict_code, dict.tenant_id, option_value.dict_sort, option_value.dict_label,
       option_value.dict_value, dict.dict_type, option_value.list_class, 'N', 103, 1, NOW()
FROM sys_dict_type AS dict
CROSS JOIN (
    SELECT 2099111900010000200 AS dict_code, 3 AS dict_sort, '语音' AS dict_label, 'audio' AS dict_value, 'purple' AS list_class
    UNION ALL
    SELECT 2099111900010000201, 5, '视频', 'video', 'red'
) AS option_value
WHERE dict.tenant_id = '000000' AND dict.dict_type = 'chat_model_category'
  AND NOT EXISTS (
      SELECT 1 FROM sys_dict_data AS existing
      WHERE existing.tenant_id = dict.tenant_id AND existing.dict_type = dict.dict_type AND existing.dict_value = option_value.dict_value
  );

COMMIT;
