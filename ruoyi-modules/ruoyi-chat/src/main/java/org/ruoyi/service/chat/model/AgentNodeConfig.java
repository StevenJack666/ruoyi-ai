package org.ruoyi.service.chat.model;

import java.util.List;

import lombok.Builder;
import lombok.Data;

/**
 * 单个 Agent 节点运行时配置
 */
@Data
@Builder
public class AgentNodeConfig {

    private String name;

    private String model;

    private String systemPrompt;

    /**
     * 绑定的技能编码或名称列表
     */
    private List<String> skills;

    private List<String> tools;

    /**
     * 并行模式下写入状态的 key，等价于 AgenticServices 的 outputKey
     */
    private String outputKey;
}
