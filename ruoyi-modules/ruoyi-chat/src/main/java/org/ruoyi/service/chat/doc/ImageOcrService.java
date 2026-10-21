package org.ruoyi.service.chat.doc;

import cn.hutool.crypto.digest.DigestUtil;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.imageio.ImageIO;

import jakarta.annotation.Resource;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.core.utils.SpringUtils;
import org.ruoyi.common.redis.utils.RedisUtils;
import org.ruoyi.service.chat.impl.provider.ProviderImageDescriber;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * 图片 OCR（并发 + 缓存 + 压缩 + 小图跳过）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImageOcrService {

    /** 并发数：与 OCR 模型 RPM 匹配 */
    private static final int CONCURRENCY = 4;
    /** 小于该字节数视为装饰性小图，跳过 OCR */
    private static final int MIN_IMAGE_BYTES = 10 * 1024;
    /** 压缩后最大边长 */
    private static final int MAX_EDGE = 1600;
    private static final String CACHE_PREFIX = "chat:ocr:";

    private final ProviderImageDescriber describer = SpringUtils.getBean(ProviderImageDescriber.class);

    private final ExecutorService pool = Executors.newFixedThreadPool(
        CONCURRENCY,
        r -> {
            Thread t = new Thread(r, "image-ocr");
            t.setDaemon(true);
            return t;
        }
    );

    /** 批量 OCR，顺序与入参一致 */
    public List<String> ocrAll(List<byte[]> images) {
        List<CompletableFuture<String>> futures = new ArrayList<>(images.size());
        for (byte[] image : images) {
            futures.add(CompletableFuture.supplyAsync(() -> ocr(image), pool));
        }
        List<String> results = new ArrayList<>(images.size());
        for (CompletableFuture<String> future : futures) {
            try {
                results.add(future.join());
            } catch (Exception e) {
                log.warn("图片 OCR 失败，忽略该图: {}", e.getMessage());
                results.add("");
            }
        }
        return results;
    }

    public String ocr(byte[] imageBytes) {
        if (imageBytes == null || imageBytes.length < MIN_IMAGE_BYTES) {
            return "";
        }
        String cacheKey = CACHE_PREFIX + DigestUtil.md5Hex(imageBytes);
        String cached = RedisUtils.getCacheObject(cacheKey);
        if (cached != null) {
            return cached;
        }
        try {
            byte[] compressed = compress(imageBytes);
            String text = describer.describeFull(compressed, "image.png");
            if (text == null) {
                text = "";
            }
            RedisUtils.setCacheObject(cacheKey, text, Duration.ofDays(7));
            return text;
        } catch (Exception e) {
            log.warn("图片 OCR 异常: {}", e.getMessage());
            return "";
        }
    }

    /** 压缩到最大边 1600，显著降低图像 token 与上传耗时 */
    private byte[] compress(byte[] original) throws Exception {
        BufferedImage src = ImageIO.read(new ByteArrayInputStream(original));
        if (src == null) {
            return original;
        }
        int width = src.getWidth();
        int height = src.getHeight();
        int maxEdge = Math.max(width, height);
        if (maxEdge <= MAX_EDGE) {
            return original;
        }
        double scale = (double) MAX_EDGE / maxEdge;
        int targetWidth = (int) (width * scale);
        int targetHeight = (int) (height * scale);

        BufferedImage target = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = target.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.drawImage(src, 0, 0, targetWidth, targetHeight, null);
        graphics.dispose();

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(target, "jpg", out);
            return out.toByteArray();
        }
    }
}
