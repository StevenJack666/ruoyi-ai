package org.ruoyi.agent;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 上传文档解析智能体
 */
public interface DocParseAgent {

    @SystemMessage("""
        你是一个极其专业的文档分析专家。
        你的唯一职责是：阅读并解析用户提供的参考文档内容，提取核心事实，并根据用户的提问给出精准回答。
        - 必须严格基于文档内容回答，禁止编造。
        - 回答要求结构化、条理清晰。
        """)
    @UserMessage("参考文档内容：\n{{documentContext}}\n\n用户问题：{{query}}")
    @Agent("文档解析专家: 专门负责解析、提取、总结刚刚上传的文档内容")
    String parse(@V("documentContext") String documentContext, @V("query") String query);
}
