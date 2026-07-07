package org.ruoyi.service.chat.impl.openapi;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.chat.enums.RoleType;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.common.json.utils.JsonUtils;
import org.ruoyi.service.IntelAnalysisService;
import org.ruoyi.service.chat.IChatMessageService;
import org.ruoyi.system.service.ISysConfigService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * 情报分析策略抽象基类（封装了与 LLM 交互的通用模板）
 * @param <T> 具体的分析结果 DTO 类型
 */
@Slf4j
public abstract class AbstractIntelAnalysisService<T> implements IntelAnalysisService {

    @Autowired
    private IChatMessageService chatMessageService;

    @Autowired
    private ISysConfigService sysConfigService;

    @Override
    public Object analyze(String content, String model, Long userId, ChatModel chatModel) {
        // 1. 调用子类提供的 Prompt
        String systemPrompt = getSystemPrompt();

        // 2. 组装消息列表
        List<ChatMessage> messages = List.of(
            new SystemMessage(systemPrompt),
            new UserMessage(content)
        );

        try {
            // 3. 调用大模型
            ChatResponse chatResponse = chatModel.chat(messages);
            String jsonResult = chatResponse.aiMessage().text();
            log.info("【{}】模型原始返回: {}", getSceneCode(), jsonResult);

            // 4. 清洗数据 (防止模型偶尔输出 ```json ... ```)
            jsonResult = cleanJsonResult(jsonResult);

            // 保存大模型消息
            chatMessageService.saveChatMessage(userId, getSessionId(),
                jsonResult, RoleType.ASSISTANT.getName(), model);

            // 5. 转换为子类指定的 DTO 类型
            return JsonUtils.parseObject(jsonResult, getResultClass());
        } catch (Exception e) {
            log.error("【{}】分析失败", getSceneCode(), e);
            throw new RuntimeException("AI 分析服务异常", e);
        }
    }

    @Override
    public Long getSessionId(){
        String sessionId = sysConfigService.selectConfigByKey(getSessionConfigKey());
        if (StringUtils.isEmpty(sessionId)){
            throw new ServiceException("未配置【{}】该操作的SessionId默认值", getSceneCode());
        }
        return Long.parseLong(sessionId);
    }

    /**
     * 获取系统提示词（由子类实现）
     */
    protected abstract String getSystemPrompt();

    /**
     * 获取解析的目标 DTO 类型（由子类实现）
     */
    protected abstract Class<T> getResultClass();

    /**
     * 清洗 JSON 字符串（提供默认实现，子类也可重写）
     */
    protected String cleanJsonResult(String jsonResult) {
        if (jsonResult == null) return null;
        return jsonResult.replaceAll("^```json\\s*", "").replaceAll("\\s*```\\s*$", "");
    }

    /**
     * 获取不同操作的会话配置键
     */
    protected abstract String getSessionConfigKey();
}
