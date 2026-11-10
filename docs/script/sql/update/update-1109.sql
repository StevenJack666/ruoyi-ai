-- ----------------------------
-- SOAR 剧本运行参数迁入 sys_config（参数管理）
-- 说明：
--   1. system-user-id / max-retry 由 ConfigService 读取，带 Redis 缓存，
--      在「参数管理」页面修改后立即生效；
--      直接改库需要 HDEL sys_config <key>（多租户再 HDEL 000000:sys_config <key>）或重启应用。
--   2. exec-core-size / exec-max-size / exec-queue-size 在应用启动时读取一次并创建线程池，
--      改库后必须重启应用才生效。
--   3. 查不到或值非法时应用内回落默认值，删行不会导致启动失败。
-- ----------------------------

INSERT INTO `sys_config`
(`config_id`, `tenant_id`, `config_name`, `config_key`, `config_value`,
 `config_type`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES
    (2119100000000000001, '000000', '剧本-系统办理人ID',
     'soar.script.system-user-id', '1', 'N',
     103, 1, now(), 1, now(),
     '流程发起与自动推进使用的账号ID，需与流程节点 permissionFlag 配置一致；默认 1'),
    (2119100000000000002, '000000', '剧本-派发失败最大重试次数',
     'soar.script.max-retry', '3', 'N',
     103, 1, now(), 1, now(),
     '脚本派发异常时的重试上限，超过后终止流程；默认 3'),
    (2119100000000000003, '000000', '剧本-执行线程池核心线程数',
     'soar.script.exec-core-size', '4', 'N',
     103, 1, now(), 1, now(),
     '本地执行线程池常驻线程数，启动时读取；修改后需重启应用'),
    (2119100000000000004, '000000', '剧本-执行线程池最大线程数',
     'soar.script.exec-max-size', '16', 'N',
     103, 1, now(), 1, now(),
     '本地执行线程池并发上限，启动时读取；修改后需重启应用'),
    (2119100000000000005, '000000', '剧本-执行队列容量',
     'soar.script.exec-queue-size', '200', 'N',
     103, 1, now(), 1, now(),
     '线程池满后的排队上限，超过即回退待派发；启动时读取，修改后需重启应用');
