package org.ruoyi.service.chat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.ruoyi.domain.entity.agent.AiMarket;
import org.ruoyi.domain.entity.agent.AiMarketTool;
import org.ruoyi.domain.entity.agent.AiSkill;
import org.ruoyi.domain.entity.mcp.McpTool;
import org.ruoyi.mapper.agent.AiMarketMapper;
import org.ruoyi.mapper.agent.AiMarketToolMapper;
import org.ruoyi.mapper.agent.AiSkillMapper;
import org.ruoyi.mapper.mcp.McpToolMapper;
import org.ruoyi.service.chat.model.AgentExecutionMode;
import org.ruoyi.service.chat.model.AgentRuntimeConfig;
import org.ruoyi.service.chat.model.RuntimeAgentConfig;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 根据 ai_market 模板组装运行时 Agent 配置
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentMarketAssemblyService {

    private final AiMarketMapper aiMarketMapper;
    private final AiMarketToolMapper aiMarketToolMapper;
    private final AiSkillMapper aiSkillMapper;
    private final McpToolMapper mcpToolMapper;
    private final ObjectMapper objectMapper;

    public AgentRuntimeConfig assemble(Long marketId) {
        if (marketId == null) {
            return null;
        }

        AiMarket market = aiMarketMapper.selectById(marketId);
        if (market == null || market.getStatus() == null || market.getStatus() != 1) {
            log.warn("Agent market unavailable, marketId={}", marketId);
            return null;
        }

        List<String> builtinNames = new ArrayList<>();
        List<String> mcpNames = new ArrayList<>();
        resolveTools(marketId, builtinNames, mcpNames);

        ParsedConfig parsedConfig = parseConfigJson(market.getConfigJson(), builtinNames, mcpNames);

        AgentExecutionMode mode = parsedConfig != null && parsedConfig.mode() != null
            ? parsedConfig.mode()
            : AgentExecutionMode.SINGLE;

        List<RuntimeAgentConfig> runtimeAgents = parsedConfig != null && !CollectionUtils.isEmpty(parsedConfig.agents())
            ? parsedConfig.agents()
            : buildDefaultRuntimeAgents(market, builtinNames, mcpNames);

        String systemPrompt = parsedConfig != null && StringUtils.hasText(parsedConfig.systemPrompt())
            ? parsedConfig.systemPrompt()
            : buildDefaultSystemPrompt(market);

        String supervisorPrompt = parsedConfig != null && StringUtils.hasText(parsedConfig.supervisorPrompt())
            ? parsedConfig.supervisorPrompt()
            : buildSupervisorPrompt(market);

        return AgentRuntimeConfig.builder()
            .marketId(market.getId())
            .marketName(market.getMarketName())
            .systemPrompt(systemPrompt)
            .supervisorPrompt(supervisorPrompt)
            .executionMode(mode)
            .agents(runtimeAgents)
            .builtinToolNames(builtinNames)
            .mcpToolNames(mcpNames)
            .build();
    }

    private List<RuntimeAgentConfig> buildDefaultRuntimeAgents(AiMarket market,
                                                               List<String> builtinNames,
                                                               List<String> mcpNames) {
        List<RuntimeAgentConfig> agents = new ArrayList<>();
        List<String> toolNames = new ArrayList<>();
        toolNames.addAll(builtinNames);
        toolNames.addAll(mcpNames);

        agents.add(RuntimeAgentConfig.builder()
            .name(market.getMarketName())
            .role("primary")
            .systemPrompt("你是" + market.getMarketName() + "场景下的智能助手，请按场景目标完成任务。")
            .tools(toolNames)
            .build());
        return agents;
    }

    private void resolveTools(Long marketId, List<String> builtinNames, List<String> mcpNames) {
        List<AiMarketTool> relTools = aiMarketToolMapper.selectList(
            new LambdaQueryWrapper<AiMarketTool>().eq(AiMarketTool::getMarketId, marketId)
        );
        if (CollectionUtils.isEmpty(relTools)) {
            return;
        }

        List<Long> toolIds = relTools.stream().map(AiMarketTool::getToolId).toList();
        List<McpTool> tools = mcpToolMapper.selectList(new LambdaQueryWrapper<McpTool>().in(McpTool::getId, toolIds));
        if (CollectionUtils.isEmpty(tools)) {
            return;
        }

        for (McpTool tool : tools) {
            String toolName = tool.getName();
            if (!StringUtils.hasText(toolName)) {
                continue;
            }

            if ("BUILTIN".equalsIgnoreCase(tool.getType())) {
                builtinNames.add(toolName);
            } else {
                mcpNames.add(toolName);
            }
        }
    }

    private String buildDefaultSystemPrompt(AiMarket market) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是").append(market.getMarketName()).append("场景下的智能助手。\n");
        if (StringUtils.hasText(market.getDescription())) {
            prompt.append("场景描述：").append(market.getDescription()).append("\n");
        }

        prompt.append("\n输出要求：回答简洁、准确；需要使用工具时优先调用工具后再回答。\n");
        return prompt.toString();
    }

    private String buildSupervisorPrompt(AiMarket market) {
        if (StringUtils.hasText(market.getDescription())) {
            return "你是任务调度器。市场场景：" + market.getDescription() + "。请在候选 agent 中选择最合适的一个执行。";
        }
        return "你是任务调度器，请在候选 agent 中选择最合适的一个执行。";
    }

    private ParsedConfig parseConfigJson(String configJson, List<String> builtinNames, List<String> mcpNames) {
        if (!StringUtils.hasText(configJson)) {
            return null;
        }

        try {
            JsonNode root = objectMapper.readTree(configJson);
            List<String> allTools = new ArrayList<>();
            allTools.addAll(builtinNames);
            allTools.addAll(mcpNames);

            AgentExecutionMode mode = parseExecutionMode(root);
            String systemPrompt = text(root, "systemPrompt");
            String supervisorPrompt = text(root, "supervisorPrompt");
            List<RuntimeAgentConfig> agents = parseAgents(root.path("agents"), allTools);

            JsonNode supervisor = root.path("supervisor");
            if (supervisor.isObject()) {
                String supervisorName = defaultIfBlank(text(supervisor, "name"), "supervisor");
                String supervisorModel = text(supervisor, "model");
                String supPrompt = text(supervisor, "systemPrompt");
                if (StringUtils.hasText(supPrompt)) {
                    supervisorPrompt = supPrompt;
                }

                List<String> supervisorTools = parseTools(supervisor.path("tools"), allTools);
                List<String> childNames = parseStringArray(supervisor.path("agents"));

                List<RuntimeAgentConfig> children = agents;
                if (!CollectionUtils.isEmpty(childNames)) {
                    Set<String> childNameSet = childNames.stream()
                        .filter(StringUtils::hasText)
                        .map(String::toLowerCase)
                        .collect(java.util.stream.Collectors.toSet());
                    children = agents.stream()
                        .filter(a -> StringUtils.hasText(a.getName()) && childNameSet.contains(a.getName().toLowerCase()))
                        .toList();
                }

                if (CollectionUtils.isEmpty(children) && !CollectionUtils.isEmpty(agents)) {
                    children = agents;
                }

                List<RuntimeAgentConfig> merged = new ArrayList<>();
                merged.add(RuntimeAgentConfig.builder()
                    .name(supervisorName)
                    .role("primary")
                    .model(supervisorModel)
                    .systemPrompt(defaultIfBlank(supPrompt, "你是任务调度器，请选择最合适的子agent处理任务。"))
                    .skills(parseStringArray(supervisor.path("skills")))
                    .tools(supervisorTools)
                    .build());

                for (RuntimeAgentConfig child : children) {
                    merged.add(RuntimeAgentConfig.builder()
                        .name(child.getName())
                        .role("child")
                        .model(child.getModel())
                        .systemPrompt(child.getSystemPrompt())
                        .skills(child.getSkills())
                        .tools(child.getTools())
                        .build());
                }

                agents = merged;
                if (mode == null) {
                    mode = AgentExecutionMode.SUPERVISOR;
                }
            }

            if (mode == null && agents.size() > 1) {
                mode = AgentExecutionMode.SUPERVISOR;
            }

            agents = enrichAgentSkills(agents);

            return new ParsedConfig(mode, systemPrompt, supervisorPrompt, agents);
        } catch (Exception e) {
            log.warn("Parse ai_market.config_json failed: {}", e.getMessage());
            return null;
        }
    }

    private AgentExecutionMode parseExecutionMode(JsonNode root) {
        String modeText = text(root, "executionMode");
        if (!StringUtils.hasText(modeText)) {
            modeText = text(root, "mode");
        }
        if (!StringUtils.hasText(modeText)) {
            return null;
        }

        String normalized = modeText.trim().toUpperCase();
        return switch (normalized) {
            case "SINGLE" -> AgentExecutionMode.SINGLE;
            case "SUPERVISOR" -> AgentExecutionMode.SUPERVISOR;
            case "PARALLEL" -> AgentExecutionMode.PARALLEL;
            default -> null;
        };
    }

    private List<RuntimeAgentConfig> parseAgents(JsonNode agentsNode, List<String> allTools) {
        List<RuntimeAgentConfig> agents = new ArrayList<>();
        if (!agentsNode.isArray()) {
            return agents;
        }

        for (JsonNode node : agentsNode) {
            String name = text(node, "name");
            if (!StringUtils.hasText(name)) {
                continue;
            }

            String role = defaultIfBlank(text(node, "role"), "child");
            String model = text(node, "model");
            String prompt = defaultIfBlank(text(node, "systemPrompt"), "你是" + name + "，请完成用户任务。");
            List<String> skills = parseStringArray(node.path("skills"));
            List<String> tools = parseTools(node.path("tools"), allTools);

            agents.add(RuntimeAgentConfig.builder()
                .name(name)
                .role(role)
                .model(model)
                .systemPrompt(prompt)
                .skills(skills)
                .tools(tools)
                .build());
        }

        return agents;
    }

    private List<String> parseTools(JsonNode toolsNode, List<String> allTools) {
        List<String> tools = parseStringArray(toolsNode);
        if (CollectionUtils.isEmpty(tools)) {
            return allTools;
        }

        Set<String> allowed = new java.util.HashSet<>(allTools);
        return tools.stream().filter(allowed::contains).toList();
    }

    private List<String> parseStringArray(JsonNode node) {
        List<String> values = new ArrayList<>();
        if (!node.isArray()) {
            return values;
        }
        for (JsonNode item : node) {
            if (item != null && item.isTextual() && StringUtils.hasText(item.asText())) {
                values.add(item.asText().trim());
            }
        }
        return values;
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isTextual() ? value.asText() : null;
    }

    private String defaultIfBlank(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private List<RuntimeAgentConfig> enrichAgentSkills(List<RuntimeAgentConfig> agents) {
        if (CollectionUtils.isEmpty(agents)) {
            return agents;
        }

        Map<String, AiSkill> skillDict = buildSkillDictionary();
        if (skillDict.isEmpty()) {
            return agents;
        }

        List<RuntimeAgentConfig> enriched = new ArrayList<>(agents.size());
        for (RuntimeAgentConfig agent : agents) {
            List<String> boundSkills = agent.getSkills();
            if (CollectionUtils.isEmpty(boundSkills)) {
                enriched.add(agent);
                continue;
            }

            StringBuilder prompt = new StringBuilder(defaultIfBlank(agent.getSystemPrompt(), "你是智能助手，请完成用户任务。"));
            prompt.append("\n\n你具备以下技能：\n");

            for (String skillKey : boundSkills) {
                if (!StringUtils.hasText(skillKey)) {
                    continue;
                }
                AiSkill skill = skillDict.get(skillKey.trim().toLowerCase(Locale.ROOT));
                if (skill == null) {
                    prompt.append("- ").append(skillKey).append("\n");
                    continue;
                }

                String skillName = StringUtils.hasText(skill.getSkillName()) ? skill.getSkillName() : skillKey;
                prompt.append("- ").append(skillName);
                if (StringUtils.hasText(skill.getDescription())) {
                    prompt.append("：").append(skill.getDescription());
                }
                prompt.append("\n");
            }

            enriched.add(RuntimeAgentConfig.builder()
                .name(agent.getName())
                .role(agent.getRole())
                .model(agent.getModel())
                .systemPrompt(prompt.toString())
                .skills(agent.getSkills())
                .tools(agent.getTools())
                .build());
        }
        return enriched;
    }

    private Map<String, AiSkill> buildSkillDictionary() {
        List<AiSkill> allSkills = aiSkillMapper.selectList(new LambdaQueryWrapper<AiSkill>());
        if (CollectionUtils.isEmpty(allSkills)) {
            return java.util.Collections.emptyMap();
        }

        Map<String, AiSkill> dict = new HashMap<>();
        for (AiSkill skill : allSkills) {
            if (StringUtils.hasText(skill.getSkillCode())) {
                dict.put(skill.getSkillCode().trim().toLowerCase(Locale.ROOT), skill);
            }
            if (StringUtils.hasText(skill.getSkillName())) {
                dict.put(skill.getSkillName().trim().toLowerCase(Locale.ROOT), skill);
            }
        }
        return dict;
    }

    private record ParsedConfig(AgentExecutionMode mode,
                                String systemPrompt,
                                String supervisorPrompt,
                                List<RuntimeAgentConfig> agents) {
    }
}
