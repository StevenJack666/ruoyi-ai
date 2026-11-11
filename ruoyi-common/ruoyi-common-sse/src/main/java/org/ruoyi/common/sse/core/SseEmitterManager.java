package org.ruoyi.common.sse.core;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.core.utils.SpringUtils;
import org.ruoyi.common.redis.utils.RedisUtils;
import org.ruoyi.common.sse.dto.SseEventDto;
import org.ruoyi.common.sse.dto.SseMessageDto;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * 管理 Server-Sent Events (SSE) 连接
 *
 * @author Lion Li
 */
@Slf4j
public class SseEmitterManager {

    /**
     * 订阅的频道
     */
    private final static String SSE_TOPIC = "global:sse";

    /**
     * 会话通道前缀，避免与 token 通道的 key 冲突
     */
    private final static String SESSION_CHANNEL_PREFIX = "session:";

    /**
     * 用户通道集合：外层 key 为用户ID，内层 key 为通道标识
     * 通道标识可以是 token（全局/长连接），也可以是 session:{sessionId}（会话隔离）
     */
    private final static Map<Long, Map<String, SseEmitter>> USER_CHANNEL_EMITTERS = new ConcurrentHashMap<>();

    public SseEmitterManager() {
        // 定时执行 SSE 心跳检测
        SpringUtils.getBean(ScheduledExecutorService.class)
            .scheduleWithFixedDelay(this::sseMonitor, 60L, 60L, TimeUnit.SECONDS);
    }

    /**
     * 设置会话通道
     */
    public static String sessionChannel(Long sessionId) {
        return SESSION_CHANNEL_PREFIX + sessionId;
    }

    /**
     * 建立与指定用户的 SSE 连接
     *
     * @param userId 用户的唯一标识符，用于区分不同用户的连接
     * @param token  用户的唯一令牌，用于识别具体的连接
     * @return 返回一个 SseEmitter 实例，客户端可以通过该实例接收 SSE 事件
     */
    public SseEmitter connect(Long userId, String token) {
        return connectByChannel(userId, token);
    }

    /**
     * 按「用户 + 会话」建立 SSE 连接，同一用户的多个会话互相隔离
     *
     * @param userId    用户ID
     * @param sessionId 会话ID
     * @return SseEmitter，sessionId 为空时返回 null
     */
    public SseEmitter connectSession(Long userId, Long sessionId) {
        if (userId == null || sessionId == null) {
            return null;
        }
        return connectByChannel(userId, sessionChannel(sessionId));
    }

    /**
     * 根据管道进行连接
     * @param userId 用户的唯一标识符，用于区分不同用户的连接
     * @param channel 管道标识（Topic）
     * @return 返回一个 SseEmitter 实例，客户端可以通过该实例接收 SSE 事件
     */
    private SseEmitter connectByChannel(Long userId, String channel) {
        Map<String, SseEmitter> emitters = USER_CHANNEL_EMITTERS.computeIfAbsent(
            userId, k -> new ConcurrentHashMap<>()
        );

        // 同一通道只保留最新连接，防止超过最大连接数
        SseEmitter oldEmitter = emitters.remove(channel);
        if (oldEmitter != null) {
            oldEmitter.complete();
        }

        SseEmitter emitter = new SseEmitter(86400000L);
        emitters.put(channel, emitter);

        emitter.onCompletion(() -> removeEmitter(userId, channel));
        emitter.onTimeout(() -> removeEmitter(userId, channel));
        emitter.onError((e) -> {
            log.error("SSE连接异常，准备移除连接 userId: {} channel: {}", userId, channel, e);
            removeEmitter(userId, channel);
        });

        try {
            emitter.send(SseEmitter.event().comment("connected"));
        } catch (IOException e) {
            removeEmitter(userId, channel);
        }
        return emitter;
    }

    /**
     * 断开指定用户的 SSE 连接
     *
     * @param userId 用户的唯一标识符，用于区分不同用户的连接
     * @param token  用户的唯一令牌，用于识别具体的连接
     */
    public void disconnect(Long userId, String token) {
        disconnectByChannel(userId, token);
    }

    /**
     * 断开指定用户会话的 SSE 连接
     */
    public void disconnectSession(Long userId, Long sessionId) {
        if (userId == null || sessionId == null) {
            return;
        }
        disconnectByChannel(userId, sessionChannel(sessionId));
    }

    /**
     * 根据管道名称进行关闭连接
     * @param userId 用户的唯一标识符，用于区分不同用户的连接
     * @param channel 管道标识（topic）
     */
    private void disconnectByChannel(Long userId, String channel) {
        if (userId == null || channel == null) {
            return;
        }
        Map<String, SseEmitter> emitters = USER_CHANNEL_EMITTERS.get(userId);
        if (MapUtil.isNotEmpty(emitters)) {
            SseEmitter sseEmitter = emitters.get(channel);
            if (sseEmitter != null) {
                try {
                    sseEmitter.send(SseEmitter.event().comment("disconnected"));
                } catch (Exception exception) {
                    log.error("SSE disconnect send error userId: {} channel: {}",
                        userId, channel, exception.getMessage(), exception);
                } finally {
                    removeEmitter(userId, channel);
                    safeComplete(sseEmitter);
                }
            } else {
                removeEmitter(userId, channel);
            }
        } else {
            USER_CHANNEL_EMITTERS.remove(userId);
        }
    }

    /**
     * SSE 心跳检测，关闭无效连接
     */
    public void sseMonitor() {
        final SseEmitter.SseEventBuilder heartbeat = SseEmitter.event().comment("heartbeat");
        // 记录需要移除的用户ID
        List<Long> toRemoveUsers = new ArrayList<>();

        USER_CHANNEL_EMITTERS.forEach((userId, emitterMap) -> {
            if (CollUtil.isEmpty(emitterMap)) {
                toRemoveUsers.add(userId);
                return;
            }

            emitterMap.entrySet().removeIf(entry -> {
                try {
                    entry.getValue().send(heartbeat);
                    return false;
                } catch (Exception ex) {
                    try {
                        entry.getValue().complete();
                    } catch (Exception ignore) {
                        // 忽略重复关闭异常
                    }
                    return true; // 发送失败 → 移除该连接
                }
            });

            // 移除空连接用户
            if (emitterMap.isEmpty()) {
                toRemoveUsers.add(userId);
            }
        });

        // 循环结束后统一清理空用户，避免并发修改异常
        toRemoveUsers.forEach(USER_CHANNEL_EMITTERS::remove);
    }

    /**
     * 订阅SSE消息主题，并提供一个消费者函数来处理接收到的消息
     *
     * @param consumer 处理SSE消息的消费者函数
     */
    public void subscribeMessage(Consumer<SseMessageDto> consumer) {
        RedisUtils.subscribe(SSE_TOPIC, SseMessageDto.class, consumer);
    }

    /**
     * 向指定的用户会话发送消息
     *
     * @param userId  要发送消息的用户id
     * @param message 要发送的消息内容
     */
    public void sendMessage(Long userId, String message) {
        Map<String, SseEmitter> emitters = USER_CHANNEL_EMITTERS.get(userId);
        if (MapUtil.isNotEmpty(emitters)) {
            for (Map.Entry<String, SseEmitter> entry : emitters.entrySet()) {
                try {
                    // 格式化为标准SSE JSON格式
                    SseEventDto eventDto = SseEventDto.content(message);
                    entry.getValue().send(SseEmitter.event()
                        .name("message")
                        .data(JSONUtil.toJsonStr(eventDto)));
                } catch (Exception e) {
                    SseEmitter remove = emitters.remove(entry.getKey());
                    if (remove != null) {
                        remove.complete();
                    }
                }
            }
        } else {
            USER_CHANNEL_EMITTERS.remove(userId);
        }
    }

    /**
     * 向指定用户的指定会话发送消息（仅该会话可见）
     */
    public void sendMessage(Long userId, Long sessionId, String message) {
        if (userId == null || sessionId == null) {
            return;
        }
        sendMessageToChannel(userId, sessionChannel(sessionId), message);
    }

    /**
     * 向通道发送消息
     * @param userId 要发送消息的用户id
     * @param channel 通道名
     * @param message 要发送的消息内容
     */
    private void sendMessageToChannel(Long userId, String channel, String message) {
        Map<String, SseEmitter> emitters = USER_CHANNEL_EMITTERS.get(userId);
        SseEmitter emitter = emitters == null ? null : emitters.get(channel);
        if (emitter == null) {
            return;
        }
        try {
            SseEventDto eventDto = SseEventDto.content(message);
            emitter.send(SseEmitter.event()
                .name("message")
                .data(JSONUtil.toJsonStr(eventDto)));
        } catch (Exception e) {
            removeEmitter(userId, channel);
            safeComplete(emitter);
        }
    }


    /**
     * 向指定的用户会话发送结构化事件
     *
     * @param userId    要发送消息的用户id
     * @param eventDto  SSE事件对象
     */
    public void sendEvent(Long userId, SseEventDto eventDto) {
        Map<String, SseEmitter> emitters = USER_CHANNEL_EMITTERS.get(userId);
        if (MapUtil.isNotEmpty(emitters)) {
            log.debug("【SSE发送】userId: {}, emitter数量: {}, event: {}", userId, emitters.size(), eventDto.getEvent());
            for (Map.Entry<String, SseEmitter> entry : emitters.entrySet()) {
                try {
                    entry.getValue().send(SseEmitter.event()
                        .name(eventDto.getEvent())
                        .data(JSONUtil.toJsonStr(eventDto)));
                } catch (Exception e) {
                    log.error("【SSE发送失败】userId: {}, token: {}, error: {}", userId, entry.getKey(), e.getMessage());
                    SseEmitter remove = emitters.remove(entry.getKey());
                    if (remove != null) {
                        safeComplete(remove);
                    }
                }
            }
        } else {
            log.warn("【SSE发送失败】userId: {} 没有活跃的SSE连接, 当前连接用户: {}", userId, USER_CHANNEL_EMITTERS.keySet());
            USER_CHANNEL_EMITTERS.remove(userId);
        }
    }

    /**
     * 向指定用户的指定会话发送结构化事件（仅该会话可见）
     */
    public void sendEvent(Long userId, Long sessionId, SseEventDto eventDto) {
        if (userId == null || sessionId == null) {
            return;
        }
        sendEventToChannel(userId, sessionChannel(sessionId), eventDto);
    }

    /**
     * 向指定的通道发送结构化事件
     * @param userId 要发送消息的用户id
     * @param channel 通道
     * @param eventDto SSE事件对象
     */
    private void sendEventToChannel(Long userId, String channel, SseEventDto eventDto) {
        Map<String, SseEmitter> emitters = USER_CHANNEL_EMITTERS.get(userId);
        SseEmitter emitter = emitters == null ? null : emitters.get(channel);
        if (emitter == null) {
            log.warn("【SSE发送失败】userId: {} channel: {} 没有活跃的SSE连接", userId, channel);
            return;
        }
        try {
            emitter.send(SseEmitter.event()
                .name(eventDto.getEvent())
                .data(JSONUtil.toJsonStr(eventDto)));
        } catch (Exception e) {
            log.error("【SSE发送失败】userId: {}, channel: {}, error: {}",
                userId, channel, e.getMessage());
            removeEmitter(userId, channel);
            safeComplete(emitter);
        }
    }

    /**
     * 将用户指定的会话通道关闭
     * @param userId 要关闭的用户id
     * @param channel 通道名称
     */
    private void removeEmitter(Long userId, String channel) {
        if (userId == null || channel == null) {
            return;
        }
        Map<String, SseEmitter> emitters = USER_CHANNEL_EMITTERS.get(userId);
        if (MapUtil.isNotEmpty(emitters)) {
            emitters.remove(channel);
            if (emitters.isEmpty()) {
                USER_CHANNEL_EMITTERS.remove(userId);
            }
        }
    }

    private void safeComplete(SseEmitter emitter) {
        if (emitter == null) {
            return;
        }
        try {
            emitter.complete();
        } catch (Exception ignore) {
            // ignore already closed or unusable response
        }
    }

    /**
     * 本机全用户会话发送消息
     *
     * @param message 要发送的消息内容
     */
    public void sendMessage(String message) {
        for (Long userId : USER_CHANNEL_EMITTERS.keySet()) {
            sendMessage(userId, message);
        }
    }

    /**
     * 发布SSE订阅消息
     *
     * @param sseMessageDto 要发布的SSE消息对象
     */
    public void publishMessage(SseMessageDto sseMessageDto) {
        SseMessageDto broadcastMessage = new SseMessageDto();
        broadcastMessage.setMessage(sseMessageDto.getMessage());
        broadcastMessage.setUserIds(sseMessageDto.getUserIds());
        RedisUtils.publish(SSE_TOPIC, broadcastMessage, consumer -> {
            log.info("SSE发送主题订阅消息topic:{} session keys:{} message:{}",
                SSE_TOPIC, sseMessageDto.getUserIds(), sseMessageDto.getMessage());
        });
    }

    /**
     * 向所有的用户发布订阅的消息(群发)
     *
     * @param message 要发布的消息内容
     */
    public void publishAll(String message) {
        SseMessageDto broadcastMessage = new SseMessageDto();
        broadcastMessage.setMessage(message);
        RedisUtils.publish(SSE_TOPIC, broadcastMessage, consumer -> {
            log.info("SSE发送主题订阅消息topic:{} message:{}", SSE_TOPIC, message);
        });
    }


    /**
     * 向指定的 SseEmitter 发送结构化事件（不经过 USER_TOKEN_EMITTERS）。
     */
    public void sendEvent(SseEmitter emitter, SseEventDto eventDto) {
        if (emitter == null) {
            return;
        }
        try {
            emitter.send(SseEmitter.event()
                .name(eventDto.getEvent())
                .data(JSONUtil.toJsonStr(eventDto)));
        } catch (Exception e) {
            log.debug("【SSE发送失败】请求级连接, error: {}", e.getMessage());
            safeComplete(emitter);
        }
    }

    /**
     * 完成指定的 SseEmitter（不经过 USER_TOKEN_EMITTERS）。
     */
    public void complete(SseEmitter emitter) {
        safeComplete(emitter);
    }
}
