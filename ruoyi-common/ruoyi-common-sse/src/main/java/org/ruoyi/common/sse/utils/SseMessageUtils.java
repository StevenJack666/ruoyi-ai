package org.ruoyi.common.sse.utils;

import java.util.Collections;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.core.utils.SpringUtils;
import org.ruoyi.common.sse.core.SseEmitterManager;
import org.ruoyi.common.sse.dto.SseEventDto;
import org.ruoyi.common.sse.dto.SseMessageDto;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * SSE 工具类
 *
 * @author Lion Li
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SseMessageUtils {

    private static final Boolean SSE_ENABLE =
        SpringUtils.getProperty("sse.enabled", Boolean.class, true);
    private static SseEmitterManager MANAGER;

    static {
        if (isEnable() && MANAGER == null) {
            MANAGER = SpringUtils.getBean(SseEmitterManager.class);
        }
    }

    // ======================== 用户维度 ========================

    public static void sendMessage(Long userId, String message) {
        if (!isEnable()) {
            return;
        }
        SseMessageDto dto = new SseMessageDto();
        dto.setMessage(message);
        dto.setUserIds(Collections.singletonList(userId));
        MANAGER.publishMessage(dto);
    }

    public static void sendMessage(String message) {
        if (!isEnable()) {
            return;
        }
        MANAGER.sendMessage(message);
    }

    public static void publishMessage(SseMessageDto sseMessageDto) {
        if (!isEnable()) {
            return;
        }
        MANAGER.publishMessage(sseMessageDto);
    }

    public static void publishAll(String message) {
        if (!isEnable()) {
            return;
        }
        MANAGER.publishAll(message);
    }

    public static void completeConnection(Long userId, String tokenValue) {
        if (!isEnable()) {
            return;
        }
        MANAGER.disconnect(userId, tokenValue);
    }

    public static void sendEvent(Long userId, SseEventDto eventDto) {
        if (!isEnable()) {
            return;
        }
        MANAGER.sendEvent(userId, eventDto);
    }

    public static void sendContent(Long userId, String content) {
        sendEvent(userId, SseEventDto.content(content));
    }

    public static void sendReasoning(Long userId, String reasoningContent) {
        sendEvent(userId, SseEventDto.reasoning(reasoningContent));
    }

    public static void sendDone(Long userId) {
        sendEvent(userId, SseEventDto.done());
    }

    public static void sendError(Long userId, String error) {
        sendEvent(userId, SseEventDto.error(error));
    }

    // ======================== 会话/流维度 ========================

    /**
     * 完成指定 SSE 通道。
     * channelKey = sessionId:streamId
     */
    public static void completeConnection(String channelKey) {
        if (!isEnable() || channelKey == null) {
            return;
        }
        MANAGER.disconnect(channelKey);
    }

    /**
     * 向指定 SSE 通道发送结构化事件：
     * 本机优先直投；本机没有连接时走 Redis 广播。
     */
    public static void sendEvent(String channelKey, SseEventDto eventDto) {
        if (!isEnable() || channelKey == null) {
            return;
        }

        // 本机有连接，直接投递
        if (MANAGER.sendLocalEvent(channelKey, eventDto)) {
            return;
        }

        // 本机没有连接，广播给持有连接的实例
        SseMessageDto dto = new SseMessageDto();
        dto.setChannelKey(channelKey);
        dto.setEventDto(eventDto);
        MANAGER.publishMessage(dto);
    }

    public static void sendMessage(String channelKey, String message) {
        sendEvent(channelKey, SseEventDto.content(message));
    }

    public static void sendContent(String channelKey, String content) {
        sendEvent(channelKey, SseEventDto.content(content));
    }

    public static void sendReasoning(String channelKey, String reasoningContent) {
        sendEvent(channelKey, SseEventDto.reasoning(reasoningContent));
    }

    public static void sendDone(String channelKey) {
        sendEvent(channelKey, SseEventDto.done());
    }

    public static void sendError(String channelKey, String error) {
        sendEvent(channelKey, SseEventDto.error(error));
    }

    /**
     * 终止性错误：error 事件带 done=true，投递后立即释放连接。
     */
    public static void sendErrorAndComplete(String channelKey, String error) {
        SseEventDto terminal = SseEventDto.error(error);
        terminal.setDone(true);
        sendEvent(channelKey, terminal);
    }

    public static Boolean isEnable() {
        return SSE_ENABLE;
    }

    // ======================== 请求级 emitter ========================

    public static void sendEvent(SseEmitter emitter, SseEventDto eventDto) {
        if (!isEnable()) {
            return;
        }
        MANAGER.sendEvent(emitter, eventDto);
    }

    public static void sendContent(SseEmitter emitter, String content) {
        sendEvent(emitter, SseEventDto.content(content));
    }

    public static void sendDone(SseEmitter emitter) {
        sendEvent(emitter, SseEventDto.done());
    }

    public static void sendError(SseEmitter emitter, String error) {
        sendEvent(emitter, SseEventDto.error(error));
    }

    public static void complete(SseEmitter emitter) {
        if (!isEnable()) {
            return;
        }
        MANAGER.complete(emitter);
    }

    // ======================== 兼容旧调用（Long sessionId） ========================

    public static void sendMessage(Long userId, Long sessionId, String message) {
        if (!isEnable()) {
            return;
        }
        if (sessionId == null) {
            MANAGER.sendMessage(userId, message);
            return;
        }
        sendMessage(String.valueOf(sessionId), message);
    }

    public static void sendEvent(Long userId, Long sessionId, SseEventDto eventDto) {
        if (!isEnable()) {
            return;
        }
        if (sessionId == null) {
            MANAGER.sendEvent(userId, eventDto);
            return;
        }
        sendEvent(String.valueOf(sessionId), eventDto);
    }

    public static void sendContent(Long userId, Long sessionId, String content) {
        sendEvent(userId, sessionId, SseEventDto.content(content));
    }

    public static void sendReasoning(Long userId, Long sessionId, String reasoningContent) {
        sendEvent(userId, sessionId, SseEventDto.reasoning(reasoningContent));
    }

    public static void sendDone(Long userId, Long sessionId) {
        sendEvent(userId, sessionId, SseEventDto.done());
    }

    public static void sendError(Long userId, Long sessionId, String error) {
        sendEvent(userId, sessionId, SseEventDto.error(error));
    }

    public static void completeConnection(Long userId, Long sessionId) {
        if (!isEnable() || sessionId == null) {
            return;
        }
        completeConnection(String.valueOf(sessionId));
    }
}
