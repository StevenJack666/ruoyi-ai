package org.ruoyi.service.chat;

import cn.hutool.core.collection.CollUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.redis.utils.RedisUtils;
import org.ruoyi.factory.ResourceLoaderFactory;
import org.ruoyi.service.knowledge.ResourceLoader;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Duration;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class FileParseAsyncService {

    private final ResourceLoaderFactory resourceLoaderFactory;
    // Redis上传文件的前缀Key
    private final String UPLOAD_REDIS_PREFIX_KEY = "session:docs:oss:";

    /**
     * 异步执行文档解析与缓存
     */
    @Async
    public void asyncParseAndCache(Long ossId, String ext, byte[] fileBytes) {
        try {
            ResourceLoader loader = resourceLoaderFactory.getLoaderByFileType(ext);
            String text;
            try (InputStream is = new ByteArrayInputStream(fileBytes)) {
                text = loader.getContent(is);
            }

            List<String> chunks = loader.getChunkList(text, null);
            if (CollUtil.isEmpty(chunks)) {
                log.warn("会话文档分块为空，跳过缓存: ossId={}", ossId);
                return;
            }

            String cacheKey = UPLOAD_REDIS_PREFIX_KEY + ossId;
            RedisUtils.deleteObject(cacheKey);
            RedisUtils.setCacheList(cacheKey, chunks);
            RedisUtils.expire(cacheKey, Duration.ofMinutes(30));
            log.info("文档异步解析并缓存成功: ossId={}, chunks={}", ossId, chunks.size());
        } catch (Exception e) {
            // 异步任务中的异常不会抛给前端，必须在这里记录日志
            log.error("异步解析文档失败: ossId={}", ossId, e);
        }
    }
}
