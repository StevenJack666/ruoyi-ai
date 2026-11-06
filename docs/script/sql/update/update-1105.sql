INSERT INTO ruoyi_ai_agent.sys_config
(config_id, tenant_id, config_name, config_key, config_value, config_type, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES(2118173392017911809, '000000', '大模型对话挡板', 'mock', 'true', 'N', 103, 1, '2026-11-05 10:47:45', 1, '2026-11-05 10:47:45', 'true：开启，false：关闭（默认关闭）');

-- token 永不过期
UPDATE sys_client
SET timeout = -1, active_timeout = -1
WHERE del_flag = '0';

-- 向量库配置
INSERT INTO ruoyi_ai_agent.sys_dict_type
(dict_id, tenant_id, dict_name, dict_type, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES(2118520927240663041, '000000', '向量库', 'vector_store', 103, 1, '2026-11-06 09:48:44', 1, '2026-11-06 09:51:41', NULL);

INSERT INTO ruoyi_ai_agent.sys_dict_data
(dict_code, tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES(2118521092374605826, '000000', 0, 'memory', 'memory', 'vector_store', NULL, '', 'N', 103, 1, '2026-11-06 09:49:23', 1, '2026-11-06 09:51:22', NULL);
