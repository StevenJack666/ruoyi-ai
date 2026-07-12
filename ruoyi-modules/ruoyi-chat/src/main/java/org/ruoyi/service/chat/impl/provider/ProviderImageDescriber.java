package org.ruoyi.service.chat.impl.provider;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.chat.domain.dto.request.ChatRequest;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.common.chat.service.chat.IChatModelService;
import org.ruoyi.service.chat.AbstractChatService;
import org.springframework.stereotype.Component;

import java.util.Base64;

/**
 * 基于已有 provider 路由的图片描述实现
 * <p>
 * 复用 ChatServiceFactory 路由体系，通过多模态模型（GPT-4o、Qwen-VL 等）识别图片。
 * 在数据库 chat_model 表中配置一个多模态模型，通过 application.yml 指定模型名。
 * <p>
 * 配置方式:
 * <pre>
 * ai:
 *   image:
 *     description:
 *       model: qwen-vl-plus
 * </pre>
 */
@Slf4j
@Component
public class ProviderImageDescriber implements AbstractChatService {

    private final IChatModelService chatModelService;

    public ProviderImageDescriber(IChatModelService chatModelService) {
        this.chatModelService = chatModelService;
    }

    @Override
    public String getProviderName() {
        return "multimodal";
    }

    @Override
    public StreamingChatModel buildStreamingChatModel(ChatModelVo chatModelVo, ChatRequest chatRequest) {
        throw new UnsupportedOperationException("图片描述服务不支持流式调用");
    }

    @Override
    public ChatModel buildChatModel(ChatModelVo chatModelVo) {
        return AbstractChatService.super.buildChatModel(chatModelVo);
    }

    public String describe(byte[] imageData, String fileName) {
        if (imageData == null || imageData.length == 0) {
            return "空图片";
        }

        try {
            return describeByModel(imageData, fileName);
        } catch (Exception e) {
            log.error("图片描述失败: fileName={}, error={}", fileName, e.getMessage());
            return fileName != null ? fileName : "未知图片";
        }
    }

    private int maxDescLength() {
        return 200;
    }

    private String describeByModel(byte[] imageData, String fileName) throws Exception {
        // 1. 获取图片 base64
        String suffix = fileName != null && fileName.contains(".")
            ? fileName.substring(fileName.lastIndexOf('.') + 1) : "png";

        // 2. 查询模型配置（使用 getProviderName() 作为模型名）
        ChatModelVo modelConfig = chatModelService.selectModelByName(getProviderName());
        if (modelConfig == null) {
            log.error("图片识别模型未配置: modelName={}，请在 chat_model 表中添加该模型", getProviderName());
            throw new IllegalArgumentException("未找到图片识别模型: " + getProviderName());
        }
        log.info("图片识别模型已加载: provider={}, model={}", modelConfig.getProviderCode(), modelConfig.getModelName());

        // 3. 构建模型并调用
        ChatModel model = buildChatModel(modelConfig);
        String base64 = Base64.getEncoder().encodeToString(imageData);

        UserMessage userMessage = UserMessage.from(
            "data:image/" + suffix + ";base64," + base64,
            "请用中文简要描述图片，不超过" + maxDescLength() + "字"
        );

        ChatResponse response = model.chat(dev.langchain4j.model.chat.request.ChatRequest.builder()
            .messages(java.util.List.of(userMessage))
            .build());

        String description = response.aiMessage().text();
        log.info("图片描述完成: fileName={}, descLen={}, desc={}", fileName,
            description.length(),
            description.length() > 60 ? description.substring(0, 60) + "..." : description);
        return description;
    }
}
