package org.ruoyi.common.sse.dto;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

import lombok.Data;

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
     * 消息唯一ID(用于客户端回执)
     */
    private String messageId;

    /**
     * 发送时间戳(毫秒)
     */
    private Long sendTime;

    /**
     * 需要发送的消息
     */
    private String message;
}
