INSERT INTO `ruoyi-ai-agent`.`sys_config` (`config_id`, `tenant_id`, `config_name`, `config_key`, `config_value`, `config_type`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (2074417562781937666, '000000', '情报分类会话ID', 'intelClassify.sessionId', '100', 'N', 103, 1, '2026-07-07 16:57:42', 1, '2026-07-07 16:57:42', NULL);
INSERT INTO `ruoyi-ai-agent`.`sys_config` (`config_id`, `tenant_id`, `config_name`, `config_key`, `config_value`, `config_type`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (2074417653013999617, '000000', '风险研判会话ID', 'riskJudge.sessionId', '101', 'N', 103, 1, '2026-07-07 16:58:04', 1, '2026-07-07 16:58:04', NULL);
INSERT INTO `ruoyi-ai-agent`.`sys_config` (`config_id`, `tenant_id`, `config_name`, `config_key`, `config_value`, `config_type`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (2074421611300319233, '000000', 'openapi默认模型名称', 'openapi.default.model', 'qwen-plus', 'N', 103, 1, '2026-07-07 17:13:48', 1, '2026-07-07 17:13:48', NULL);
INSERT INTO `ruoyi-ai-agent`.`sys_user` (`user_id`, `tenant_id`, `dept_id`, `user_name`, `nick_name`, `user_type`, `email`, `phonenumber`, `sex`, `avatar`, `password`, `status`, `del_flag`, `login_ip`, `login_date`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `open_id`, `user_balance`) VALUES (5, '000000', 103, 'openapi', 'openApiUser（固化角色）', 'sys_user', '', '', '0', NULL, '', '0', '0', '', NULL, 103, NULL, '2026-07-07 16:36:25', NULL, NULL, 'openApi固化角色', NULL, 0.00);

UPDATE `ruoyi-ai-agent`.`sys_config` SET `tenant_id` = '000000', `config_name` = '情报分类会话配置', `config_key` = 'intelClassify.session.config', `config_value` = '{\n    \"sessionId\":101,\n    \"sessionContent\":\"情报分类\",\n    \"sessionTitle\":\"情报分类\"\n}', `config_type` = 'N', `create_dept` = 103, `create_by` = 1, `create_time` = '2026-07-07 16:57:42', `update_by` = 1, `update_time` = '2026-07-08 14:48:20', `remark` = NULL WHERE `config_id` = 2074417562781937666;
UPDATE `ruoyi-ai-agent`.`sys_config` SET `tenant_id` = '000000', `config_name` = '风险研判会话配置', `config_key` = 'riskJudge.session.config', `config_value` = '{\n    \"sessionId\":100,\n    \"sessionContent\":\"风险研判\",\n    \"sessionTitle\":\"风险研判\"\n}', `config_type` = 'N', `create_dept` = 103, `create_by` = 1, `create_time` = '2026-07-07 16:58:04', `update_by` = 1, `update_time` = '2026-07-08 14:47:59', `remark` = NULL WHERE `config_id` = 2074417653013999617;


DROP TABLE IF EXISTS `session_message_file_rel`;
CREATE TABLE `session_message_file_rel`  (
`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
`message_id` bigint(20) NULL DEFAULT NULL COMMENT '对话消息ID',
`session_id` bigint(20) NULL DEFAULT NULL COMMENT '消息ID',
`oss_file_id` bigint(20) NULL DEFAULT NULL COMMENT '文件ID',
`create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
`update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
`create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建者',
`update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '更新者',
`remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
`is_deleted` tinyint(1) NULL DEFAULT 0 COMMENT '是否删除（0：未删除，1：已删除）',
PRIMARY KEY (`id`) USING BTREE,
INDEX `idx_message_id`(`message_id` ASC) USING BTREE,
INDEX `idx_oss_file_id`(`oss_file_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2070449697850568707 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '会话消息文件关联表' ROW_FORMAT = Dynamic;

DROP TABLE IF EXISTS `resource_info`;
CREATE TABLE `resource_info`  (
`id` bigint(20) NOT NULL AUTO_INCREMENT,
`resource_uri` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
`name` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
`status` smallint(6) NULL DEFAULT NULL COMMENT '是否拦截：0（拦截），1（放行）',
PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

DROP TABLE IF EXISTS `user_authen_info`;
CREATE TABLE `user_authen_info`  (
 `id` bigint(20) NOT NULL AUTO_INCREMENT,
 `app_id` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
 `security_key` varchar(500) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
 `security_type` varchar(2) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT 'app_id对应的验签方式(01-原方式，请求参数都参与,02-只针对appid进行参与签名)',
 `organization` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '所属机构',
 `un_security_key` varchar(500) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
 PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

DROP TABLE IF EXISTS `user_permission_ip`;
CREATE TABLE `user_permission_ip`  (
`id` bigint(20) NOT NULL AUTO_INCREMENT,
`app_id` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
`ip` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

DROP TABLE IF EXISTS `user_permission_resource`;
CREATE TABLE `user_permission_resource`  (
`id` bigint(20) NOT NULL AUTO_INCREMENT,
`app_id` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
`resource_uri` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

DROP TABLE IF EXISTS `rds_data_flow_config`;
CREATE TABLE `rds_data_flow_config`  (
 `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '流控配置ID',
 `app_id` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '应用ID',
 `resource_uri` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '请求RUI路径',
 `permits_per` decimal(20, 2) NULL DEFAULT NULL COMMENT '并发数(每秒)',
 `wait_timeout` bigint(20) NULL DEFAULT NULL COMMENT '等待时间(毫秒)',
 `enable_flag` varchar(2) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '是否启用',
 `create_by` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '创建者',
 `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
 `update_by` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '更新者',
 `update_time` datetime NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
 `is_deleted` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '是否已删除',
 `remark` varchar(256) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '备注',
 PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '暴露面流控配置表' ROW_FORMAT = Dynamic;
