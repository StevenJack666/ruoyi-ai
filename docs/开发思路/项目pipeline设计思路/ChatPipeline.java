package org.ruoyi.service.chat.pipeline;

import lombok.extern.slf4j.Slf4j;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ChatPipeline {
    private final List<ChatFilter> filters = new ArrayList<>();

    public ChatPipeline addFilter(ChatFilter filter) {
        filters.add(filter);
        return this;
    }

    public boolean execute(ChatContext ctx) {
        try {
            new ChatFilterChain(filters).doFilter(ctx);
            return !ctx.isBlocked();
        } catch (Exception e) {
            log.error("管道执行异常", e);
            ctx.setBlocked(true);
            ctx.setBlockReason("系统错误: " + e.getMessage());
            return false;
        }
    }
}
