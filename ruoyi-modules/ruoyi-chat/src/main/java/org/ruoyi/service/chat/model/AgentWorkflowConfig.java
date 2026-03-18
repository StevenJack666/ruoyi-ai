package org.ruoyi.service.chat.model;

import java.util.List;

import lombok.Builder;
import lombok.Data;

/**
 * Agent 工作流运行时配置
 */
@Data
@Builder
public class AgentWorkflowConfig {

    private Long marketId;

    private String marketName;

    private AgentExecutionMode executionMode;

    private List<AgentNodeConfig> agents;

    /**
     * 并行模式聚合输出 key（类似 parallelBuilder.outputKey）
     */
    private String parallelOutputKey;

    /**
     * 并行模式聚合方式：merge(默认) / zip
     */
    private String parallelAggregate;

    /**
     * 并行模式聚合读取的状态 key 列表
     */
    private List<String> parallelReadKeys;

    /**
     * 条件分支
     */
    private ConditionalConfig conditional;
}
