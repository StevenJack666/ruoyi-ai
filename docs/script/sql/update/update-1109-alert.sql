-- ----------------------------
-- SOAR 告警接入：规则表 + 事件表
-- 告警推送 -> /openapi/workflow/alert -> 规则匹配 -> 触发对应流程
-- ----------------------------

DROP TABLE IF EXISTS `t_alert_rule`;
CREATE TABLE `t_alert_rule`
(
    `id`                bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `rule_name`         varchar(100) NOT NULL COMMENT '规则名称',
    `source_pattern`    varchar(200)          DEFAULT NULL COMMENT '告警来源，逗号分隔支持*通配，空=不限',
    `type_pattern`      varchar(200)          DEFAULT NULL COMMENT '告警类型，逗号分隔支持*通配，空=不限',
    `severity_min`      varchar(20)           DEFAULT NULL COMMENT '最低等级 info/low/medium/high/critical，空=不限',
    `condition_spel`    varchar(1000)         DEFAULT NULL COMMENT '附加SpEL条件，只读上下文执行，空=不校验',
    `flow_code`         varchar(40)  NOT NULL COMMENT '命中后触发的流程编码',
    `dedup_window_sec`  int          NOT NULL DEFAULT 300 COMMENT '去重窗口(秒)，0=不去重',
    `priority`          int          NOT NULL DEFAULT 100 COMMENT '匹配优先级，值越小越先匹配',
    `enabled`           tinyint(1)   NOT NULL DEFAULT 1 COMMENT '是否启用',
    `remark`            varchar(500)          DEFAULT NULL COMMENT '备注',
    `create_time`       datetime              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`       datetime              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_enabled_priority` (`enabled`, `priority`)
) ENGINE = InnoDB COMMENT = '告警触发规则';

DROP TABLE IF EXISTS `t_alert_event`;
CREATE TABLE `t_alert_event`
(
    `id`            bigint      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `alert_id`      varchar(64) NOT NULL COMMENT '告警唯一标识',
    `dedup_key`     varchar(200)         DEFAULT NULL COMMENT '去重键：规则id+来源+类型+资产',
    `dedup_bucket`  bigint               DEFAULT NULL COMMENT '去重时间桶，null 表示不去重',
    `source`        varchar(64)          DEFAULT NULL COMMENT '告警来源',
    `type`          varchar(100)         DEFAULT NULL COMMENT '告警类型',
    `severity`      varchar(20)          DEFAULT NULL COMMENT '告警等级',
    `asset`         varchar(255)         DEFAULT NULL COMMENT '受影响资产',
    `raw`           json                 DEFAULT NULL COMMENT '告警原始请求',
    `decision`      tinyint     NOT NULL DEFAULT 0 COMMENT '0忽略 1建单',
    `rule_id`       bigint               DEFAULT NULL COMMENT '命中的规则id',
    `flow_code`     varchar(40)          DEFAULT NULL COMMENT '触发的流程编码',
    `business_id`   varchar(64)          DEFAULT NULL COMMENT '流程业务id',
    `instance_id`   bigint               DEFAULT NULL COMMENT '流程实例id',
    `result_msg`    varchar(500)         DEFAULT NULL COMMENT '处理说明',
    `create_time`   datetime             DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_dedup` (`dedup_key`, `dedup_bucket`),
    KEY `idx_alert` (`alert_id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE = InnoDB COMMENT = '告警接入事件';

-- ----------------------------
-- 示例规则
--   1. 高危及以上告警统一走 1109_n，5 分钟去重窗口（默认启用，按实际流程调整）
--   2. SpEL 条件示例：中危且数量超过 100 才建单（默认停用）
-- ----------------------------
INSERT INTO `t_alert_rule`
(`rule_name`, `source_pattern`, `type_pattern`, `severity_min`, `condition_spel`,
 `flow_code`, `dedup_window_sec`, `priority`, `enabled`, `remark`)
VALUES
    ('高危告警-默认处置', 'siem', '*', 'high', NULL,
     '1109_n', 300, 100, 1, '来源 siem 且等级 >= high 的告警，5 分钟内同类只开一次单'),
    ('中危高频告警-按数量', 'siem', '*', 'medium', 'count > 100',
     '1109_n', 600, 200, 0, '示例：中危且 attributes.count > 100 才建单，确认后手工启用');
