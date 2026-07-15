package org.ruoyi.service.chat.pipeline.filter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.service.chat.pipeline.ChatContext;
import org.ruoyi.service.chat.pipeline.ChatFilter;
import org.ruoyi.service.chat.pipeline.ChatFilterChain;
import org.ruoyi.service.chat.impl.ChatServiceFacade;

@Slf4j
@RequiredArgsConstructor
public class StreamingChatFilter implements ChatFilter {

    private final ChatServiceFacade chatService;

    @Override
    public void doFilter(ChatContext ctx, ChatFilterChain chain) throws Exception {
        log.info("AI 流式生成开始: userId={}", ctx.getUserId());
        // 调用现有的 sseChat 方法
        // 如果不需要继续后续 filter，可以不调用 chain.doFilter()
        chain.doFilter(ctx);
    }
}
