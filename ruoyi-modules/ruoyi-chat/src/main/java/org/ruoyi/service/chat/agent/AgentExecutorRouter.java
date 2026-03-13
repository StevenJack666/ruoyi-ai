package org.ruoyi.service.chat.agent;

import java.util.List;

import org.ruoyi.common.chat.domain.dto.request.ChatRequest;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;

/**
 * Agent 执行路由器
 */
@Component
@RequiredArgsConstructor
public class AgentExecutorRouter {

    private final List<AgentExecutor> executors;

    public String execute(String userMessage, ChatModelVo chatModelVo, ChatRequest request) {
        for (AgentExecutor executor : executors) {
            if (executor.supports(request)) {
                String result = executor.execute(userMessage, chatModelVo, request);
                if (StringUtils.hasText(result)) {
                    return result;
                }
            }
        }
        throw new IllegalStateException("No AgentExecutor found for current request");
    }
}
