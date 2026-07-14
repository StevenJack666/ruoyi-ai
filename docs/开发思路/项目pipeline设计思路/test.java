//使用方式介绍
public SseEmitter sseChat(ChatRequest chatRequest) {

    // ... 原有 userId、token、模型配置等逻辑 ...

    // 组装管道
    ChatPipeline pipeline = new ChatPipeline()
        .addFilter(new ContentModerationFilter())
        .addFilter(new RagRetrievalFilter(knowledgeRetrievalService, kid, embeddingModel))
        .addFilter(new StreamingChatFilter(this));

    ChatContext ctx = ChatContext.builder()
        .userId(userId)
        .tokenValue(tokenValue)
        .chatRequest(chatRequest)
        .build();

    boolean passed = pipeline.execute(ctx);
    if (!passed) {
        SseMessageUtils.sendError(userId, ctx.getBlockReason());
        return emitter;  // 阻断，不调 AI
    }

    // 继续原有 AI 调用逻辑...
}
