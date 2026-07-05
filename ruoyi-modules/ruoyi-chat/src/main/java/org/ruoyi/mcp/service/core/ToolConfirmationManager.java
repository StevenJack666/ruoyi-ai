package org.ruoyi.mcp.service.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.sse.dto.SseEventDto;
import org.ruoyi.common.sse.utils.SseMessageUtils;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * MCP 工具调用确认管理器
 * <p>
 * AI 调用工具前，等待用户二次确认。
 * 通过 SSE 向前端推送确认请求，前端弹窗让用户选择同意/拒绝。
 *
 * @author ruoyi team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ToolConfirmationManager {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final long DEFAULT_TIMEOUT_SECONDS = 120;

    /** 待确认请求: confirmId -> PendingConfirm */
    private final Map<String, PendingConfirm> pendingConfirmations = new ConcurrentHashMap<>();

    /**
     * 创建确认请求，推送 SSE 事件到前端
     *
     * @param userId   用户ID
     * @param toolName 工具名称
     * @param args     工具参数
     * @return confirmId
     */
    public String createConfirmation(Long userId, String toolName, Map<String, Object> args) {
        String confirmId = UUID.randomUUID().toString().replace("-", "");
        pendingConfirmations.put(confirmId, new PendingConfirm(userId, new CompletableFuture<>()));
        pushConfirmationEvent(userId, confirmId, toolName, args);
        log.info("创建工具确认请求: confirmId={}, userId={}, toolName={}", confirmId, userId, toolName);
        return confirmId;
    }

    /**
     * 等待用户确认（阻塞）
     *
     * @return true=同意, false=拒绝/超时
     */
    public boolean waitForConfirmation(String confirmId) {
        PendingConfirm pc = pendingConfirmations.get(confirmId);
        if (pc == null) return false;
        try {
            Boolean result = pc.future.get(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            return result;
        } catch (java.util.concurrent.TimeoutException e) {
            log.warn("确认请求超时: {}", confirmId);
            return false;
        } catch (Exception e) {
            log.error("确认请求异常: {}", confirmId, e);
            return false;
        } finally {
            pendingConfirmations.remove(confirmId);
        }
    }

    /**
     * 用户响应确认（同意或拒绝）
     *
     * @param confirmId 确认ID
     * @param approved  true=同意, false=拒绝
     * @return 是否找到对应的确认请求
     */
    public boolean respond(String confirmId, boolean approved) {
        PendingConfirm pc = pendingConfirmations.get(confirmId);
        if (pc == null) return false;
        pc.future.complete(approved);
        return true;
    }

    private void pushConfirmationEvent(Long userId, String confirmId, String toolName, Map<String, Object> args) {
        try {
            Map<String, Object> content = new HashMap<>();
            content.put("confirmId", confirmId);
            content.put("toolName", toolName);
            content.put("arguments", args);

            String json = OBJECT_MAPPER.writeValueAsString(content);
            SseMessageUtils.sendEvent(userId, SseEventDto.builder()
                .event("tool_confirmation")
                .content(json)
                .build());
        } catch (Exception e) {
            log.error("推送工具确认事件失败: {}", e.getMessage());
        }
    }

    private record PendingConfirm(Long userId, CompletableFuture<Boolean> future) {}
}
