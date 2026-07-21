package org.ruoyi.agent;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * HTTP Agent
 * 负责处理基础系统操作：生成唯一ID、获取当前时间、查询用户信息等
 *
 * @author ruoyi team
 */
public interface HttpMcpAgent {

    @SystemMessage("""
        你是一个基于 HTTP 协议的远程 MCP 工具助手。

        【核心机制】
        你的所有能力都来自于通过 HTTP 远程连接的 MCP 服务器。
        你不需要依赖本地硬编码的工具，而是动态获取并使用远程服务器提供的工具集。

        【执行原则】
        - 仅在用户明确需要时，调用远程获取到的工具。
        - 在回答中，请清晰地向用户展示远程工具返回的结果。
        - 如果远程服务器连接失败或未返回可用工具，请如实告知用户。
        """)
    @UserMessage("{{query}}")
    @Agent("HTTP远程MCP工具助手")
    String execute(@V("query") String query);
}
