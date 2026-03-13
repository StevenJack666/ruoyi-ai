package org.ruoyi.service.chat.agent.impl;

import static org.springframework.core.Ordered.HIGHEST_PRECEDENCE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.ruoyi.agent.DynamicMcpAgent;
import org.ruoyi.agent.SupervisorRouterAgent;
import org.ruoyi.common.chat.domain.dto.request.ChatRequest;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.mcp.service.core.ToolProviderFactory;
import org.ruoyi.service.chat.AgentMarketAssemblyService;
import org.ruoyi.service.chat.agent.AgentExecutor;
import org.ruoyi.service.chat.model.AgentExecutionMode;
import org.ruoyi.service.chat.model.AgentRuntimeConfig;
import org.ruoyi.service.chat.model.RuntimeAgentConfig;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.service.tool.ToolProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 基于 market 模板的动态 Agent 执行器
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(HIGHEST_PRECEDENCE + 100)
public class MarketConfiguredAgentExecutor implements AgentExecutor {

    private final AgentMarketAssemblyService assemblyService;
    private final ToolProviderFactory toolProviderFactory;

    @Override
    public boolean supports(ChatRequest request) {
        return request != null && request.getAgentMarketId() != null;
    }

    @Override
    public String execute(String userMessage, ChatModelVo chatModelVo, ChatRequest request) {
        AgentRuntimeConfig runtimeConfig = assemblyService.assemble(request.getAgentMarketId());
        if (runtimeConfig == null) {
            log.warn("Market config not found or inactive, fallback to default executor. marketId={}", request.getAgentMarketId());
            return null;
        }

        List<RuntimeAgentConfig> agents = runtimeConfig.getAgents();
        if (CollectionUtils.isEmpty(agents)) {
            log.warn("No runtime agents assembled for market {}, fallback executor will be used", request.getAgentMarketId());
            return null;
        }

        AgentExecutionMode mode = runtimeConfig.getExecutionMode() == null
            ? AgentExecutionMode.SINGLE
            : runtimeConfig.getExecutionMode();

        RuntimeAgentConfig primary = findPrimaryAgent(agents);
        List<RuntimeAgentConfig> children = findChildAgents(agents, primary);

        return switch (mode) {
            case SINGLE -> executeSingle(userMessage, chatModelVo, primary != null ? primary : agents.get(0));
            case SUPERVISOR -> executeSupervisor(userMessage, chatModelVo, runtimeConfig, primary, children, agents);
            case PARALLEL -> executeParallel(userMessage, chatModelVo,
                CollectionUtils.isEmpty(children) ? agents : children);
        };
    }

    private String executeSingle(String userMessage, ChatModelVo chatModelVo, RuntimeAgentConfig agentConfig) {
        return executeOneAgent(userMessage, chatModelVo, agentConfig);
    }

    private String executeSupervisor(String userMessage, ChatModelVo chatModelVo,
                                     AgentRuntimeConfig runtimeConfig,
                                     RuntimeAgentConfig primary,
                                     List<RuntimeAgentConfig> children,
                                     List<RuntimeAgentConfig> fallbackAgents) {
        List<RuntimeAgentConfig> candidates = CollectionUtils.isEmpty(children) ? fallbackAgents : children;
        RuntimeAgentConfig selected = chooseBySupervisor(userMessage, chatModelVo, runtimeConfig, primary, candidates);
        if (selected == null) {
            selected = candidates.get(0);
        }
        return executeOneAgent(userMessage, chatModelVo, selected);
    }

    private String executeParallel(String userMessage, ChatModelVo chatModelVo,
                                   List<RuntimeAgentConfig> agents) {
        List<CompletableFuture<String>> futures = agents.stream()
            .map(agent -> CompletableFuture.supplyAsync(() -> executeOneAgent(userMessage, chatModelVo, agent)))
            .toList();

        List<String> outputs = futures.stream().map(CompletableFuture::join).toList();

        StringBuilder merged = new StringBuilder();
        merged.append("并行执行结果汇总：\n");
        for (int i = 0; i < agents.size(); i++) {
            String name = agents.get(i).getName();
            String output = outputs.get(i);
            merged.append("\n[").append(name).append("]\n")
                .append(StringUtils.isNotBlank(output) ? output : "(无输出)")
                .append("\n");
        }

        RuntimeAgentConfig summarizer = RuntimeAgentConfig.builder()
            .name("parallel-summarizer")
            .model(null)
            .tools(Collections.emptyList())
            .systemPrompt("你是并行结果汇总助手，请整合多个agent结果，去重冲突并给出清晰最终答案。")
            .build();

        String summaryInput = "用户问题：\n" + userMessage + "\n\n" + merged;
        String summary = executeOneAgent(summaryInput, chatModelVo, summarizer);
        return StringUtils.isNotBlank(summary) ? summary : merged.toString();
    }

    private RuntimeAgentConfig chooseBySupervisor(String userMessage, ChatModelVo chatModelVo,
                                                  AgentRuntimeConfig runtimeConfig,
                                                  RuntimeAgentConfig primary,
                                                  List<RuntimeAgentConfig> agents) {
        Map<String, RuntimeAgentConfig> agentMap = agents.stream()
            .filter(a -> StringUtils.isNotBlank(a.getName()))
            .collect(Collectors.toMap(a -> a.getName().toLowerCase(Locale.ROOT), Function.identity(), (a, b) -> a));

        if (agentMap.isEmpty()) {
            return agents.get(0);
        }

        List<String> candidates = new ArrayList<>(agentMap.keySet());
        String picked = routeWithSupervisor(userMessage, chatModelVo,
            runtimeConfig.getSupervisorPrompt(), primary, candidates);
        if (StringUtils.isBlank(picked)) {
            return agents.get(0);
        }

        RuntimeAgentConfig exact = agentMap.get(picked.toLowerCase(Locale.ROOT));
        if (exact != null) {
            return exact;
        }

        String pickedLower = picked.toLowerCase(Locale.ROOT);
        for (RuntimeAgentConfig candidate : agents) {
            if (StringUtils.isNotBlank(candidate.getName())
                && pickedLower.contains(candidate.getName().toLowerCase(Locale.ROOT))) {
                return candidate;
            }
        }
        return agents.get(0);
    }

    private String routeWithSupervisor(String userMessage, ChatModelVo chatModelVo,
                                       String supervisorPrompt,
                                       RuntimeAgentConfig primary,
                                       List<String> candidates) {
        try {
            String primaryModel = primary != null ? primary.getModel() : null;
            QwenChatModel model = QwenChatModel.builder()
                .apiKey(chatModelVo.getApiKey())
                .modelName(StringUtils.isNotBlank(primaryModel)
                    ? primaryModel
                    : chatModelVo.getModelName())
                .build();
            SupervisorRouterAgent router = AgenticServices.agentBuilder(SupervisorRouterAgent.class)
                .chatModel(model)
                .build();

            String primaryPrompt = primary != null ? primary.getSystemPrompt() : null;
            String prompt = StringUtils.isNotBlank(supervisorPrompt)
                ? supervisorPrompt
                : (StringUtils.isNotBlank(primaryPrompt)
                ? primaryPrompt
                : "你是任务调度器，请在候选 agent 中选择最合适的一个执行");
            return router.route(prompt, userMessage, String.join(",", candidates));
        } catch (Exception e) {
            log.warn("Supervisor route failed: {}", e.getMessage());
            return null;
        }
    }

    private RuntimeAgentConfig findPrimaryAgent(List<RuntimeAgentConfig> agents) {
        if (CollectionUtils.isEmpty(agents)) {
            return null;
        }
        return agents.stream()
            .filter(a -> StringUtils.isNotBlank(a.getRole()) && "primary".equalsIgnoreCase(a.getRole()))
            .findFirst()
            .orElse(null);
    }

    private List<RuntimeAgentConfig> findChildAgents(List<RuntimeAgentConfig> agents, RuntimeAgentConfig primary) {
        if (CollectionUtils.isEmpty(agents)) {
            return List.of();
        }

        List<RuntimeAgentConfig> children = agents.stream()
            .filter(a -> StringUtils.isNotBlank(a.getRole()) && "child".equalsIgnoreCase(a.getRole()))
            .toList();
        if (!CollectionUtils.isEmpty(children)) {
            return children;
        }

        if (primary == null) {
            return agents;
        }

        return agents.stream()
            .filter(a -> a != primary)
            .toList();
    }

    private String executeOneAgent(String userMessage, ChatModelVo chatModelVo, RuntimeAgentConfig agentConfig) {
        if (agentConfig == null) {
            return null;
        }

        try {
            QwenChatModel qwenChatModel = QwenChatModel.builder()
                .apiKey(chatModelVo.getApiKey())
                .modelName(StringUtils.isNotBlank(agentConfig.getModel()) ? agentConfig.getModel() : chatModelVo.getModelName())
                .build();

            List<String> toolNames = agentConfig.getTools() == null ? Collections.emptyList() : agentConfig.getTools();
            List<Object> builtinTools = toolProviderFactory.getBuiltinToolObjectsByNames(toolNames);
            List<Object> allTools = new ArrayList<>(builtinTools);
            ToolProvider mcpToolProvider = toolProviderFactory.getMcpToolsProviderByNames(toolNames);

            var agentBuilder = AgenticServices.agentBuilder(DynamicMcpAgent.class).chatModel(qwenChatModel);
            if (!allTools.isEmpty()) {
                agentBuilder.tools(allTools.toArray(new Object[0]));
            }
            if (mcpToolProvider != null) {
                agentBuilder.toolProvider(mcpToolProvider);
            }

            DynamicMcpAgent agent = agentBuilder.build();
            String prompt = StringUtils.isNotBlank(agentConfig.getSystemPrompt())
                ? agentConfig.getSystemPrompt()
                : "你是智能助手，请完成用户任务。";
            return agent.callMcpTool(prompt, userMessage);
        } catch (Exception e) {
            log.error("Market configured sub-agent execution failed: {}", e.getMessage(), e);
            return null;
        }
    }
}
