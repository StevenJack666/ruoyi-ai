package org.ruoyi.agent;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 支持运行时系统提示词注入的通用 MCP Agent
 */
public interface DynamicMcpAgent extends Agent {

    @SystemMessage("{{systemPrompt}}")
    @UserMessage("{{query}}")
    @dev.langchain4j.agentic.Agent("动态工具调用智能体")
    String callMcpTool(@V("systemPrompt") String systemPrompt, @V("query") String query);
}
