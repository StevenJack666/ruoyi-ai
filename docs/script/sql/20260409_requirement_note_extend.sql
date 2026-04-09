-- 项目记事（简化版：纯文本 + 附件）
CREATE TABLE IF NOT EXISTS `req_ext_note` (
  `id` bigint(20) NOT NULL COMMENT '主键',
  `title` varchar(255) NOT NULL COMMENT '标题',
  `content` longtext DEFAULT NULL COMMENT '正文内容(纯文本)',
  `attachment_count` int(11) NOT NULL DEFAULT 0 COMMENT '附件数量',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志(0正常 2删除)',
  `create_dept` bigint(20) DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `tenant_id` varchar(20) DEFAULT '000000' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_req_note_title` (`title`),
  KEY `idx_req_note_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目记事';

CREATE TABLE IF NOT EXISTS `req_ext_note_attachment` (
  `id` bigint(20) NOT NULL COMMENT '主键',
  `note_id` bigint(20) NOT NULL COMMENT '记事ID',
  `file_url` varchar(500) NOT NULL COMMENT '文件地址',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志(0正常 2删除)',
  `create_dept` bigint(20) DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `tenant_id` varchar(20) DEFAULT '000000' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_req_note_attachment_note` (`note_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目记事附件';
