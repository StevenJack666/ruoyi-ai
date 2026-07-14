package org.ruoyi.service.chat.pipeline.filter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.service.chat.pipeline.ChatContext;
import org.ruoyi.service.chat.pipeline.ChatFilter;
import org.ruoyi.service.chat.pipeline.ChatFilterChain;
import org.ruoyi.service.retrieval.KnowledgeRetrievalService;
import org.ruoyi.domain.bo.vector.QueryVectorBo;

@Slf4j
@RequiredArgsConstructor
public class RagRetrievalFilter implements ChatFilter {

    private final KnowledgeRetrievalService retrievalService;
    private final String knowledgeId;
    private final String embeddingModelName;

    @Override
    public void doFilter(ChatContext ctx, ChatFilterChain chain) throws Exception {
        String query = getQuery(ctx);
        if (query == null || knowledgeId == null) {
            chain.doFilter(ctx);
            return;
        }

        QueryVectorBo bo = new QueryVectorBo();
        bo.setKid(knowledgeId);
        bo.setQuery(query);
        bo.setEmbeddingModelName(embeddingModelName);

        java.util.List<String> docs = retrievalService.retrieveTexts(bo);
        ctx.setRetrievedDocs(docs);
        log.info("RAG 检索完成: kid={}, 命中={}条", knowledgeId, docs.size());

        chain.doFilter(ctx);
    }

    private String getQuery(ChatContext ctx) throws Exception {
        if (ctx.getChatRequest() == null) return null;
        var m = ctx.getChatRequest().getClass().getMethod("getContent");
        return (String) m.invoke(ctx.getChatRequest());
    }
}
