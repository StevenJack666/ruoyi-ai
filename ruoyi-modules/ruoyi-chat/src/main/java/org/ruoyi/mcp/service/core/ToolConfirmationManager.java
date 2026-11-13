package org.ruoyi.mcp.service.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.common.sse.dto.SseEventDto;
import org.ruoyi.common.sse.utils.SseMessageUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * MCP 工具调用确认管理器
 *
 * @author ruoyi team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ToolConfirmationManager {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final long DEFAULT_TIMEOUT_SECONDS = 120;

    private static final String CONFIRM_QUEUE_PREFIX = "agent:tool:confirm:";

    private final StringRedisTemplate redisTemplate;

    public String createConfirmation(Long userId, String toolName, Map<String, Object> args) {
        return createConfirmation(userId, null, null, null, toolName, args);
    }

    public String createConfirmation(Long userId, Long sessionId,
                                     String toolName, Map<String, Object> args) {
        return createConfirmation(userId, sessionId, null, null, toolName, args);
    }

    public String createConfirmation(Long userId, Long sessionId, SseEmitter emitter,
                                     String toolName, Map<String, Object> args) {
        return createConfirmation(userId, sessionId, emitter, null, toolName, args);
    }

    /**
     * 创建确认请求并推送 SSE 事件。
     *
     * @param sseChannelKey 精确 SSE 通道 key：sessionId:streamId
     */
    public String createConfirmation(Long userId, Long sessionId, SseEmitter emitter,
                                     String sseChannelKey, String toolName,
                                     Map<String, Object> args) {
        String confirmId = UUID.randomUUID().toString().replace("-", "");
        pushConfirmationEvent(
            userId, sessionId, emitter, sseChannelKey,
            confirmId, toolName, args
        );
        log.info("创建工具确认请求并推入Redis监听: confirmId={}, userId={}, sessionId={}, channelKey={}",
            confirmId, userId, sessionId, sseChannelKey);
        return confirmId;
    }

    /**
     * 阻塞等待用户确认（基于 Redis BRPOP）
     */
    public boolean waitForConfirmation(String confirmId) {
        String queueKey = CONFIRM_QUEUE_PREFIX + confirmId;
        try {
            String result = redisTemplate.opsForList()
                .rightPop(queueKey, DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            if (StringUtils.isEmpty(result)) {
                log.warn("确认请求超时: {}", confirmId);
                return false;
            }
            return Boolean.parseBoolean(result);
        } catch (Exception e) {
            log.error("Redis 确认请求异常: {}", confirmId, e);
            return false;
        } finally {
            redisTemplate.delete(queueKey);
        }
    }

    /**
     * 用户响应确认（同意或拒绝）
     */
    public boolean respond(String confirmId, boolean approved) {
        String queueKey = CONFIRM_QUEUE_PREFIX + confirmId;
        try {
            redisTemplate.opsForList().leftPush(queueKey, String.valueOf(approved));
            return true;
        } catch (Exception e) {
            log.error("Redis 推送确认结果失败: {}", confirmId, e);
            return false;
        }
    }

    private void pushConfirmationEvent(Long userId, Long sessionId,
                                       SseEmitter emitter, String sseChannelKey,
                                       String confirmId, String toolName,
                                       Map<String, Object> args) {
        try {
            Map<String, Object> content = new HashMap<>();
            content.put("confirmId", confirmId);
            content.put("toolName", toolName);
            content.put("arguments", args);

            String json = OBJECT_MAPPER.writeValueAsString(content);
            SseEventDto dto = SseEventDto.builder()
                .event("tool_confirmation")
                .content(json)
                .build();

            // 1. 优先精确 channelKey：本机直投，跨实例 Redis
            if (sseChannelKey != null) {
                SseMessageUtils.sendEvent(sseChannelKey, dto);
                return;
            }

            // 2. 请求级 emitter
            if (emitter != null) {
                SseMessageUtils.sendEvent(emitter, dto);
                return;
            }

            // 3. 兼容旧路径
            if (sessionId != null) {
                SseMessageUtils.sendEvent(userId, sessionId, dto);
            } else if (userId != null) {
                SseMessageUtils.sendEvent(userId, dto);
            } else {
                log.warn("userId/sessionId/emitter/channelKey 均为空，无法推送工具确认事件");
            }
        } catch (Exception e) {
            log.error("推送工具确认事件失败: {}", e.getMessage());
        }
    }
}
