package org.ruoyi.service.chat.agent.impl;

import static org.springframework.core.Ordered.LOWEST_PRECEDENCE;

import java.util.ArrayList;
import java.util.List;

import org.ruoyi.agent.DynamicMcpAgent;
import org.ruoyi.common.chat.domain.dto.request.ChatRequest;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.mcp.service.core.ToolProviderFactory;
import org.ruoyi.service.chat.agent.AgentExecutor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.service.tool.ToolProvider;
import lombok.extern.slf4j.Slf4j;

/**
 * 默认固定 Agent 执行器
 */
@Slf4j
@Component
@Order(LOWEST_PRECEDENCE)
public class DefaultAgentExecutor implements AgentExecutor {

    private final ToolProviderFactory toolProviderFactory;

    public DefaultAgentExecutor(ToolProviderFactory toolProviderFactory) {
        this.toolProviderFactory = toolProviderFactory;
    }

    @Override
    public boolean supports(ChatRequest request) {
        return true;
    }

    @Override
    public String execute(String userMessage, ChatModelVo chatModelVo, ChatRequest request) {
        try {
            QwenChatModel qwenChatModel = QwenChatModel.builder()
                .apiKey(chatModelVo.getApiKey())
                .modelName(chatModelVo.getModelName())
                .build();

            List<Object> builtinTools = toolProviderFactory.getAllBuiltinToolObjects();
            List<Object> allTools = new ArrayList<>(builtinTools);
            ToolProvider mcpToolProvider = toolProviderFactory.getAllEnabledMcpToolsProvider();

            var agentBuilder = AgenticServices.agentBuilder(DynamicMcpAgent.class).chatModel(qwenChatModel);
            if (!allTools.isEmpty()) {
                agentBuilder.tools(allTools.toArray(new Object[0]));
            }
            if (mcpToolProvider != null) {
                agentBuilder.toolProvider(mcpToolProvider);
            }

            DynamicMcpAgent agent = agentBuilder.build();
            return agent.callMcpTool(defaultSystemPrompt(), userMessage);
        } catch (Exception e) {
            log.error("Default agent execution failed: {}", e.getMessage(), e);
            return null;
        }
    }

    private String defaultSystemPrompt() {
        return """
            你是一个AI助手，可以通过调用各种工具来帮助用户完成不同的任务。

            工具使用规则：
            1. 根据用户请求选择最合适的工具。
            2. 调用工具前先确认参数完整且正确。
            3. 工具执行后基于结果进行归纳，再输出清晰结论。
            4. 若工具失败，说明失败原因并给出下一步建议。
            """;
    }
}
