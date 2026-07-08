package org.ruoyi.service;


import com.alibaba.fastjson.JSONObject;
import dev.langchain4j.model.chat.ChatModel;

/**
 * 场景处理策略接口
 */
public interface IntelAnalysisService {

    // 获取该策略支持的场景标识
    String getSceneCode();

    // 执行分析逻辑
    Object analyze(String content, String model, Long userId, ChatModel chatModel);

    // 获取会话配置（包含SessionId、title、content）
    JSONObject getSessionConfig();
}
