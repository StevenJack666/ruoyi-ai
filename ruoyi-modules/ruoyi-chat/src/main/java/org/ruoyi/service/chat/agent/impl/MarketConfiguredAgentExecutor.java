package org.ruoyi.service.chat.agent.impl;

import static org.springframework.core.Ordered.HIGHEST_PRECEDENCE;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.ruoyi.agent.DynamicMcpAgent;
import org.ruoyi.common.chat.domain.dto.request.ChatRequest;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.mcp.service.core.ToolProviderFactory;
import org.ruoyi.service.chat.AgentMarketAssemblyService;
import org.ruoyi.service.chat.agent.AgentExecutor;
import org.ruoyi.service.chat.model.AgentExecutionMode;
import org.ruoyi.service.chat.model.AgentNodeConfig;
import org.ruoyi.service.chat.model.AgentWorkflowConfig;
import org.ruoyi.service.chat.model.ConditionalConfig;
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
        AgentWorkflowConfig runtimeConfig = assemblyService.assemble(request.getAgentMarketId());
        if (runtimeConfig == null) {
            log.warn("Market config not found or inactive, fallback to default executor. marketId={}", request.getAgentMarketId());
            return null;
        }

        List<AgentNodeConfig> agents = runtimeConfig.getAgents();
        if (CollectionUtils.isEmpty(agents)) {
            log.warn("No runtime agents assembled for market {}, fallback executor will be used", request.getAgentMarketId());
            return null;
        }

        AgentExecutionMode mode = runtimeConfig.getExecutionMode() == null
            ? AgentExecutionMode.SINGLE
            : runtimeConfig.getExecutionMode();

        return switch (mode) {
            case SINGLE -> executeOneAgent(userMessage, chatModelVo, agents.get(0));
            case SEQUENTIAL -> executeSequential(userMessage, chatModelVo, agents);
            case PARALLEL -> executeParallel(userMessage, chatModelVo, runtimeConfig, agents);
            case CONDITIONAL -> executeConditional(userMessage, chatModelVo, runtimeConfig, agents);
        };
    }

    private String executeSequential(String userMessage, ChatModelVo chatModelVo,
                                     List<AgentNodeConfig> agents) {
        if (CollectionUtils.isEmpty(agents)) {
            return null;
        }

        String currentInput = userMessage;
        String finalOutput = null;

        for (AgentNodeConfig agent : agents) {
            String output = executeOneAgent(currentInput, chatModelVo, agent);
            finalOutput = output;

            if (StringUtils.isNotBlank(output)) {
                currentInput = "用户原始问题：\n" + userMessage + "\n\n上一个agent输出：\n" + output;
            }
        }

        return finalOutput;
    }

    private String executeParallel(String userMessage, ChatModelVo chatModelVo,
                                   AgentWorkflowConfig runtimeConfig,
                                   List<AgentNodeConfig> agents) {
        if (CollectionUtils.isEmpty(agents)) {
            return null;
        }
        int poolSize = Math.max(1, Math.min(agents.size(), 8));
        ExecutorService executor = Executors.newFixedThreadPool(poolSize);
        try {
            List<CompletableFuture<String>> futures = agents.stream()
                .map(agent -> CompletableFuture.supplyAsync(
                    () -> executeOneAgent(userMessage, chatModelVo, agent), executor))
                .toList();

            List<String> outputs = futures.stream().map(CompletableFuture::join).toList();
            LinkedHashMap<String, String> state = new LinkedHashMap<>();
            for (int i = 0; i < agents.size(); i++) {
                AgentNodeConfig agent = agents.get(i);
                String key = StringUtils.isNotBlank(agent.getOutputKey())
                    ? agent.getOutputKey()
                    : (StringUtils.isNotBlank(agent.getName()) ? agent.getName() : ("agent_" + (i + 1)));
                state.put(key, outputs.get(i));
            }
            return aggregateParallelState(state, userMessage, runtimeConfig);
        } finally {
            executor.shutdown();
        }
    }

    private String aggregateParallelState(LinkedHashMap<String, String> state, String userMessage,
                                          AgentWorkflowConfig runtimeConfig) {
        String aggregateType = runtimeConfig == null ? null : runtimeConfig.getParallelAggregate();
        List<String> readKeys = runtimeConfig == null ? List.of() : runtimeConfig.getParallelReadKeys();
        String outputKey = runtimeConfig == null ? null : runtimeConfig.getParallelOutputKey();

        if ("zip".equalsIgnoreCase(aggregateType) && readKeys != null && readKeys.size() >= 2) {
            String zipped = zipParallelState(state, readKeys, outputKey);
            if (StringUtils.isNotBlank(zipped)) {
                return zipped;
            }
        }

        StringBuilder merged = new StringBuilder();
        merged.append("并行执行结果汇总：\n");
        merged.append("用户问题：\n").append(userMessage).append("\n");

        state.forEach((key, value) -> {
            merged.append("\n[").append(key).append("]\n")
                .append(StringUtils.isNotBlank(value) ? value : "(无输出)")
                .append("\n");
        });
        return merged.toString();
    }

    private String zipParallelState(LinkedHashMap<String, String> state, List<String> readKeys, String outputKey) {
        List<List<String>> columns = new ArrayList<>();
        for (String key : readKeys) {
            if (StringUtils.isBlank(key)) {
                columns.add(List.of());
                continue;
            }
            String raw = state.getOrDefault(key, "");
            columns.add(parseOutputItems(raw));
        }

        int min = columns.stream().mapToInt(List::size).min().orElse(0);
        if (min <= 0) {
            return "[]";
        }

        StringBuilder json = new StringBuilder();
        json.append("{\"").append(StringUtils.isNotBlank(outputKey) ? outputKey : "plans").append("\":[");
        for (int i = 0; i < min; i++) {
            if (i > 0) {
                json.append(",");
            }
            json.append("{");
            for (int c = 0; c < readKeys.size(); c++) {
                if (c > 0) {
                    json.append(",");
                }
                String key = readKeys.get(c);
                String value = columns.get(c).get(i).replace("\\", "\\\\").replace("\"", "\\\"");
                json.append("\"").append(key).append("\":\"").append(value).append("\"");
            }
            json.append("}");
        }
        json.append("]}");
        return json.toString();
    }

    private List<String> parseOutputItems(String raw) {
        if (StringUtils.isBlank(raw)) {
            return List.of();
        }
        String content = raw.trim();
        String[] lines = content.split("\\r?\\n");
        return Arrays.stream(lines)
            .map(String::trim)
            .filter(StringUtils::isNotBlank)
            .map(line -> line.replaceFirst("^[\\-*•\\d\\.)\\s]+", "").trim())
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.toList());
    }

    private String executeOneAgent(String userMessage, ChatModelVo chatModelVo, AgentNodeConfig agentConfig) {
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
            String prompt = agentConfig.getSystemPrompt();
            return agent.callMcpTool(prompt, userMessage);
        } catch (Exception e) {
            log.error("Market configured sub-agent execution failed: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 执行条件工作流
     * 根据 Planner 的决策动态选择执行路径
     */
    private String executeConditional(String userMessage, ChatModelVo chatModelVo, AgentWorkflowConfig config,
                                      List<AgentNodeConfig> agents) {
        ConditionalConfig conditionalConfig = config.getConditional();
        if (conditionalConfig == null || conditionalConfig.getBranches() == null || conditionalConfig.getBranches().isEmpty()) {
            log.warn("条件工作流配置缺失或分支定义为空");
            return "工作流配置错误：缺少条件分支配置。";
        }

        try {
            // Step 1: 执行 Planner 获取决策
            String plannerName = conditionalConfig.getPlanner();
            AgentNodeConfig plannerAgent = findAgentByName(agents, plannerName);

            if (plannerAgent == null) {
                log.error("未找到规划器Agent：{}，请检查配置", plannerName);
                return "系统配置错误，未能找到规划组件。";
            }

            log.info("正在执行规划器Agent：{}", plannerName);
            String rawDecision = executeOneAgent(userMessage, chatModelVo, plannerAgent);

            if (rawDecision == null) {
                log.warn("规划器Agent返回了空决策");
                return "规划器未能生成有效方案，请重试。";
            }

            // Step 2: 清洗并解析决策结果(增强清洗：去除引号、括号、换行符及常见中文标点)
            String decision = rawDecision.trim()
                .replaceAll("[\"`'\\[\\]()\\n\\r\\s，。；：]", "")
                .toUpperCase();

            log.info("收到原始决策内容: [{}], 清洗后决策关键字: [{}]", rawDecision, decision);

            // Step 3: 根据决策选择分支
            List<String> branchAgentNames = selectBranch(conditionalConfig.getBranches(), decision);

            if (branchAgentNames == null || branchAgentNames.isEmpty()) {
                log.warn("未能匹配到任何有效分支，决策内容: {}", decision);
                return "未能理解您的需求意图，请明确您的选择。";
            }

            // Step 4: 解析分支中的 Agent 配置
            List<AgentNodeConfig> branchAgents = resolveAgents(agents, branchAgentNames);
            if (branchAgents.isEmpty()) {
                log.error("分支中未找到有效的Agent配置，分支Agent名称列表: {}", branchAgentNames);
                return "系统错误：未能加载分支执行组件。";
            }

            log.info("已匹配分支 [{}]，即将执行 Agents: {}", decision, branchAgentNames);

            // Step 5: 执行选中的分支
            String branchOutput = executeSequential(userMessage, chatModelVo, branchAgents);

            // Step 6: 执行后处理器
            if (StringUtils.isNotBlank(conditionalConfig.getPostProcessor()) && branchOutput != null) {
                AgentNodeConfig postProcessor = findAgentByName(agents, conditionalConfig.getPostProcessor());
                if (postProcessor != null) {
                    // 构造输入：包含原始问题和分支执行结果
                    String finalInput = "【原始用户问题】\n" + userMessage +
                        "\n\n【分支执行结果】\n" + branchOutput;

                    log.info("执行后处理器: {}", conditionalConfig.getPostProcessor());
                    return executeOneAgent(finalInput, chatModelVo, postProcessor);
                }
            }

            return branchOutput;
        } catch (Exception e) {
            log.error("条件工作流执行异常", e);
            return "执行过程中发生未知错误：" + e.getMessage();
        }
    }

    /**
     * 从配置的分支映射中根据决策选择对应的 Agent 列表
     * 支持精确匹配和模糊包含匹配，并处理默认分支
     */
    private List<String> selectBranch(Map<String, List<String>> branches, String decision) {
        // 1. 精确匹配
        if (branches.containsKey(decision)) {
            return branches.get(decision);
        }

        // 2. 模糊匹配 (容错)
        for (Map.Entry<String, List<String>> entry : branches.entrySet()) {
            // 跳过 DEFAULT 分支，它只在最后兜底
            if (!"DEFAULT".equalsIgnoreCase(entry.getKey()) && decision.contains(entry.getKey())) {
                return entry.getValue();
            }
        }

        // 3. 兜底匹配 (DEFAULT)
        return branches.get("DEFAULT");
    }

    /**
     * 根据名称列表从配置池中解析出有效的 Agent 配置
     */
    private List<AgentNodeConfig> resolveAgents(List<AgentNodeConfig> allAgents, List<String> agentNames) {
        return agentNames.stream()
            .map(name -> findAgentByName(allAgents, name))
            .filter(java.util.Objects::nonNull)
            .collect(Collectors.toList());
    }

    /**
     * 根据名称查找 Agent
     *
     * @param agents 智能体集合
     * @param name   智能体名称
     * @return 智能体配置
     */
    private AgentNodeConfig findAgentByName(List<AgentNodeConfig> agents, String name) {
        return agents.stream()
            .filter(agent -> name.equals(agent.getName()))
            .findFirst()
            .orElse(null);
    }
}
