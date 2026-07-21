package org.ruoyi.mcp.service.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.common.sse.dto.SseEventDto;
import org.ruoyi.common.sse.utils.SseMessageUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.*;
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

    // Redis List 的 Key 前缀
    private static final String CONFIRM_QUEUE_PREFIX = "agent:tool:confirm:";

    private final StringRedisTemplate redisTemplate;

    /**
     * 创建确认请求，推送 SSE 事件到前端
     */
    public String createConfirmation(Long userId, String toolName, Map<String, Object> args) {
        String confirmId = UUID.randomUUID().toString().replace("-", "");
        pushConfirmationEvent(userId, confirmId, toolName, args);
        log.info("创建工具确认请求并推入Redis监听: confirmId={}, userId={}", confirmId, userId);
        return confirmId;
    }

    /**
     * 阻塞等待用户确认（基于 Redis BRPOP）
     * 当队列没有数据时，线程会进入休眠状态，不消耗 CPU；
     * 一旦前端调用 /tool-confirm 推入数据，线程会立刻醒过来。
     */
    public boolean waitForConfirmation(String confirmId) {
        String queueKey = CONFIRM_QUEUE_PREFIX + confirmId;
        try {
            // 🔥 核心：使用 BRPOP 阻塞等待，超时时间设为 120 秒
            // 0 表示无限等待，这里我们设置合理的超时时间防止线程永久挂起
            String result = redisTemplate.opsForList().rightPop(queueKey, DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            if (StringUtils.isEmpty(result)) {
                log.warn("确认请求超时: {}", confirmId);
                return false;
            }
            // result 是弹出的值（"true" 或 "false"）
            return Boolean.parseBoolean(result);
        } catch (Exception e) {
            log.error("Redis 确认请求异常: {}", confirmId, e);
            return false;
        } finally {
            // 消费完毕后，清理可能残留的 Key
            redisTemplate.delete(queueKey);
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
        String queueKey = CONFIRM_QUEUE_PREFIX + confirmId;
        try {
            // 🔥 核心：将结果推入队列头部
            // 正在 BRPOP 等待的线程会立刻被唤醒并拿到这个值
            redisTemplate.opsForList().leftPush(queueKey, String.valueOf(approved));
            return true;
        } catch (Exception e) {
            log.error("Redis 推送确认结果失败: {}", confirmId, e);
            return false;
        }
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
}
