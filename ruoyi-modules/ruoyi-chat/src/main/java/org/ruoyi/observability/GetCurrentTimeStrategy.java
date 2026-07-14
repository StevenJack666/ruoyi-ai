package org.ruoyi.observability;

import lombok.extern.slf4j.Slf4j;
import org.ruoyi.domain.result.InterceptResult;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class GetCurrentTimeStrategy extends AbstractToolInterceptStrategy {

    @Override
    public boolean matches(String toolName) {
        return "getCurrentTime".equals(toolName);
    }

    @Override
    public InterceptResult onBeforeExecute(String toolName, String argumentsJson, Long userId) {
        log.info("🔥 触发 getCurrentTime 专属拦截逻辑");
        return new InterceptResult(true, "", ""); // 返回 true，表示继续执行原来的 MCP 工具
    }
}
