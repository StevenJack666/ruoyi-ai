package org.ruoyi.service.chat.impl.openapi;

import com.alibaba.fastjson.JSONObject;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.chat.enums.RoleType;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.common.json.utils.JsonUtils;
import org.ruoyi.service.IntelAnalysisService;
import org.ruoyi.service.chat.IChatMessageService;
import org.ruoyi.system.service.ISysConfigService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
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

    /**
     * 大模型最大重试次数配置key
     */
    private static final String MAX_RETRIES_CONFIG_KEY = "llm.maxRetries";

    /**
     * 大模型最大重试次数默认值
     */
    private static final int DEFAULT_MAX_RETRIES = 3;

    @Override
    public Object analyze(String content, String model, Long userId, ChatModel chatModel) {
        // 1. 组装消息列表 (注意：必须用 ArrayList，因为重试时需要追加消息)
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new SystemMessage(getSystemPrompt()));
        messages.add( new UserMessage(content));

        Long sessionId = getSessionConfig().getLong("sessionId");
        String jsonResult = null;
        try {
            // 2. 首次调用大模型
            ChatResponse chatResponse = chatModel.chat(messages);
            jsonResult = chatResponse.aiMessage().text();
            log.info("【{}】模型原始返回: {}", getSceneCode(), jsonResult);
            // 3. 尝试清洗并解析
            String cleanedJson = cleanJsonResult(jsonResult);
            Object response = JsonUtils.parseObject(cleanedJson, getResultClass());
            // 4. 如果解析成功且不为空，说明没有幻觉，直接走正常流程
            if (response != null) {
                chatMessageService.saveChatMessage(userId, sessionId, cleanedJson, RoleType.ASSISTANT.getName(), model);
                return response;
            }
            // 5. 解析结果为空，说明出现幻觉，进入重试机制
            log.warn("【{}】首次返回 JSON 解析结果为空，触发重试机制", getSceneCode());
        } catch (Exception e) {
            // 6. 清洗或解析抛出异常，说明出现幻觉，进入重试机制
            log.warn("【{}】首次返回 JSON 格式异常或解析失败: {}，触发重试机制", getSceneCode(), e.getMessage());
        }
        // 7. 走到这里说明首次尝试失败，执行重试修复
        int maxRetries = getMaxRetries();
        String retryJsonResult = executeWithRetry(chatModel, messages, jsonResult, maxRetries);
        // 8. 重试成功后保存并返回
        chatMessageService.saveChatMessage(userId, sessionId, retryJsonResult, RoleType.ASSISTANT.getName(), model);
        return JsonUtils.parseObject(retryJsonResult, getResultClass());
    }

    /**
     * 幻觉重试机制
     */
    protected String executeWithRetry(ChatModel chatModel, List<ChatMessage> messages,
                                      String content, int maxRetries) {

        String jsonResult = content;
        String lastFailedJson = null;
        // 提前获取参考示例，避免在循环中重复解析
        String outputExample = getOutputExample();
        for (int i = 1; i <= maxRetries; i++) {
            try {
                // 1. 触发重试：将错误信息 + 参考示例反馈给大模型
                if (jsonResult != null) {
                    messages.add(new AiMessage(jsonResult));
                }
                // 🌟 核心优化：带上参考示例，让模型“照猫画虎”
                String retryPrompt = String.format(
                    "你上一次返回的 JSON 格式有误或不符合要求，请严格只输出合法的 JSON 对象，不要包含任何解释文字。\n" +
                        "错误原因：解析失败或不符合业务标准。\n" +
                        "请严格参考以下输出格式和字段要求进行修正：\n%s",
                    outputExample
                );
                messages.add(new UserMessage(retryPrompt));
                // 2. 重新调用大模型
                ChatResponse retryResponse = chatModel.chat(messages);
                jsonResult = retryResponse.aiMessage().text();
                log.info("【{}】第 {} 次重试模型原始返回: {}", getSceneCode(), i, jsonResult);
                // 3. 清洗并校验
                String cleanedJson = cleanJsonResult(jsonResult);
                Object response = JsonUtils.parseObject(cleanedJson, getResultClass());
                if (response == null) {
                    throw new IllegalArgumentException("响应体解析为空或不符合业务标准");
                }
                return cleanedJson;
            } catch (Exception e) {
                log.warn("【{}】第 {} 次重试仍然失败: {}", getSceneCode(), i, e.getMessage());
                if (i == maxRetries) {
                    throw new RuntimeException("大模型返回格式持续异常，解析失败。最后一次原始返回: " + jsonResult, e);
                }
                // 防抖机制：如果模型连续返回相同的错误，直接终止
                if (jsonResult != null && jsonResult.equals(lastFailedJson)) {
                    log.error("【{}】大模型连续返回相同的错误 JSON，陷入死循环，终止重试。", getSceneCode());
                    throw new RuntimeException("大模型连续返回相同的错误 JSON，无法自动修复。", e);
                }
                lastFailedJson = jsonResult;
            }
        }
        throw new RuntimeException("未知重试异常");
    }

    @Override
    public JSONObject getSessionConfig(){
        String sessionKey = sysConfigService.selectConfigByKey(getSessionConfigKey());
        if (StringUtils.isEmpty(sessionKey)){
            throw new ServiceException("未配置【{}】该操作的SessionId默认值", getSceneCode());
        }
        JSONObject jsonObject = JSONObject.parseObject(sessionKey);
        if (null == jsonObject){
            throw new ServiceException("获取【{}】该操作的Session配置转换异常", getSceneCode());
        }
        Long sessionId = jsonObject.getLong("sessionId");
        if (null == sessionId){
            throw new ServiceException("获取【{}】该操作的SessionId配置为空！", getSceneCode());
        }
        String sessionContent = jsonObject.getString("sessionContent");
        if (StringUtils.isEmpty(sessionContent)){
            throw new ServiceException("获取【{}】该操作的sessionContent配置为空！", getSceneCode());
        }
        String sessionTitle = jsonObject.getString("sessionTitle");
        if (StringUtils.isEmpty(sessionTitle)){
            throw new ServiceException("获取【{}】该操作的sessionTitle配置为空！", getSceneCode());
        }
        return jsonObject;
    }

    /**
     * 获取重试次数
     */
    private int getMaxRetries(){
        String configKey = sysConfigService.selectConfigByKey(MAX_RETRIES_CONFIG_KEY);
        if (StringUtils.isEmpty(configKey)){
            log.warn("未配置大模型最大重试次数，将使用默认值 3");
            return DEFAULT_MAX_RETRIES;
        }
        return Integer.parseInt(configKey);
    }

    /**
     * 从子类的 System Prompt 中提取输出示例，作为重试时的参考
     */
    protected String getOutputExample() {
        String prompt = getSystemPrompt();
        if (prompt == null) return "";

        // 尝试提取 【输出示例】 之后的内容
        int index = prompt.indexOf("【输出示例】");
        if (index != -1) {
            return prompt.substring(index);
        }
        // 如果没有这个标记，返回整个 Prompt 的最后 500 个字符作为兜底
        return prompt.length() > 500 ? prompt.substring(prompt.length() - 500) : prompt;
    }

    /**
     * 清洗 JSON 字符串（提供默认实现，子类也可重写）
     */
    protected String cleanJsonResult(String jsonResult) {
        if (jsonResult == null) return null;
        return jsonResult.replaceAll("```json\\s*", "").replaceAll("\\s*```\\s*", "");
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
     * 获取不同操作的会话配置键
     */
    protected abstract String getSessionConfigKey();
}
