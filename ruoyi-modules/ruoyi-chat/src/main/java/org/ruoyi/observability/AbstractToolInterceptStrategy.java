package org.ruoyi.observability;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.sse.dto.SseEventDto;
import org.ruoyi.common.sse.utils.SseMessageUtils;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public abstract class AbstractToolInterceptStrategy implements ToolInterceptStrategy{

    protected static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    protected static void pushMcpEvent(String name, String event, String status, Long userId, Object result) {
        if (userId == null) {
            log.warn("userId 为空，无法推送 MCP 事件");
            return;
        }
        try {
            Map<String, Object> content = new HashMap<>();
            content.put("name", name);
            content.put("status", status);
            content.put("result", result);
            content.put("userId", userId);

            String json = OBJECT_MAPPER.writeValueAsString(content);
            SseMessageUtils.sendEvent(userId, SseEventDto.builder()
                .event(event)
                .content(json)
                .build());
        } catch (JsonProcessingException e) {
            log.error("序列化 MCP 事件失败: {}", e.getMessage());
        }
    }
}
