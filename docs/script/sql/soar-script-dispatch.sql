-- ----------------------------
-- SOAR 剧本自动执行：脚本映射 与 派发 outbox
-- 依赖：warm-flow 的 flow_definition / flow_node / flow_task 表已存在
-- 说明：实体继承 TenantEntity，故包含 create_dept / create_by / create_time / update_by / update_time / tenant_id
-- ----------------------------

DROP TABLE IF EXISTS `t_playbook_script`;
CREATE TABLE `t_playbook_script`
(
    `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `flow_code`   varchar(40)  NOT NULL COMMENT '流程编码',
    `node_code`   varchar(100) NOT NULL COMMENT '节点编码',
    `script_path` varchar(500) NOT NULL COMMENT 'Python 脚本路径',
    `python_bin`  varchar(500) NOT NULL COMMENT '该脚本使用的 Python 解释器路径（如独立 venv），必填',
    `timeout_sec` int          NOT NULL DEFAULT 900 COMMENT '执行超时（秒）',
    `max_retry`   int          NOT NULL DEFAULT 3 COMMENT '派发失败最大重试次数',
    `enabled`     tinyint(1)   NOT NULL DEFAULT 1 COMMENT '是否启用（1启用 0停用）',
    `remark`      varchar(500)          DEFAULT NULL COMMENT '备注',
    `create_dept` bigint                DEFAULT NULL COMMENT '创建部门',
    `create_by`   bigint                DEFAULT NULL COMMENT '创建者',
    `create_time` datetime              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`   bigint                DEFAULT NULL COMMENT '更新者',
    `update_time` datetime              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `tenant_id`   varchar(40)           DEFAULT NULL COMMENT '租户编号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_flow_node` (`flow_code`, `node_code`)
) ENGINE = InnoDB COMMENT = '剧本节点脚本映射';

-- ----------------------------
-- 示例数据（按实际流程编码与节点编码调整）
-- ----------------------------
-- INSERT INTO `t_playbook_script` (`flow_code`, `node_code`, `script_path`, `timeout_sec`, `max_retry`, `enabled`, `remark`)
-- VALUES ('phishing_response', 'node_block_ip', '/opt/soar/playbooks/ip_block.py', 900, 3, 1, '封禁恶意 IP');

DROP TABLE IF EXISTS `t_script_outbox`;
CREATE TABLE `t_script_outbox`
(
    `id`            bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `run_id`        varchar(64)  NOT NULL COMMENT '本次脚本执行唯一标识，回调幂等键',
    `instance_id`   bigint       NOT NULL COMMENT '流程实例id',
    `task_id`       bigint       NOT NULL COMMENT '流程任务id',
    `business_id`   varchar(64)  NOT NULL COMMENT '业务id（告警id）',
    `flow_code`     varchar(40)  NOT NULL COMMENT '流程编码',
    `node_code`     varchar(100) NOT NULL COMMENT '节点编码',
    `script_path`   varchar(500) NOT NULL COMMENT '脚本绝对路径',
    `python_bin`    varchar(500)          DEFAULT NULL COMMENT '派发时的解释器快照',
    `script_sha256` varchar(64)           DEFAULT NULL COMMENT '派发时计算的脚本文件哈希，本机不可读时由执行器回调回报',
    `output`        json                  DEFAULT NULL COMMENT '脚本输出（stdout 解析后的 output）',
    `trace`         json                  DEFAULT NULL COMMENT '执行轨迹（trace 数组）',
    `payload`       json         NOT NULL COMMENT '派发入参',
    `timeout_sec`   int          NOT NULL DEFAULT 900 COMMENT '执行超时（秒），入队时快照',
    `status`        tinyint      NOT NULL DEFAULT 0 COMMENT '0待派发 1已派发 2已回调 3失败 4超时 5取消',
    `retry_count`   int          NOT NULL DEFAULT 0 COMMENT '已重试次数',
    `next_retry_at` datetime              DEFAULT NULL COMMENT '下次重试时间',
    `error_msg`     varchar(500)          DEFAULT NULL COMMENT '失败原因',
    `dispatched_at` datetime              DEFAULT NULL COMMENT '派发时间',
    `deadline_at`   datetime              DEFAULT NULL COMMENT '超时截止时间 = 派发时间 + 超时秒数',
    `finished_at`   datetime              DEFAULT NULL COMMENT '完成时间',
    `create_dept`   bigint                DEFAULT NULL COMMENT '创建部门',
    `create_by`     bigint                DEFAULT NULL COMMENT '创建者',
    `create_time`   datetime              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`     bigint                DEFAULT NULL COMMENT '更新者',
    `update_time`   datetime              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `tenant_id`     varchar(40)           DEFAULT NULL COMMENT '租户编号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_run_id` (`run_id`),
    KEY `idx_dispatch` (`status`, `next_retry_at`),
    KEY `idx_deadline` (`status`, `deadline_at`),
    KEY `idx_business` (`business_id`),
    KEY `idx_instance` (`instance_id`)
) ENGINE = InnoDB COMMENT = '脚本派发 outbox';
