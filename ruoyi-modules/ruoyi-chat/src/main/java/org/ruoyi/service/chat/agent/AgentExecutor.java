package org.ruoyi.service.chat.agent;

import org.ruoyi.common.chat.domain.dto.request.ChatRequest;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;

/**
 * Agent 执行器抽象
 */
public interface AgentExecutor {

    /**
     * 当前执行器是否支持该请求
     */
    boolean supports(ChatRequest request);

    /**
     * 执行 Agent 调用
     */
    String execute(String userMessage, ChatModelVo chatModelVo, ChatRequest request);
}
