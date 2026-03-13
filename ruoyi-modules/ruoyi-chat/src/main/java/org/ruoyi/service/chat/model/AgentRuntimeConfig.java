package org.ruoyi.service.chat.model;

import java.util.List;

import lombok.Builder;
import lombok.Data;

/**
 * 运行时 Agent 配置
 */
@Data
@Builder
public class AgentRuntimeConfig {

    private Long marketId;

    private String marketName;

    private String systemPrompt;

    private String supervisorPrompt;

    private AgentExecutionMode executionMode;

    private List<RuntimeAgentConfig> agents;

    private List<String> builtinToolNames;

    private List<String> mcpToolNames;
}
