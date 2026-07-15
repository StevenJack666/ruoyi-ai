package org.ruoyi.service.chat.pipeline.filter;

import lombok.extern.slf4j.Slf4j;
import org.ruoyi.service.chat.pipeline.ChatContext;
import org.ruoyi.service.chat.pipeline.ChatFilter;
import org.ruoyi.service.chat.pipeline.ChatFilterChain;
// 内容审核
@Slf4j
public class ContentModerationFilter implements ChatFilter {

    private final java.util.List<String> violationKeywords;

    public ContentModerationFilter() {
        this.violationKeywords = java.util.List.of("违规词1", "违规词2");
    }

    public ContentModerationFilter(java.util.List<String> keywords) {
        this.violationKeywords = keywords;
    }

    @Override
    public void doFilter(ChatContext ctx, ChatFilterChain chain) throws Exception {
        String content = getContent(ctx);
        if (content != null && containsViolation(content)) {
            ctx.setBlocked(true);
            ctx.setBlockReason("内容包含违规词汇，已拦截");
            log.warn("内容违规拦截: userId={}", ctx.getUserId());
            return;
        }
        chain.doFilter(ctx);
    }

    private String getContent(ChatContext ctx) throws Exception {
        if (ctx.getChatRequest() == null) return null;
        var m = ctx.getChatRequest().getClass().getMethod("getContent");
        return (String) m.invoke(ctx.getChatRequest());
    }

    private boolean containsViolation(String text) {
        for (String kw : violationKeywords) {
            if (text.contains(kw)) return true;
        }
        return false;
    }
}
