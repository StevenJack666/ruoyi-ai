CREATE TABLE `agent_info` (
                              `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '智能体ID',
                              `agent_name` VARCHAR(255) DEFAULT NULL COMMENT '智能体名称',
                              `agent_describe` VARCHAR(500) DEFAULT NULL COMMENT '智能体描述（下拉展示用）',
                              `agent_show` VARCHAR(500) DEFAULT NULL COMMENT '展示图标/头像URL',
                              `model_id` BIGINT DEFAULT NULL COMMENT '绑定的聊天模型ID（chat_model.id, category=chat）',
                              `enable_thinking` VARCHAR(10) DEFAULT '0' COMMENT '是否启用深度思考(ReAct多子Agent)：0 否 1 是',
                              `system_prompt` TEXT COMMENT '自定义系统提示词',

    -- 兼容 MySQL 5.7 以下版本，使用 TEXT 类型存储 JSON 数组字符串
                              `mcp_tool_ids` TEXT COMMENT '关联MCP工具ID列表（JSON数组，[Long]）',
                              `skill_names` TEXT COMMENT '关联磁盘技能名列表（JSON数组，[String]）',
                              `knowledge_ids` TEXT COMMENT '关联知识库ID列表（JSON数组，[Long]）',

                              `status` VARCHAR(10) DEFAULT '0' COMMENT '状态：0 正常 1 停用',
                              `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',

    -- 以下为继承自 TenantEntity 的通用字段
                              `tenant_id` BIGINT DEFAULT NULL COMMENT '租户ID',
                              `create_dept` BIGINT DEFAULT NULL COMMENT '创建部门ID',
                              `create_by` BIGINT DEFAULT NULL COMMENT '创建者ID',
                              `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                              `update_by` BIGINT DEFAULT NULL COMMENT '更新者ID',
                              `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

                              PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='智能体信息表';

INSERT INTO ruoyi_ai_agent.sys_config
(config_id, tenant_id, config_name, config_key, config_value, config_type, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES(2112382154769518593, '000000', '上传路径地址', 'sys.upload.path', '', 'N', 103, 1, '2026-10-20 11:15:27', 1, '2026-10-20 11:23:59', NULL);

ALTER TABLE session_upload_record
    ADD COLUMN oss_url VARCHAR(1000) DEFAULT NULL COMMENT 'OSS文件访问地址'
AFTER oss_id;

ALTER TABLE session_upload_record
ADD COLUMN create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
ADD COLUMN update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
ADD COLUMN is_deleted TINYINT DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除';

ALTER TABLE session_upload_record
MODIFY COLUMN session_id BIGINT DEFAULT NULL COMMENT '会话ID';
