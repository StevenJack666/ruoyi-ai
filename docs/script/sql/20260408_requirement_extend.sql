SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 需求项目表
CREATE TABLE IF NOT EXISTS `req_ext_project` (
    `id` bigint NOT NULL COMMENT '主键',
    `project_code` varchar(64) NOT NULL COMMENT '项目编码',
    `project_name` varchar(128) NOT NULL COMMENT '项目名称',
    `owner_id` bigint DEFAULT NULL COMMENT '负责人',
    `status` char(1) DEFAULT '0' COMMENT '状态(0正常 1停用)',
    `description` varchar(500) DEFAULT NULL COMMENT '描述',
    `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
    `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime DEFAULT NULL COMMENT '创建时间',
    `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime DEFAULT NULL COMMENT '更新时间',
    `remark` varchar(500) DEFAULT NULL COMMENT '备注',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户ID',
    `del_flag` char(1) DEFAULT '0' COMMENT '删除标志(0存在 1删除)',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `uk_req_ext_project_code_tenant` (`project_code`, `tenant_id`) USING BTREE,
    KEY `idx_req_ext_project_status` (`status`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='需求项目表';

-- 需求表
CREATE TABLE IF NOT EXISTS `req_ext_requirement` (
    `id` bigint NOT NULL COMMENT '主键',
    `project_id` bigint NOT NULL COMMENT '项目ID',
    `req_code` varchar(64) NOT NULL COMMENT '需求编码',
    `title` varchar(255) NOT NULL COMMENT '需求标题',
    `type` varchar(32) DEFAULT NULL COMMENT '需求类型',
    `priority` varchar(32) DEFAULT NULL COMMENT '优先级',
    `status` varchar(32) NOT NULL DEFAULT 'draft' COMMENT '状态(draft/reviewing/in_progress/closed)',
    `owner_id` bigint DEFAULT NULL COMMENT '负责人',
    `assignee_id` bigint DEFAULT NULL COMMENT '指派人',
    `source` varchar(64) DEFAULT NULL COMMENT '来源',
    `plan_start_time` datetime DEFAULT NULL COMMENT '计划开始时间',
    `plan_end_time` datetime DEFAULT NULL COMMENT '计划结束时间',
    `content` text COMMENT '需求内容',
    `flow_code` varchar(64) DEFAULT NULL COMMENT '流程编码(预留)',
    `process_instance_id` bigint DEFAULT NULL COMMENT '流程实例ID(预留)',
    `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
    `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime DEFAULT NULL COMMENT '创建时间',
    `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime DEFAULT NULL COMMENT '更新时间',
    `remark` varchar(500) DEFAULT NULL COMMENT '备注',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户ID',
    `del_flag` char(1) DEFAULT '0' COMMENT '删除标志(0存在 1删除)',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `uk_req_ext_req_code_tenant` (`req_code`, `tenant_id`) USING BTREE,
    KEY `idx_req_ext_req_project` (`project_id`) USING BTREE,
    KEY `idx_req_ext_req_status` (`status`) USING BTREE,
    KEY `idx_req_ext_req_priority` (`priority`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='需求表';

-- 需求评论表
CREATE TABLE IF NOT EXISTS `req_ext_requirement_comment` (
    `id` bigint NOT NULL COMMENT '主键',
    `requirement_id` bigint NOT NULL COMMENT '需求ID',
    `content` text NOT NULL COMMENT '评论内容',
    `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
    `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime DEFAULT NULL COMMENT '创建时间',
    `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime DEFAULT NULL COMMENT '更新时间',
    `remark` varchar(500) DEFAULT NULL COMMENT '备注',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户ID',
    `del_flag` char(1) DEFAULT '0' COMMENT '删除标志(0存在 1删除)',
    PRIMARY KEY (`id`) USING BTREE,
    KEY `idx_req_ext_comment_req` (`requirement_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='需求评论表';

-- 需求历史表
CREATE TABLE IF NOT EXISTS `req_ext_requirement_history` (
    `id` bigint NOT NULL COMMENT '主键',
    `requirement_id` bigint NOT NULL COMMENT '需求ID',
    `action_type` varchar(32) NOT NULL COMMENT '动作类型',
    `field_name` varchar(64) DEFAULT NULL COMMENT '变更字段',
    `old_value` varchar(1000) DEFAULT NULL COMMENT '旧值',
    `new_value` varchar(1000) DEFAULT NULL COMMENT '新值',
    `action_remark` varchar(500) DEFAULT NULL COMMENT '动作说明',
    `operator_id` bigint DEFAULT NULL COMMENT '操作人ID',
    `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
    `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime DEFAULT NULL COMMENT '创建时间',
    `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime DEFAULT NULL COMMENT '更新时间',
    `remark` varchar(500) DEFAULT NULL COMMENT '备注',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户ID',
    `del_flag` char(1) DEFAULT '0' COMMENT '删除标志(0存在 1删除)',
    PRIMARY KEY (`id`) USING BTREE,
    KEY `idx_req_ext_history_req` (`requirement_id`) USING BTREE,
    KEY `idx_req_ext_history_action` (`action_type`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='需求历史表';

-- 字典类型初始化
INSERT INTO `sys_dict_type` (`dict_id`, `dict_name`, `dict_type`, `create_dept`, `create_by`, `create_time`, `remark`, `tenant_id`)
SELECT 2026040801, '需求状态', 'req_ext_status', 103, 1, NOW(), '需求跟踪扩展模块', '000000'
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict_type` WHERE `dict_type` = 'req_ext_status' AND `tenant_id` = '000000');

INSERT INTO `sys_dict_type` (`dict_id`, `dict_name`, `dict_type`, `create_dept`, `create_by`, `create_time`, `remark`, `tenant_id`)
SELECT 2026040802, '需求优先级', 'req_ext_priority', 103, 1, NOW(), '需求跟踪扩展模块', '000000'
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict_type` WHERE `dict_type` = 'req_ext_priority' AND `tenant_id` = '000000');

INSERT INTO `sys_dict_type` (`dict_id`, `dict_name`, `dict_type`, `create_dept`, `create_by`, `create_time`, `remark`, `tenant_id`)
SELECT 2026040803, '需求类型', 'req_ext_type', 103, 1, NOW(), '需求跟踪扩展模块', '000000'
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict_type` WHERE `dict_type` = 'req_ext_type' AND `tenant_id` = '000000');

-- 字典数据初始化
INSERT INTO `sys_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `create_dept`, `create_by`, `create_time`, `remark`, `tenant_id`)
SELECT 202604080101, 1, '草稿', 'draft', 'req_ext_status', '', 'default', 'N', 103, 1, NOW(), '需求状态', '000000'
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict_data` WHERE `dict_type` = 'req_ext_status' AND `dict_value` = 'draft' AND `tenant_id` = '000000');

INSERT INTO `sys_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `create_dept`, `create_by`, `create_time`, `remark`, `tenant_id`)
SELECT 202604080102, 2, '待评审', 'reviewing', 'req_ext_status', '', 'warning', 'N', 103, 1, NOW(), '需求状态', '000000'
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict_data` WHERE `dict_type` = 'req_ext_status' AND `dict_value` = 'reviewing' AND `tenant_id` = '000000');

INSERT INTO `sys_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `create_dept`, `create_by`, `create_time`, `remark`, `tenant_id`)
SELECT 202604080103, 3, '进行中', 'in_progress', 'req_ext_status', '', 'primary', 'N', 103, 1, NOW(), '需求状态', '000000'
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict_data` WHERE `dict_type` = 'req_ext_status' AND `dict_value` = 'in_progress' AND `tenant_id` = '000000');

INSERT INTO `sys_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `create_dept`, `create_by`, `create_time`, `remark`, `tenant_id`)
SELECT 202604080104, 4, '已关闭', 'closed', 'req_ext_status', '', 'danger', 'N', 103, 1, NOW(), '需求状态', '000000'
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict_data` WHERE `dict_type` = 'req_ext_status' AND `dict_value` = 'closed' AND `tenant_id` = '000000');

INSERT INTO `sys_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `create_dept`, `create_by`, `create_time`, `remark`, `tenant_id`)
SELECT 202604080201, 1, '低', 'low', 'req_ext_priority', '', 'default', 'N', 103, 1, NOW(), '需求优先级', '000000'
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict_data` WHERE `dict_type` = 'req_ext_priority' AND `dict_value` = 'low' AND `tenant_id` = '000000');

INSERT INTO `sys_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `create_dept`, `create_by`, `create_time`, `remark`, `tenant_id`)
SELECT 202604080202, 2, '中', 'medium', 'req_ext_priority', '', 'warning', 'N', 103, 1, NOW(), '需求优先级', '000000'
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict_data` WHERE `dict_type` = 'req_ext_priority' AND `dict_value` = 'medium' AND `tenant_id` = '000000');

INSERT INTO `sys_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `create_dept`, `create_by`, `create_time`, `remark`, `tenant_id`)
SELECT 202604080203, 3, '高', 'high', 'req_ext_priority', '', 'danger', 'N', 103, 1, NOW(), '需求优先级', '000000'
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict_data` WHERE `dict_type` = 'req_ext_priority' AND `dict_value` = 'high' AND `tenant_id` = '000000');

INSERT INTO `sys_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `create_dept`, `create_by`, `create_time`, `remark`, `tenant_id`)
SELECT 202604080301, 1, '业务需求', 'business', 'req_ext_type', '', 'default', 'N', 103, 1, NOW(), '需求类型', '000000'
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict_data` WHERE `dict_type` = 'req_ext_type' AND `dict_value` = 'business' AND `tenant_id` = '000000');

INSERT INTO `sys_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `create_dept`, `create_by`, `create_time`, `remark`, `tenant_id`)
SELECT 202604080302, 2, '技术需求', 'tech', 'req_ext_type', '', 'primary', 'N', 103, 1, NOW(), '需求类型', '000000'
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict_data` WHERE `dict_type` = 'req_ext_type' AND `dict_value` = 'tech' AND `tenant_id` = '000000');

SET FOREIGN_KEY_CHECKS = 1;
