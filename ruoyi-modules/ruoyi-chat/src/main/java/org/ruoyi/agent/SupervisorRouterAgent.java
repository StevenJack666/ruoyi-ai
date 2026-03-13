package org.ruoyi.agent;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 用于在多 Agent 配置中选择目标 Agent
 */
public interface SupervisorRouterAgent extends Agent {

    @SystemMessage("{{systemPrompt}}")
    @UserMessage("""
        用户问题：
        {{query}}

        可选 agent 列表（仅可选择一个）：
        {{candidates}}

        只返回一个 agent 名称，不要返回其他内容。
        """)
    @dev.langchain4j.agentic.Agent("多Agent调度器")
    String route(@V("systemPrompt") String systemPrompt,
                 @V("query") String query,
                 @V("candidates") String candidates);
}
