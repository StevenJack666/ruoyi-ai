SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 项目Bug表
CREATE TABLE IF NOT EXISTS `req_ext_project_bug` (
    `id` bigint NOT NULL COMMENT '主键',
    `project_id` bigint NOT NULL COMMENT '项目ID',
    `bug_code` varchar(64) NOT NULL COMMENT 'Bug编码',
    `title` varchar(255) NOT NULL COMMENT 'Bug标题',
    `severity` varchar(32) DEFAULT NULL COMMENT '严重程度',
    `priority` varchar(32) DEFAULT NULL COMMENT '优先级',
    `status` varchar(32) NOT NULL DEFAULT 'open' COMMENT '状态(open/in_progress/resolved/closed)',
    `owner_id` bigint DEFAULT NULL COMMENT '负责人',
    `assignee_id` bigint DEFAULT NULL COMMENT '指派人',
    `found_version` varchar(64) DEFAULT NULL COMMENT '发现版本',
    `fixed_version` varchar(64) DEFAULT NULL COMMENT '修复版本',
    `reproduce_steps` text COMMENT '复现步骤',
    `expected_result` text COMMENT '预期结果',
    `actual_result` text COMMENT '实际结果',
    `resolved_time` datetime DEFAULT NULL COMMENT '解决时间',
    `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
    `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime DEFAULT NULL COMMENT '创建时间',
    `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime DEFAULT NULL COMMENT '更新时间',
    `remark` varchar(500) DEFAULT NULL COMMENT '备注',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户ID',
    `del_flag` char(1) DEFAULT '0' COMMENT '删除标志(0存在 1删除)',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `uk_req_ext_bug_code_tenant` (`bug_code`, `tenant_id`) USING BTREE,
    KEY `idx_req_ext_bug_project` (`project_id`) USING BTREE,
    KEY `idx_req_ext_bug_status` (`status`) USING BTREE,
    KEY `idx_req_ext_bug_severity` (`severity`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目Bug表';

-- 字典类型初始化
INSERT INTO `sys_dict_type` (`dict_id`, `dict_name`, `dict_type`, `status`, `create_dept`, `create_by`, `create_time`, `remark`, `tenant_id`)
SELECT 2026040810, 'Bug状态', 'req_ext_bug_status', '0', 103, 1, NOW(), '需求跟踪Bug扩展模块', 0
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict_type` WHERE `dict_type` = 'req_ext_bug_status' AND `tenant_id` = 0);

INSERT INTO `sys_dict_type` (`dict_id`, `dict_name`, `dict_type`, `status`, `create_dept`, `create_by`, `create_time`, `remark`, `tenant_id`)
SELECT 2026040811, 'Bug严重程度', 'req_ext_bug_severity', '0', 103, 1, NOW(), '需求跟踪Bug扩展模块', 0
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict_type` WHERE `dict_type` = 'req_ext_bug_severity' AND `tenant_id` = 0);

INSERT INTO `sys_dict_type` (`dict_id`, `dict_name`, `dict_type`, `status`, `create_dept`, `create_by`, `create_time`, `remark`, `tenant_id`)
SELECT 2026040812, 'Bug优先级', 'req_ext_bug_priority', '0', 103, 1, NOW(), '需求跟踪Bug扩展模块', 0
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict_type` WHERE `dict_type` = 'req_ext_bug_priority' AND `tenant_id` = 0);

-- 字典数据初始化
INSERT INTO `sys_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `status`, `create_dept`, `create_by`, `create_time`, `remark`, `tenant_id`)
SELECT 202604081001, 1, '待处理', 'open', 'req_ext_bug_status', '0', 103, 1, NOW(), 'Bug状态', 0
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict_data` WHERE `dict_type` = 'req_ext_bug_status' AND `dict_value` = 'open' AND `tenant_id` = 0);

INSERT INTO `sys_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `status`, `create_dept`, `create_by`, `create_time`, `remark`, `tenant_id`)
SELECT 202604081002, 2, '处理中', 'in_progress', 'req_ext_bug_status', '0', 103, 1, NOW(), 'Bug状态', 0
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict_data` WHERE `dict_type` = 'req_ext_bug_status' AND `dict_value` = 'in_progress' AND `tenant_id` = 0);

INSERT INTO `sys_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `status`, `create_dept`, `create_by`, `create_time`, `remark`, `tenant_id`)
SELECT 202604081003, 3, '已解决', 'resolved', 'req_ext_bug_status', '0', 103, 1, NOW(), 'Bug状态', 0
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict_data` WHERE `dict_type` = 'req_ext_bug_status' AND `dict_value` = 'resolved' AND `tenant_id` = 0);

INSERT INTO `sys_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `status`, `create_dept`, `create_by`, `create_time`, `remark`, `tenant_id`)
SELECT 202604081004, 4, '已关闭', 'closed', 'req_ext_bug_status', '0', 103, 1, NOW(), 'Bug状态', 0
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict_data` WHERE `dict_type` = 'req_ext_bug_status' AND `dict_value` = 'closed' AND `tenant_id` = 0);

INSERT INTO `sys_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `status`, `create_dept`, `create_by`, `create_time`, `remark`, `tenant_id`)
SELECT 202604081101, 1, '致命', 'critical', 'req_ext_bug_severity', '0', 103, 1, NOW(), 'Bug严重程度', 0
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict_data` WHERE `dict_type` = 'req_ext_bug_severity' AND `dict_value` = 'critical' AND `tenant_id` = 0);

INSERT INTO `sys_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `status`, `create_dept`, `create_by`, `create_time`, `remark`, `tenant_id`)
SELECT 202604081102, 2, '严重', 'major', 'req_ext_bug_severity', '0', 103, 1, NOW(), 'Bug严重程度', 0
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict_data` WHERE `dict_type` = 'req_ext_bug_severity' AND `dict_value` = 'major' AND `tenant_id` = 0);

INSERT INTO `sys_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `status`, `create_dept`, `create_by`, `create_time`, `remark`, `tenant_id`)
SELECT 202604081103, 3, '一般', 'normal', 'req_ext_bug_severity', '0', 103, 1, NOW(), 'Bug严重程度', 0
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict_data` WHERE `dict_type` = 'req_ext_bug_severity' AND `dict_value` = 'normal' AND `tenant_id` = 0);

INSERT INTO `sys_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `status`, `create_dept`, `create_by`, `create_time`, `remark`, `tenant_id`)
SELECT 202604081104, 4, '轻微', 'minor', 'req_ext_bug_severity', '0', 103, 1, NOW(), 'Bug严重程度', 0
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict_data` WHERE `dict_type` = 'req_ext_bug_severity' AND `dict_value` = 'minor' AND `tenant_id` = 0);

INSERT INTO `sys_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `status`, `create_dept`, `create_by`, `create_time`, `remark`, `tenant_id`)
SELECT 202604081201, 1, '低', 'low', 'req_ext_bug_priority', '0', 103, 1, NOW(), 'Bug优先级', 0
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict_data` WHERE `dict_type` = 'req_ext_bug_priority' AND `dict_value` = 'low' AND `tenant_id` = 0);

INSERT INTO `sys_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `status`, `create_dept`, `create_by`, `create_time`, `remark`, `tenant_id`)
SELECT 202604081202, 2, '中', 'medium', 'req_ext_bug_priority', '0', 103, 1, NOW(), 'Bug优先级', 0
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict_data` WHERE `dict_type` = 'req_ext_bug_priority' AND `dict_value` = 'medium' AND `tenant_id` = 0);

INSERT INTO `sys_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `status`, `create_dept`, `create_by`, `create_time`, `remark`, `tenant_id`)
SELECT 202604081203, 3, '高', 'high', 'req_ext_bug_priority', '0', 103, 1, NOW(), 'Bug优先级', 0
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict_data` WHERE `dict_type` = 'req_ext_bug_priority' AND `dict_value` = 'high' AND `tenant_id` = 0);

INSERT INTO `sys_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `status`, `create_dept`, `create_by`, `create_time`, `remark`, `tenant_id`)
SELECT 202604081204, 4, '紧急', 'urgent', 'req_ext_bug_priority', '0', 103, 1, NOW(), 'Bug优先级', 0
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict_data` WHERE `dict_type` = 'req_ext_bug_priority' AND `dict_value` = 'urgent' AND `tenant_id` = 0);

SET FOREIGN_KEY_CHECKS = 1;
