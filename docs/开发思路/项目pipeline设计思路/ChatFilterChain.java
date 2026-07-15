package org.ruoyi.service.chat.pipeline;

import java.util.List;
import java.util.Iterator;

public class ChatFilterChain {
    private final Iterator<ChatFilter> iterator;

    public ChatFilterChain(List<ChatFilter> filters) {
        this.iterator = filters.iterator();
    }

    public void doFilter(ChatContext ctx) throws Exception {
        if (iterator.hasNext()) {
            iterator.next().doFilter(ctx, this);
        }
    }
}
