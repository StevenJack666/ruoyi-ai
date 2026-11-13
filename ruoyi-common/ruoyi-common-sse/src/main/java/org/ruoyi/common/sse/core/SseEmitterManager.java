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
     * Redis 订阅频道
     */
    private static final String SSE_TOPIC = "global:sse";

    /**
     * 会话/流维度：
     * key = channelKey = sessionId:streamId
     * 同一会话可以有多个连接，对应多个浏览器/多个并发请求。
     */
    private static final Map<String, SseEmitter> SESSION_EMITTERS = new ConcurrentHashMap<>();

    /**
     * 用户维度：全局通知、站内信等。
     * key = userId
     * value = token -> SseEmitter
     */
    private static final Map<Long, Map<String, SseEmitter>> USER_TOKEN_EMITTERS = new ConcurrentHashMap<>();

    public SseEmitterManager() {
        SpringUtils.getBean(ScheduledExecutorService.class)
            .scheduleWithFixedDelay(this::sseMonitor, 60L, 60L, TimeUnit.SECONDS);
    }

    // ======================== 会话/流维度 ========================

    /**
     * 生成 SSE 通道 key。
     * sessionId=1001, streamId=abc => "1001:abc"
     */
    public static String streamKey(Long sessionId, String streamId) {
        if (sessionId == null) {
            return null;
        }
        if (streamId == null || streamId.isEmpty()) {
            return String.valueOf(sessionId);
        }
        return sessionId + ":" + streamId;
    }

    /**
     * 建立会话/流 SSE 连接。
     * 同一个 channelKey 重复建连会替换旧连接。
     */
    public SseEmitter connect(String channelKey) {
        if (channelKey == null) {
            throw new IllegalArgumentException("channelKey 不能为空");
        }

        SseEmitter oldEmitter = SESSION_EMITTERS.remove(channelKey);
        if (oldEmitter != null) {
            safeComplete(oldEmitter);
        }

        SseEmitter emitter = new SseEmitter(86400000L);
        SESSION_EMITTERS.put(channelKey, emitter);

        emitter.onCompletion(() -> removeChannelEmitter(channelKey, emitter));
        emitter.onTimeout(() -> removeChannelEmitter(channelKey, emitter));
        emitter.onError(e -> removeChannelEmitter(channelKey, emitter));

        try {
            emitter.send(SseEmitter.event().comment("connected"));
        } catch (IOException e) {
            SESSION_EMITTERS.remove(channelKey, emitter);
        }
        return emitter;
    }

    /**
     * 断开指定 channelKey 的 SSE 连接。
     */
    public void disconnect(String channelKey) {
        if (channelKey == null) {
            return;
        }
        SseEmitter emitter = SESSION_EMITTERS.remove(channelKey);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event().comment("disconnected"));
            } catch (Exception exception) {
                log.error("SSE disconnect 失败 channelKey: {}", channelKey, exception);
            } finally {
                safeComplete(emitter);
            }
        }
    }

    /**
     * 向指定 channelKey 发送结构化事件（仅本机）。
     */
    public void sendEvent(String channelKey, SseEventDto eventDto) {
        if (!sendLocalEvent(channelKey, eventDto)) {
            log.warn("【SSE发送失败】channelKey: {} 没有活跃连接, 当前连接数: {}",
                channelKey, SESSION_EMITTERS.size());
        }
    }

    /**
     * 仅本机投递。
     * 返回 true 表示本机存在该 channelKey 的连接并已处理。
     * done=true 的事件发送成功后立即移除并关闭连接。
     */
    public boolean sendLocalEvent(String channelKey, SseEventDto eventDto) {
        if (channelKey == null || eventDto == null) {
            return false;
        }

        SseEmitter emitter = SESSION_EMITTERS.get(channelKey);
        if (emitter == null) {
            return false;
        }

        try {
            synchronized (emitter) {
                if (SESSION_EMITTERS.get(channelKey) != emitter) {
                    return false;
                }

                emitter.send(SseEmitter.event()
                    .name(eventDto.getEvent())
                    .data(JSONUtil.toJsonStr(eventDto)));

                if (Boolean.TRUE.equals(eventDto.getDone())) {
                    removeChannelEmitter(channelKey, emitter);
                }
            }
        } catch (Exception e) {
            log.error("【SSE发送失败】channelKey: {}, event: {}",
                channelKey, eventDto.getEvent(), e);
            removeChannelEmitter(channelKey, emitter);
        }
        return true;
    }

    /**
     * 向 channelKey 发送纯文本消息（仅本机）。
     */
    public void sendMessage(String channelKey, String message) {
        sendEvent(channelKey, SseEventDto.content(message));
    }

    private void removeChannelEmitter(String channelKey, SseEmitter emitter) {
        if (SESSION_EMITTERS.remove(channelKey, emitter)) {
            safeComplete(emitter);
        }
    }

    // ======================== 用户维度 ========================

    /**
     * 建立用户级 SSE 连接。
     */
    public SseEmitter connect(Long userId, String token) {
        Map<String, SseEmitter> emitters = USER_TOKEN_EMITTERS.computeIfAbsent(
            userId, k -> new ConcurrentHashMap<>()
        );

        SseEmitter oldEmitter = emitters.remove(token);
        if (oldEmitter != null) {
            safeComplete(oldEmitter);
        }

        SseEmitter emitter = new SseEmitter(86400000L);
        emitters.put(token, emitter);

        emitter.onCompletion(() -> removeUserEmitter(userId, token));
        emitter.onTimeout(() -> removeUserEmitter(userId, token));
        emitter.onError(e -> {
            log.error("SSE连接异常 userId: {} token: {}", userId, token, e);
            removeUserEmitter(userId, token);
        });

        try {
            emitter.send(SseEmitter.event().comment("connected"));
        } catch (IOException e) {
            removeUserEmitter(userId, token);
        }
        return emitter;
    }

    /**
     * 断开用户级 SSE 连接。
     */
    public void disconnect(Long userId, String token) {
        if (userId == null || token == null) {
            return;
        }

        Map<String, SseEmitter> emitters = USER_TOKEN_EMITTERS.get(userId);
        if (MapUtil.isNotEmpty(emitters)) {
            SseEmitter emitter = emitters.remove(token);
            if (emitter != null) {
                try {
                    emitter.send(SseEmitter.event().comment("disconnected"));
                } catch (Exception e) {
                    log.error("SSE disconnect 失败 userId: {} token: {}", userId, token, e);
                } finally {
                    safeComplete(emitter);
                }
            }
            if (emitters.isEmpty()) {
                USER_TOKEN_EMITTERS.remove(userId);
            }
        }
    }

    private void removeUserEmitter(Long userId, String token) {
        Map<String, SseEmitter> emitters = USER_TOKEN_EMITTERS.get(userId);
        if (MapUtil.isEmpty(emitters)) {
            USER_TOKEN_EMITTERS.remove(userId);
            return;
        }

        SseEmitter emitter = emitters.remove(token);
        if (emitter != null) {
            safeComplete(emitter);
        }
        if (emitters.isEmpty()) {
            USER_TOKEN_EMITTERS.remove(userId);
        }
    }

    /**
     * SSE 心跳检测，清理无效连接。
     */
    public void sseMonitor() {
        final SseEmitter.SseEventBuilder heartbeat = SseEmitter.event().comment("heartbeat");

        List<Long> toRemoveUsers = new ArrayList<>();
        USER_TOKEN_EMITTERS.forEach((userId, emitterMap) -> {
            if (CollUtil.isEmpty(emitterMap)) {
                toRemoveUsers.add(userId);
                return;
            }

            emitterMap.entrySet().removeIf(entry -> {
                try {
                    entry.getValue().send(heartbeat);
                    return false;
                } catch (Exception ex) {
                    safeComplete(entry.getValue());
                    return true;
                }
            });

            if (emitterMap.isEmpty()) {
                toRemoveUsers.add(userId);
            }
        });
        toRemoveUsers.forEach(USER_TOKEN_EMITTERS::remove);

        SESSION_EMITTERS.entrySet().removeIf(entry -> {
            try {
                entry.getValue().send(heartbeat);
                return false;
            } catch (Exception ex) {
                safeComplete(entry.getValue());
                return true;
            }
        });
    }

    /**
     * 订阅 SSE Redis 主题。
     */
    public void subscribeMessage(Consumer<SseMessageDto> consumer) {
        RedisUtils.subscribe(SSE_TOPIC, SseMessageDto.class, consumer);
    }

    /**
     * 向指定用户的所有连接发送消息。
     */
    public void sendMessage(Long userId, String message) {
        Map<String, SseEmitter> emitters = USER_TOKEN_EMITTERS.get(userId);
        if (MapUtil.isNotEmpty(emitters)) {
            for (Map.Entry<String, SseEmitter> entry : emitters.entrySet()) {
                try {
                    SseEventDto eventDto = SseEventDto.content(message);
                    entry.getValue().send(SseEmitter.event()
                        .name("message")
                        .data(JSONUtil.toJsonStr(eventDto)));
                } catch (Exception e) {
                    SseEmitter remove = emitters.remove(entry.getKey());
                    safeComplete(remove);
                }
            }
            if (emitters.isEmpty()) {
                USER_TOKEN_EMITTERS.remove(userId);
            }
        } else {
            USER_TOKEN_EMITTERS.remove(userId);
        }
    }

    /**
     * 向指定用户的所有连接发送结构化事件。
     */
    public void sendEvent(Long userId, SseEventDto eventDto) {
        Map<String, SseEmitter> emitters = USER_TOKEN_EMITTERS.get(userId);
        if (MapUtil.isNotEmpty(emitters)) {
            for (Map.Entry<String, SseEmitter> entry : emitters.entrySet()) {
                try {
                    entry.getValue().send(SseEmitter.event()
                        .name(eventDto.getEvent())
                        .data(JSONUtil.toJsonStr(eventDto)));
                } catch (Exception e) {
                    log.error("【SSE发送失败】userId: {}, token: {}", userId, entry.getKey(), e);
                    SseEmitter remove = emitters.remove(entry.getKey());
                    safeComplete(remove);
                }
            }
            if (emitters.isEmpty()) {
                USER_TOKEN_EMITTERS.remove(userId);
            }
        } else {
            log.warn("【SSE发送失败】userId: {} 没有活跃的SSE连接, 当前连接用户: {}",
                userId, USER_TOKEN_EMITTERS.keySet());
            USER_TOKEN_EMITTERS.remove(userId);
        }
    }

    // ======================== 兼容旧调用 ========================

    /**
     * @deprecated 会话已改为 channelKey 注册，请使用 {@link #connect(String)}
     */
    @Deprecated
    public SseEmitter connectSession(Long userId, Long sessionId) {
        if (userId == null || sessionId == null) {
            return null;
        }
        return connect(String.valueOf(sessionId));
    }

    @Deprecated
    public void disconnectSession(Long userId, Long sessionId) {
        if (userId == null || sessionId == null) {
            return;
        }
        disconnect(String.valueOf(sessionId));
    }

    public void sendMessage(Long userId, Long sessionId, String message) {
        if (sessionId == null) {
            sendMessage(userId, message);
            return;
        }
        sendMessage(String.valueOf(sessionId), message);
    }

    public void sendEvent(Long userId, Long sessionId, SseEventDto eventDto) {
        if (sessionId == null) {
            sendEvent(userId, eventDto);
            return;
        }
        sendEvent(String.valueOf(sessionId), eventDto);
    }

    /**
     * 本机全用户发送消息。
     */
    public void sendMessage(String message) {
        for (Long userId : USER_TOKEN_EMITTERS.keySet()) {
            sendMessage(userId, message);
        }
    }

    /**
     * 发布 SSE 订阅消息。
     */
    public void publishMessage(SseMessageDto sseMessageDto) {
        SseMessageDto broadcastMessage = new SseMessageDto();
        broadcastMessage.setMessage(sseMessageDto.getMessage());
        broadcastMessage.setUserIds(sseMessageDto.getUserIds());
        broadcastMessage.setSessionId(sseMessageDto.getSessionId());
        broadcastMessage.setChannelKey(sseMessageDto.getChannelKey());
        broadcastMessage.setEventDto(sseMessageDto.getEventDto());

        RedisUtils.publish(SSE_TOPIC, broadcastMessage, consumer -> {
            log.info("SSE发送主题订阅消息 topic:{} channelKey:{} sessionId:{} userIds:{} event:{}",
                SSE_TOPIC,
                sseMessageDto.getChannelKey(),
                sseMessageDto.getSessionId(),
                sseMessageDto.getUserIds(),
                sseMessageDto.getEventDto() == null
                    ? null
                    : sseMessageDto.getEventDto().getEvent());
        });
    }

    /**
     * 向所有用户发布消息（群发）。
     */
    public void publishAll(String message) {
        SseMessageDto broadcastMessage = new SseMessageDto();
        broadcastMessage.setMessage(message);
        RedisUtils.publish(SSE_TOPIC, broadcastMessage, consumer -> {
            log.info("SSE发送主题订阅消息 topic:{} message:{}", SSE_TOPIC, message);
        });
    }

    /**
     * 向请求级 SseEmitter 发送结构化事件。
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

    public void complete(SseEmitter emitter) {
        safeComplete(emitter);
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
}
