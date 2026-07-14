package org.ruoyi.agent;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 基础工具 Agent
 * 负责处理基础系统操作：生成唯一ID、获取当前时间、查询用户信息等
 *
 * @author ruoyi team
 */
public interface BasicAgent {

    @SystemMessage("""
        你是一个基础系统工具助手，能够使用基础工具来帮助用户获取系统信息和生成数据。

        【最重要原则】
        仅在用户明确要求使用基础工具时调用，不要主动调用。
        使用指南：
        - 需要生成带前缀的随机ID或单号时使用 add 工具
        - 需要获取当前系统时间时使用 getCurrentTime 工具
        - 需要查询当前登录账号或特定用户信息时使用 getUsername 工具
        - 在回答中清晰展示工具返回的结果
        """)
    @UserMessage("{{query}}")
    @Agent("基础工具助手，支持生成随机ID、获取当前时间和查询用户信息")
    String execute(@V("query") String query);
}
