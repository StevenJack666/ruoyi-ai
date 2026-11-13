package org.ruoyi.common.sse.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 消息的dto
 *
 * @author zendwang
 */
@Data
public class SseMessageDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 需要推送到的session key 列表
     */
    private List<Long> userIds;

    /**
     * 需要发送的消息
     */
    private String message;

    /**
     * 兼容 main 分支的会话字段。
     * 没有 channelKey 时按该字段路由到会话通道。
     */
    private String sessionId;

    /**
     * 精确 SSE 通道 key，格式：sessionId:streamId。
     * 非空时优先于 sessionId。
     */
    private String channelKey;

    /**
     * 结构化事件。
     */
    private SseEventDto eventDto;
}
