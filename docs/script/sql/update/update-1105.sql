INSERT INTO ruoyi_ai_agent.sys_config
(config_id, tenant_id, config_name, config_key, config_value, config_type, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES(2118173392017911809, '000000', '大模型对话挡板', 'mock', 'true', 'N', 103, 1, '2026-11-05 10:47:45', 1, '2026-11-05 10:47:45', 'true：开启，false：关闭（默认关闭）');

-- token 永不过期
UPDATE sys_client
SET timeout = -1, active_timeout = -1
WHERE del_flag = '0';
