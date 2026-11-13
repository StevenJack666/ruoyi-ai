package org.ruoyi.observability;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.internal.Json;
import dev.langchain4j.service.tool.ToolExecutor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.core.utils.SpringUtils;
import org.ruoyi.domain.result.InterceptResult;
import org.ruoyi.mcp.service.core.ToolConfirmationManager;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.*;

import static com.baomidou.mybatisplus.extension.spi.SpringCompatibleSet.applicationContext;

@Slf4j
public class GobalToolExecutor implements ToolExecutor {

    private final ToolExecutor originalExecutor;
    private final Long userId;
    private final Long sessionId;
    private final SseEmitter emitter;
    private final String sseChannelKey;
    private final List<ToolInterceptStrategy> strategies;

    private final ToolConfirmationManager confirmationManager =
        SpringUtils.getBean(ToolConfirmationManager.class);

    public GobalToolExecutor(ToolExecutor originalExecutor, Long userId) {
        this(originalExecutor, userId, null, null, null);
    }

    public GobalToolExecutor(ToolExecutor originalExecutor, Long userId, Long sessionId) {
        this(originalExecutor, userId, sessionId, null, null);
    }

    public GobalToolExecutor(ToolExecutor originalExecutor, Long userId,
                             Long sessionId, SseEmitter emitter) {
        this(originalExecutor, userId, sessionId, emitter, null);
    }

    public GobalToolExecutor(ToolExecutor originalExecutor, Long userId,
                             Long sessionId, SseEmitter emitter,
                             String sseChannelKey) {
        this.originalExecutor = originalExecutor;
        this.userId = userId;
        this.sessionId = sessionId;
        this.emitter = emitter;
        this.sseChannelKey = sseChannelKey;
        this.strategies = new ArrayList<>(
            applicationContext.getBeansOfType(ToolInterceptStrategy.class).values()
        );
    }

    @Override
    @SuppressWarnings("unchecked")
    public String execute(ToolExecutionRequest request, Object memoryId) {
        String toolName = request.name();

        for (ToolInterceptStrategy strategy : strategies) {
            if (strategy.matches(toolName)) {
                InterceptResult result = strategy.onBeforeExecute(
                    toolName, request.arguments(), userId
                );

                if (!result.isShouldContinue() && confirmationManager != null) {
                    log.info("🛑 工具 [{}] 已被策略拦截，开始阻塞等待用户确认...", toolName);
                    Map<String, Object> args = Json.fromJson(request.arguments(), Map.class);
                    String confirmId = confirmationManager.createConfirmation(
                        userId, sessionId, emitter, sseChannelKey, toolName, args
                    );

                    boolean approved = confirmationManager.waitForConfirmation(confirmId);
                    if (!approved) {
                        log.info("❌ 用户拒绝或超时，取消工具调用: {}", toolName);
                        return "用户拒绝了该操作，或操作已超时。";
                    }
                    log.info("✅ 用户已同意，线程被唤醒，准备继续执行: {}", toolName);
                }
                break;
            }
        }

        log.info("🚀 放行请求，准备调用原始 MCP 工具: {}", toolName);
        return originalExecutor.execute(request, memoryId);
    }
}
