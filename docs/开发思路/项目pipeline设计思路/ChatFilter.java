package org.ruoyi.service.chat.pipeline;

@FunctionalInterface
public interface ChatFilter {
    void doFilter(ChatContext ctx, ChatFilterChain chain) throws Exception;
}
