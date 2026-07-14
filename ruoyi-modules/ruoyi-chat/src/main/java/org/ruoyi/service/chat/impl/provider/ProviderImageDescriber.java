package org.ruoyi.service.chat.impl.provider;

import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.data.message.*;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.chat.domain.dto.request.ChatRequest;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.common.chat.service.chat.IChatModelService;
import org.ruoyi.factory.ChatServiceFactory;
import org.ruoyi.service.chat.AbstractChatService;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.List;

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
@RequiredArgsConstructor
public class ProviderImageDescriber  {

    private final ChatServiceFactory chatServiceFactory;
    private final IChatModelService chatModelService;

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
        ChatModelVo modelConfig = chatModelService.selectModelByCategory(getCategory());
        if (modelConfig == null) {
            throw new IllegalArgumentException("模型对应的分类不存在: " + getCategory());
        }
        log.info("图片识别模型已加载: provider={}, model={}", modelConfig.getProviderCode(), modelConfig.getModelName());

        // 【核心改动】通过工厂获取指定的模型运营商（比如通义千问）
        String providerCode = modelConfig.getProviderCode();
        log.info("路由到服务提供商: {}, 模型: {}", providerCode, modelConfig.getModelName());
        AbstractChatService chatService = chatServiceFactory.getOriginalService(providerCode);
        if (chatService == null) {
            throw new IllegalArgumentException("未找到指定的模型服务");
        }

        // 3. 构建模型并调用
        ChatModel model = chatService.buildChatModel(modelConfig);
        String base64 = Base64.getEncoder().encodeToString(imageData);

        String userPrompt = "你是一个专业的 OCR 文字识别引擎。请严格提取图片中的所有可见文字。\n" +
            "要求：\n" +
            "1. 仅输出识别到的纯文本内容，严格保持原始的换行、段落和空格结构。\n" +
            "2. 严禁输出任何解释性、描述性或总结性的语言（如“图片中显示”、“这是一个界面”、“识别结果如下”等）。\n" +
            "3. 不要猜测、补全或编造任何图中不存在的文字。\n" +
            "4. 如果图片中没有文字，请直接返回空字符串。 \n" +
            "5.字数限制在" + maxDescLength() + "字以内。";

        // 3. 构建内容对象（明确传入 base64 字符串和 MIME 类型）
        ImageContent imageContent = ImageContent.from(base64, "image/" + suffix);
        // 4. 构建文本提示词
        TextContent textContent = TextContent.from(userPrompt);
        UserMessage userMessage = UserMessage.from(imageContent, textContent);

        ChatResponse response = model.chat(dev.langchain4j.model.chat.request.ChatRequest.builder()
            .messages(List.of(userMessage))
            .build());

        String description = response.aiMessage().text();
        if (description.length() < 5) {
            log.warn("模型返回内容过短，可能识别失败");
        }

        log.info("图片描述完成: fileName={}, descLen={}, desc={}", fileName,
            description.length(),
            description.length() > 60 ? description.substring(0, 60) + "..." : description);
        return description;
    }

    /**
     * 获取模型分类
     */
    private String getCategory(){
        return "multimodal";
    }
}
