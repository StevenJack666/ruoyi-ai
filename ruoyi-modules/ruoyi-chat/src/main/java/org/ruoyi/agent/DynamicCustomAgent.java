package org.ruoyi.agent;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 配置Agent智能体
 */
public interface DynamicCustomAgent {

    @UserMessage("{{query}}")
    @Agent("动态自定义智能体: 严格按照注入的系统提示词进行角色扮演和任务处理")
    String chat(@V("query") String query);
}
