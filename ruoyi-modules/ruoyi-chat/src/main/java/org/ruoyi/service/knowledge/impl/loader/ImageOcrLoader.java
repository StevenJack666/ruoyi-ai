package org.ruoyi.service.knowledge.impl.loader;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.core.utils.SpringUtils;
import org.ruoyi.service.chat.impl.provider.ProviderImageDescriber;
import org.ruoyi.service.knowledge.ResourceLoader;
import org.ruoyi.service.knowledge.TextSplitter;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * 图片 OCR 识别加载器
 * 负责将图片文件通过 OCR 提取文字或生成描述，并进行文本分块
 *
 * @author ruoyi team
 */
@Component
@AllArgsConstructor
@Slf4j
public class ImageOcrLoader implements ResourceLoader {

    private final TextSplitter textSplitter;

    // 复用现有的图片描述/OCR服务提供者
    private final ProviderImageDescriber describer = SpringUtils.getBean(ProviderImageDescriber.class);

    @Override
    public String getContent(InputStream inputStream) {
        try {
            // 1. 将输入流读取为字节数组（ProviderImageDescriber 通常需要 byte[]）
            byte[] imageBytes = inputStream.readAllBytes();

            if (imageBytes.length == 0) {
                log.warn("图片文件为空，跳过 OCR 识别");
                return "";
            }

            // 2. 调用图片描述/OCR 服务
            // 由于独立图片文件没有原始文件名，这里传入一个默认标识
            String description = describer != null
                ? describer.describe(imageBytes, "uploaded_image")
                : "";

            log.debug("图片 OCR/描述完成，提取内容长度: {} 字符", description.length());
            return description.trim();
        } catch (IOException e) {
            log.error("图片 OCR 识别失败", e);
            throw new RuntimeException("图片 OCR 识别失败", e);
        }
    }

    @Override
    public List<String> getChunkList(String content, String kid) {
        // 3. 将 OCR 提取出的纯文本内容，交由 TextSplitter 进行分块处理
        return textSplitter.split(content, kid);
    }
}
