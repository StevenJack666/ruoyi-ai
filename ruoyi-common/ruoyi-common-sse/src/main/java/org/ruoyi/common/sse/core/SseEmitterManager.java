package org.ruoyi.common.sse.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.ruoyi.common.bus.domain.BusMessageHistory;
import org.ruoyi.common.bus.service.IBusMessageService;
import org.ruoyi.common.core.utils.SpringUtils;
import org.ruoyi.common.json.utils.JsonUtils;
import org.ruoyi.common.sse.dto.SseMessageDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.IdUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 管理 Server-Sent Events (SSE) 连接
 *
 * @author Lion Li
 */
@Slf4j
public class SseEmitterManager {

    private final static Map<Long, Map<String, SseEmitter>> USER_TOKEN_EMITTERS = new ConcurrentHashMap<>();

    @Autowired
    private IBusMessageService busMessageService;

    public SseEmitterManager() {
        // 定时执行 SSE 心跳检测
        SpringUtils.getBean(ScheduledExecutorService.class)
            .scheduleWithFixedDelay(this::sseMonitor, 60L, 60L, TimeUnit.SECONDS);
    }

    /**
     * 建立与指定用户的 SSE 连接
     *
     * @param userId 用户的唯一标识符，用于区分不同用户的连接
     * @param token  用户的唯一令牌，用于识别具体的连接
     * @return 返回一个 SseEmitter 实例，客户端可以通过该实例接收 SSE 事件
     */
    public SseEmitter connect(Long userId, String token) {
        // 从 USER_TOKEN_EMITTERS 中获取或创建当前用户的 SseEmitter 映射表（ConcurrentHashMap）
        // 每个用户可以有多个 SSE 连接，通过 token 进行区分
        Map<String, SseEmitter> emitters = USER_TOKEN_EMITTERS.computeIfAbsent(userId, k -> new ConcurrentHashMap<>());

        // 关闭已存在的SseEmitter，防止超过最大连接数
        SseEmitter oldEmitter = emitters.remove(token);
        if (oldEmitter != null) {
            oldEmitter.complete();
        }

        // 创建一个新的 SseEmitter 实例，超时时间设置为一天 避免连接之后直接关闭浏览器导致连接停滞
        SseEmitter emitter = new SseEmitter(86400000L);

        emitters.put(token, emitter);

        // 当 emitter 完成、超时或发生错误时，从映射表中移除对应的 token
        emitter.onCompletion(() -> {
            SseEmitter remove = emitters.remove(token);
            if (remove != null) {
//                remove.complete();
            }
        });
        emitter.onTimeout(() -> {
            SseEmitter remove = emitters.remove(token);
            if (remove != null) {
                remove.complete();
            }
        });
        emitter.onError((e) -> {
            SseEmitter remove = emitters.remove(token);
            if (remove != null) {
                remove.complete();
            }
        });

        try {
            // 向客户端发送一条连接成功的事件
            emitter.send(SseEmitter.event().comment("connected"));
            replayUnreadMessages(userId, emitter);
        } catch (IOException e) {
            // 如果发送消息失败，则从映射表中移除 emitter
            emitters.remove(token);
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
        if (userId == null || token == null) {
            return;
        }
        Map<String, SseEmitter> emitters = USER_TOKEN_EMITTERS.get(userId);
        if (MapUtil.isNotEmpty(emitters)) {
            try {
                SseEmitter sseEmitter = emitters.get(token);
                sseEmitter.send(SseEmitter.event().comment("disconnected"));
                //sseEmitter.complete();
            } catch (Exception exception) {
                log.error(exception.getMessage());
            }
            emitters.remove(token);
        } else {
            USER_TOKEN_EMITTERS.remove(userId);
        }
    }

    /**
     * SSE 心跳检测，关闭无效连接
     */
    public void sseMonitor() {
        final SseEmitter.SseEventBuilder heartbeat = SseEmitter.event().comment("heartbeat");
        // 记录需要移除的用户ID
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
        toRemoveUsers.forEach(USER_TOKEN_EMITTERS::remove);
    }

    /**
     * 向指定的用户会话发送消息
     *
     * @param userId  要发送消息的用户id
     * @param message 要发送的消息内容
     */
    public void sendMessage(Long userId, String message) {
        Map<String, SseEmitter> emitters = USER_TOKEN_EMITTERS.get(userId);
        if (MapUtil.isNotEmpty(emitters)) {
            for (Map.Entry<String, SseEmitter> entry : emitters.entrySet()) {
                try {
                    entry.getValue().send(SseEmitter.event()
                        .name("message")
                        .data(message));
                } catch (Exception e) {
                    SseEmitter remove = emitters.remove(entry.getKey());
                    if (remove != null) {
                        remove.complete();
                    }
                }
            }
        } else {
            USER_TOKEN_EMITTERS.remove(userId);
        }
    }

    /**
     * 向指定用户发送结构化消息(包含消息ID/时间戳)
     *
     * @return true=至少有一个连接发送成功
     */
    public boolean sendMessage(Long userId, SseMessageDto sseMessageDto) {
        Map<String, SseEmitter> emitters = USER_TOKEN_EMITTERS.get(userId);
        if (MapUtil.isEmpty(emitters)) {
            USER_TOKEN_EMITTERS.remove(userId);
            return false;
        }
        String payload = JsonUtils.toJsonString(sseMessageDto);
        boolean delivered = false;
        for (Map.Entry<String, SseEmitter> entry : emitters.entrySet()) {
            try {
                entry.getValue().send(SseEmitter.event()
                    .name("message")
                    .data(payload));
                delivered = true;
            } catch (Exception e) {
                SseEmitter remove = emitters.remove(entry.getKey());
                if (remove != null) {
                    remove.complete();
                }
            }
        }
        return delivered;
    }

    /**
     * 本机全用户会话发送消息
     *
     * @param message 要发送的消息内容
     */
    public void sendMessage(String message) {
        for (Long userId : USER_TOKEN_EMITTERS.keySet()) {
            sendMessage(userId, message);
        }
    }

    /**
     * 本机全用户会话发送结构化消息
     */
    public void sendMessage(SseMessageDto sseMessageDto) {
        for (Long userId : USER_TOKEN_EMITTERS.keySet()) {
            sendMessage(userId, sseMessageDto);
        }
    }

    /**
     * 分发消息: 在线实时推送，离线存储待回执
     */
    public void dispatchMessage(SseMessageDto sseMessageDto) {
        if (sseMessageDto == null) {
            return;
        }
        if (CollUtil.isNotEmpty(sseMessageDto.getUserIds())) {
            sseMessageDto.getUserIds().forEach(userId -> {
                busMessageService.saveMessage(userId, sseMessageDto.getMessageId(), sseMessageDto.getMessage(), sseMessageDto.getSendTime());
                sendMessage(userId, sseMessageDto);
            });
        } else {
            sendMessage(sseMessageDto);
        }
    }

    /**
     * 发布SSE订阅消息
     *
     * @param sseMessageDto 要发布的SSE消息对象
     */
    public void publishMessage(SseMessageDto sseMessageDto) {
        SseMessageDto broadcastMessage = new SseMessageDto();
        broadcastMessage.setMessageId(buildMessageId());
        broadcastMessage.setSendTime(System.currentTimeMillis());
        broadcastMessage.setMessage(sseMessageDto.getMessage());
        broadcastMessage.setUserIds(sseMessageDto.getUserIds());
        dispatchMessage(broadcastMessage);
        log.info("SSE发送本地消息 session keys:{} message:{}",
            sseMessageDto.getUserIds(), sseMessageDto.getMessage());
    }

    /**
     * 向所有的用户发布订阅的消息(群发)
     *
     * @param message 要发布的消息内容
     */
    public void publishAll(String message) {
        SseMessageDto broadcastMessage = new SseMessageDto();
        broadcastMessage.setMessageId(buildMessageId());
        broadcastMessage.setSendTime(System.currentTimeMillis());
        broadcastMessage.setMessage(message);
        broadcastMessage.setUserIds(new ArrayList<>(USER_TOKEN_EMITTERS.keySet()));
        dispatchMessage(broadcastMessage);
        log.info("SSE发送本机群发消息 message:{} online users:{}",
            message, broadcastMessage.getUserIds().size());
    }

    /**
     * 读取 bus 未读并转换为 SSE 事件格式后回放给当前连接。
     */
    private void replayUnreadMessages(Long userId, SseEmitter emitter) {
        List<BusMessageHistory> unreadMessages = busMessageService.listUnread(userId);
        if (CollUtil.isEmpty(unreadMessages)) {
            return;
        }
        unreadMessages.forEach(record -> {
            try {
                SseMessageDto message = new SseMessageDto();
                message.setMessageId(record.getMessageId());
                message.setMessage(record.getMessage());
                message.setSendTime(record.getSendTime());
                message.setUserIds(List.of(record.getUserId()));
                emitter.send(SseEmitter.event()
                    .name("message")
                    .data(JsonUtils.toJsonString(message)));
            } catch (Exception e) {
                log.warn("回放离线消息失败 userId={} messageId={}", userId, record.getMessageId(), e);
            }
        });
    }

    private String buildMessageId() {
        return IdUtil.fastSimpleUUID();
    }
}
