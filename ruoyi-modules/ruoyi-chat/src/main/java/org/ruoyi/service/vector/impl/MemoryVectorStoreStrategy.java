package org.ruoyi.service.vector.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.chat.service.chat.IChatModelService;
import org.ruoyi.config.VectorStoreProperties;
import org.ruoyi.domain.bo.vector.QueryVectorBo;
import org.ruoyi.domain.bo.vector.StoreEmbeddingBo;
import org.ruoyi.domain.entity.knowledge.KnowledgeAttach;
import org.ruoyi.domain.entity.knowledge.KnowledgeFragment;
import org.ruoyi.domain.vo.knowledge.KnowledgeRetrievalVo;
import org.ruoyi.factory.EmbeddingModelFactory;
import org.ruoyi.mapper.knowledge.KnowledgeAttachMapper;
import org.ruoyi.mapper.knowledge.KnowledgeFragmentMapper;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 内存 + MySQL 向量库策略
 * <p>
 * 向量数据持久化到 MySQL，首次检索时加载到 JVM 内存。
 * 后续检索直接在内存中暴力余弦相似度计算，不再查询数据库。
 * 增删改时自动清除对应知识库的内存缓存，下次检索重新加载。
 * 不依赖任何外部向量数据库。
 *
 * @author ruoyi team
 */
@Slf4j
@Component("memory")
public class MemoryVectorStoreStrategy extends AbstractVectorStoreStrategy {

    private final KnowledgeFragmentMapper fragmentMapper;
    private final KnowledgeAttachMapper attachMapper;

    /** 内存缓存: kid -> 该知识库的所有向量数据，30分钟无操作后自动过期 */
    private final Cache<String, List<CachedVector>> vectorCache = Caffeine.newBuilder()
        .expireAfterAccess(30, java.util.concurrent.TimeUnit.MINUTES)
        .recordStats()
        .build();

    public MemoryVectorStoreStrategy(VectorStoreProperties vectorStoreProperties,
                                     IChatModelService chatModelService,
                                     EmbeddingModelFactory embeddingModelFactory,
                                     KnowledgeFragmentMapper fragmentMapper,
                                     KnowledgeAttachMapper attachMapper) {
        super(vectorStoreProperties, embeddingModelFactory, chatModelService);
        this.fragmentMapper = fragmentMapper;
        this.attachMapper = attachMapper;
    }

    @Override
    public String getVectorStoreType() {
        return "memory";
    }

    @Override
    public void createSchema(String kid, String modelName) {
        log.info("内存向量库 schema 已就绪: kid={}, model={}", kid, modelName);
    }

    // ==================== 存储 ====================

    @Override
    public void storeEmbeddings(StoreEmbeddingBo storeEmbeddingBo) {
        List<String> chunkList = storeEmbeddingBo.getChunkList();
        String docId = storeEmbeddingBo.getDocId();
        String kid = storeEmbeddingBo.getKid();
        String modelName = storeEmbeddingBo.getEmbeddingModelName();

        EmbeddingModel embeddingModel = getEmbeddingModel(modelName);
        log.info("内存向量库存储: kid={}, docId={}, chunks={}", kid, docId, chunkList.size());

        long start = System.currentTimeMillis();

        IntStream.range(0, chunkList.size()).parallel().forEach(i -> {
            String text = chunkList.get(i);

            Embedding embedding = embeddingModel.embed(text).content();
            float[] vector = embedding.vector();
            normalize(vector);

            String vectorStr = vectorToString(vector);

            KnowledgeFragment update = new KnowledgeFragment();
            update.setVector(vectorStr);
            fragmentMapper.update(update, Wrappers.<KnowledgeFragment>lambdaUpdate()
                .eq(KnowledgeFragment::getDocId, docId)
                .eq(KnowledgeFragment::getIdx, i));
        });

        // 清除缓存，下次检索重新加载
        vectorCache.invalidate(kid);
        log.info("内存向量库存储完成: kid={}, docId={}, 缓存已清除, 耗时={}ms",
            kid, docId, System.currentTimeMillis() - start);
    }

    // ==================== 检索 ====================

    @Override
    public List<String> getQueryVector(QueryVectorBo queryVectorBo) {
        return search(queryVectorBo).stream()
            .map(KnowledgeRetrievalVo::getContent)
            .collect(Collectors.toList());
    }

    @Override
    public List<KnowledgeRetrievalVo> search(QueryVectorBo queryVectorBo) {
        String kid = queryVectorBo.getKid();
        String query = queryVectorBo.getQuery();
        int maxResults = queryVectorBo.getMaxResults() != null ? queryVectorBo.getMaxResults() : 10;
        String modelName = queryVectorBo.getEmbeddingModelName();

        long start = System.currentTimeMillis();

        // 1. 从内存缓存获取向量数据（缓存未命中则从 MySQL 加载）
        List<CachedVector> cached = vectorCache.get(kid, this::loadFromDatabase);
        if (cached.isEmpty()) {
            log.warn("知识库无向量数据: kid={}", kid);
            return new ArrayList<>();
        }

        // 2. 生成查询向量
        EmbeddingModel embeddingModel = getEmbeddingModel(modelName);
        Embedding queryEmbedding = embeddingModel.embed(query).content();
        float[] queryVector = queryEmbedding.vector();
        normalize(queryVector);

        // 3. 内存中暴力计算余弦相似度
        List<ScoredResult> scored = new ArrayList<>();
        for (CachedVector cv : cached) {
            double score = cosineSimilarity(queryVector, cv.vector);
            scored.add(new ScoredResult(cv, score));
        }

        // 4. 排序取 top-k
        scored.sort((a, b) -> Double.compare(b.score, a.score));
        if (scored.size() > maxResults) {
            scored = scored.subList(0, maxResults);
        }

        // 5. 组装结果
        List<KnowledgeRetrievalVo> results = scored.stream().map(s -> {
            KnowledgeRetrievalVo vo = new KnowledgeRetrievalVo();
            vo.setId(s.cv.id);
            vo.setContent(s.cv.content);
            vo.setDocId(s.cv.docId);
            vo.setIdx(s.cv.idx);
            vo.setKnowledgeId(s.cv.knowledgeId);
            vo.setScore(s.score);
            vo.setSourceName(s.cv.sourceName);
            return vo;
        }).collect(Collectors.toList());

        log.info("内存向量库检索完成: kid={}, query={}, 缓存={}, 返回={}, 耗时={}ms",
            kid, query, cached.size(), results.size(), System.currentTimeMillis() - start);

        return results;
    }

    // ==================== 内存缓存管理 ====================

    /**
     * 从 MySQL 加载该知识库的所有向量数据到内存
     */
    private List<CachedVector> loadFromDatabase(String kid) {
        long start = System.currentTimeMillis();

        // 1. 查询所有有向量的片段
        List<KnowledgeFragment> fragments = fragmentMapper.selectList(
            Wrappers.<KnowledgeFragment>lambdaQuery()
                .eq(KnowledgeFragment::getKnowledgeId, kid)
                .isNotNull(KnowledgeFragment::getVector));

        if (fragments.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 预加载来源名称
        List<String> docIds = fragments.stream()
            .map(KnowledgeFragment::getDocId)
            .distinct()
            .collect(Collectors.toList());

        Map<String, String> sourceNameMap = new HashMap<>();
        for (String docId : docIds) {
            KnowledgeAttach attach = attachMapper.selectOne(
                Wrappers.<KnowledgeAttach>lambdaQuery()
                    .eq(KnowledgeAttach::getDocId, docId)
                    .last("LIMIT 1"));
            sourceNameMap.put(docId, attach != null ? attach.getName() : "未知来源");
        }

        // 3. 解码向量并组装缓存对象
        List<CachedVector> list = new ArrayList<>();
        for (KnowledgeFragment f : fragments) {
            float[] vec = decodeVector(f.getVector());
            if (vec == null) continue;
            normalize(vec);
            list.add(new CachedVector(
                f.getId().toString(),
                f.getContent(),
                f.getDocId(),
                f.getIdx(),
                f.getKnowledgeId(),
                vec,
                sourceNameMap.getOrDefault(f.getDocId(), "未知来源")
            ));
        }

        log.info("内存向量库加载: kid={}, 加载={}条, 耗时={}ms",
            kid, list.size(), System.currentTimeMillis() - start);
        return list;
    }

    // ==================== 删除 ====================

    @Override
    public void removeById(String id, String modelName) {
        fragmentMapper.deleteById(id);
        // 删除后清除所有缓存
        vectorCache.invalidateAll();
        log.info("内存向量库删除: id={}", id);
    }

    @Override
    public void removeByDocId(String docId, String kid) {
        fragmentMapper.delete(Wrappers.<KnowledgeFragment>lambdaQuery()
            .eq(KnowledgeFragment::getDocId, docId));
        vectorCache.invalidate(kid);
        log.info("内存向量库删除: docId={}", docId);
    }

    @Override
    public void removeByFid(String fid, String kid) {
        fragmentMapper.deleteById(fid);
        vectorCache.invalidate(kid);
        log.info("内存向量库删除: fid={}", fid);
    }

    // ==================== 工具方法 ====================

    private float[] decodeVector(String vectorStr) {
        if (vectorStr == null || vectorStr.isEmpty()) return null;
        String[] parts = vectorStr.split(",");
        float[] result = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = Float.parseFloat(parts[i].trim());
        }
        return result;
    }

    private String vectorToString(float[] vector) {
        StringBuilder sb = new StringBuilder();
        for (int j = 0; j < vector.length; j++) {
            if (j > 0) sb.append(",");
            sb.append(vector[j]);
        }
        return sb.toString();
    }

    private double cosineSimilarity(float[] a, float[] b) {
        if (a == null || b == null || a.length != b.length) return 0;
        double dot = 0, n1 = 0, n2 = 0;
        for (int i = 0; i < a.length; i++) {
            dot += (double) a[i] * b[i];
            n1 += (double) a[i] * a[i];
            n2 += (double) b[i] * b[i];
        }
        double denom = Math.sqrt(n1) * Math.sqrt(n2);
        return denom == 0 ? 0 : dot / denom;
    }

    /** 内存缓存中的向量数据 */
    private record CachedVector(String id, String content, String docId, Integer idx,
                                Long knowledgeId, float[] vector, String sourceName) {}

    /** 排序后的检索结果 */
    private record ScoredResult(CachedVector cv, double score) {}
}
