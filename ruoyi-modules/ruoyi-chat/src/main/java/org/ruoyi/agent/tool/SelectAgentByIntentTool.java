package org.ruoyi.agent.tool;

import org.ruoyi.common.core.utils.SpringUtils;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.mcp.service.core.BuiltinToolProvider;
import org.ruoyi.service.chat.AgentIntentRoutingService;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;

/**
 * 根据用户输入意图从 ai_market 中选择最合适 Agent 的 Tool
 */
@Slf4j
@Component
public class SelectAgentByIntentTool implements BuiltinToolProvider {

    private AgentIntentRoutingService getRoutingService() {
        return SpringUtils.getBean(AgentIntentRoutingService.class);
    }

    /**
     * 根据用户输入选择最合适的 Agent。
     * 返回数组：匹配到则只有一个元素；未匹配到返回空数组 []。
     */
    @Tool("Select the best agent market from ai_market by user input. Return [] when no suitable agent is found.")
    public String selectAgentByIntent(String userInput) {
        if (StringUtils.isBlank(userInput)) {
            return "[]";
        }

        return getRoutingService().selectBestMatch(userInput)
            .map(match -> {
                JSONObject item = new JSONObject();
                item.put("marketId", match.marketId());
                item.put("marketName", match.marketName());
                item.put("description", match.description());
                item.put("score", match.score());

                JSONArray result = new JSONArray();
                result.add(item);
                return result.toJSONString();
            })
            .orElse("[]");
    }

    @Override
    public String getToolName() {
        return "select_agent_by_intent";
    }

    @Override
    public String getDisplayName() {
        return "意图选择Agent";
    }

    @Override
    public String getDescription() {
        return "Select the best agent market from ai_market by user input. Return [] when no suitable agent is found.";
    }
}