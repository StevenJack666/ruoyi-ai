package org.ruoyi.common.sse.listener;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.sse.core.SseEmitterManager;
import org.ruoyi.common.sse.dto.SseEventDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;

/**
 * SSE 主题订阅监听器
 *
 * @author Lion Li
 */
@Slf4j
public class SseTopicListener implements ApplicationRunner, Ordered {

    @Autowired
    private SseEmitterManager sseEmitterManager;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        sseEmitterManager.subscribeMessage((message) -> {
            log.info("SSE主题订阅收到消息 channelKey={} sessionId={} userIds={}",
                message.getChannelKey(), message.getSessionId(), message.getUserIds());

            // 1. 精确 channelKey 路由（同一会话多窗口/多请求）
            if (StrUtil.isNotBlank(message.getChannelKey())) {
                if (message.getEventDto() != null) {
                    sseEmitterManager.sendLocalEvent(
                        message.getChannelKey(), message.getEventDto()
                    );
                } else if (message.getMessage() != null) {
                    sseEmitterManager.sendLocalEvent(
                        message.getChannelKey(),
                        SseEventDto.content(message.getMessage())
                    );
                }
                return;
            }

            // 2. 兼容 main 分支：按 sessionId 路由
            if (StrUtil.isNotBlank(message.getSessionId())) {
                if (message.getEventDto() != null) {
                    sseEmitterManager.sendLocalEvent(
                        message.getSessionId(), message.getEventDto()
                    );
                } else if (message.getMessage() != null) {
                    sseEmitterManager.sendLocalEvent(
                        message.getSessionId(),
                        SseEventDto.content(message.getMessage())
                    );
                }
                return;
            }

            // 3. 用户/群发路由
            if (CollUtil.isNotEmpty(message.getUserIds())) {
                message.getUserIds().forEach(key ->
                    sseEmitterManager.sendMessage(key, message.getMessage()));
            } else {
                sseEmitterManager.sendMessage(message.getMessage());
            }
        });
        log.info("初始化SSE主题订阅监听器成功");
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
