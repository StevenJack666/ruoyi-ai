CREATE TABLE IF NOT EXISTS `sse_message_history` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `message_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '消息ID',
  `message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '消息内容',
  `send_time` bigint NOT NULL COMMENT '发送时间戳(毫秒)',
  `read_status` tinyint NOT NULL DEFAULT 0 COMMENT '读取状态(0未读 1已读)',
  `read_time` datetime NULL DEFAULT NULL COMMENT '读取时间',
  `create_dept` bigint NULL DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户Id',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_tenant_user_message` (`tenant_id`, `user_id`, `message_id`) USING BTREE,
  KEY `idx_tenant_user_read_send` (`tenant_id`, `user_id`, `read_status`, `send_time`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'SSE消息历史' ROW_FORMAT = DYNAMIC;
