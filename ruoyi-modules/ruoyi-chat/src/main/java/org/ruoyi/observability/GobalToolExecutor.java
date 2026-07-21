package org.ruoyi.observability;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.internal.Json;
import dev.langchain4j.service.tool.ToolExecutor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.core.utils.SpringUtils;
import org.ruoyi.domain.result.InterceptResult;
import org.ruoyi.mcp.service.core.ToolConfirmationManager;

import java.util.*;

import static com.baomidou.mybatisplus.extension.spi.SpringCompatibleSet.applicationContext;

@Slf4j
public class GobalToolExecutor implements ToolExecutor {

    private final ToolExecutor originalExecutor;
    private final Long userId;
    private final List<ToolInterceptStrategy> strategies;

    private final ToolConfirmationManager confirmationManager = SpringUtils.getBean(ToolConfirmationManager.class);


    public GobalToolExecutor(ToolExecutor originalExecutor, Long userId) {
        this.originalExecutor = originalExecutor;
        this.userId = userId;
        this.strategies = new ArrayList<>(applicationContext.getBeansOfType(ToolInterceptStrategy.class).values());
    }

    @Override
    @SuppressWarnings("unchecked")
    public String execute(ToolExecutionRequest request, Object memoryId) {
        String toolName = request.name();

        // 1. 遍历策略，寻找匹配项
        for (ToolInterceptStrategy strategy : strategies) {
            if (strategy.matches(toolName)) {
                // 2. 调用【操作前方法】（比如推送 SSE 预览卡片）
                InterceptResult result = strategy.onBeforeExecute(toolName, request.arguments(), userId);
                // 3. 如果策略要求阻断，且配置了确认管理器
                if (!result.isShouldContinue() && confirmationManager != null) {
                    log.info("🛑 工具 [{}] 已被策略拦截，开始阻塞等待用户确认...", toolName);
                    Map<String, Object> args = Json.fromJson(request.arguments(), Map.class);
                    String confirmId = confirmationManager.createConfirmation(userId, toolName, args);
                    // 最长等待 2 分钟
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

        // 4. 放行请求，调用真实的 MCP 执行器
        log.info("🚀 放行请求，准备调用原始 MCP 工具: {}", toolName);
        return originalExecutor.execute(request, memoryId);
    }
}
