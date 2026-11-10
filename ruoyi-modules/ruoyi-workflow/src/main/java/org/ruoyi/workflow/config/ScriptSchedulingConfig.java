package org.ruoyi.workflow.config;

import org.ruoyi.workflow.common.ConditionalOnEnable;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 剧本派发调度配置
 * <p>
 * 项目默认只在 snail-job 开启时才启用调度（见 SnailJobConfig），
 * 这里显式开启，供剧本派发的轮询任务使用。
 *
 * @author ruoyi
 */
@ConditionalOnEnable
@Configuration
@EnableScheduling
public class ScriptSchedulingConfig {
}
