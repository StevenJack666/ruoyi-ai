package org.ruoyi.observability;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.langchain4j.mcp.client.McpCallContext;
import dev.langchain4j.mcp.client.McpClientListener;
import dev.langchain4j.mcp.client.McpGetPromptResult;
import dev.langchain4j.mcp.client.McpReadResourceResult;
import dev.langchain4j.mcp.protocol.McpCallToolRequest;
import dev.langchain4j.mcp.protocol.McpClientMessage;
import dev.langchain4j.service.tool.ToolExecutionResult;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.sse.dto.SseEventDto;
import org.ruoyi.common.sse.utils.SseMessageUtils;
import org.ruoyi.mcp.service.core.ToolConfirmationManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * MCP 客户端监听器
 * <p>
 * 监听 MCP 工具执行事件，通过 SSE 推送到前端。
 * 支持用户二次确认：AI 调用工具前推送确认事件，等待用户同意后才执行。
 *
 * @author evo
 */
@Slf4j
public class MyMcpClientListener implements McpClientListener {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /** 静态 holder，由 Spring 启动时注入 */
    @Setter
    private static ToolConfirmationManager confirmationManager;

    /** 需要用户二次确认的工具名称列表 */
    private static final Set<String> CONFIRM_REQUIRED_TOOLS = Set.of(
        "execute_sql",
        "edit_file",
        "delete_file",
        "write_file",
        "batchLoadTools",
        "remove",
        "getCurrentTime",
        "orderMilkTea"
    );

    private final Long userId;

    public MyMcpClientListener(Long userId) {
        this.userId = userId;
    }

    public MyMcpClientListener() {
        this.userId = null;
    }

    // ==================== 工具执行 ====================

    @Override
    public void beforeExecuteTool(McpCallContext context) {
        if (!(context.message() instanceof McpCallToolRequest request)) return;
        String name = (String) request.getParams().get("name");
        log.info("工具调用之前：{}", name);
        pushMcpEvent(name, "pending", null);

        // 用户二次确认（仅针对特定工具）
        if (confirmationManager != null && userId != null && CONFIRM_REQUIRED_TOOLS.contains(name)) {
            @SuppressWarnings("unchecked")
            ObjectNode argsNode = (ObjectNode) request.getParams().get("arguments");
            Map<String, Object> args = new ObjectMapper().convertValue(argsNode, Map.class);
            String confirmId = confirmationManager.createConfirmation(userId, name, args);
            boolean approved = confirmationManager.waitForConfirmation(confirmId);
            if (!approved) {
                log.info("用户拒绝工具调用: {}", name);
                return;
            }
            log.info("用户同意工具调用: {}", name);
        }
    }

    @Override
    public void afterExecuteTool(McpCallContext context, ToolExecutionResult result, Map<String, Object> rawResult) {
        if (!(context.message() instanceof McpCallToolRequest request)) return;
        String name = (String) request.getParams().get("name");
        String resultText = result != null ? result.toString() : "";
        log.info("工具调用之后：{}, 返回结果 {}", name, result);
        pushMcpEvent(name, "success", truncate(resultText, 500));
    }

    @Override
    public void onExecuteToolError(McpCallContext context, Throwable error) {
        String toolName = getMethodName(context);
        log.error("【MCP工具执行错误】工具: {}, 错误: {}", toolName, error.getMessage());
        pushMcpEvent(toolName, "error", error.getMessage());
    }

    // ==================== 资源读取 ====================

    @Override
    public void beforeResourceGet(McpCallContext context) {
        String name = getMethodName(context);
        log.info("【MCP资源读取前】资源: {}", name);
        pushMcpEvent(name, "pending", null);
    }

    @Override
    public void afterResourceGet(McpCallContext context, McpReadResourceResult result, Map<String, Object> rawResult) {
        String name = getMethodName(context);
        int count = result.contents() != null ? result.contents().size() : 0;
        log.info("【MCP资源读取后】资源: {}, 数量: {}", name, count);
        pushMcpEvent(name, "success", "读取 " + count + " 条资源");
    }

    @Override
    public void onResourceGetError(McpCallContext context, Throwable error) {
        String name = getMethodName(context);
        log.error("【MCP资源读取错误】资源: {}, 错误: {}", name, error.getMessage());
        pushMcpEvent(name, "error", error.getMessage());
    }

    // ==================== 提示词获取 ====================

    @Override
    public void beforePromptGet(McpCallContext context) {
        String name = getMethodName(context);
        log.info("【MCP提示词获取前】提示词: {}", name);
        pushMcpEvent(name, "pending", null);
    }

    @Override
    public void afterPromptGet(McpCallContext context, McpGetPromptResult result, Map<String, Object> rawResult) {
        String name = getMethodName(context);
        int count = result.messages() != null ? result.messages().size() : 0;
        log.info("【MCP提示词获取后】提示词: {}, 消息数: {}", name, count);
        pushMcpEvent(name, "success", "获取 " + count + " 条消息");
    }

    @Override
    public void onPromptGetError(McpCallContext context, Throwable error) {
        String name = getMethodName(context);
        log.error("【MCP提示词获取错误】提示词: {}, 错误: {}", name, error.getMessage());
        pushMcpEvent(name, "error", error.getMessage());
    }

    // ==================== 辅助方法 ====================

    private String getMethodName(McpCallContext context) {
        try {
            McpClientMessage message = context.message();
            return message.method != null ? message.method.toString() : "unknown";
        } catch (Exception e) {
            return "unknown";
        }
    }

    protected void pushMcpEvent(String name, String status, String result) {
        if (userId == null) {
            log.warn("userId 为空，无法推送 MCP 事件");
            return;
        }
        try {
            Map<String, Object> content = new HashMap<>();
            content.put("name", name);
            content.put("status", status);
            content.put("result", result);

            String json = OBJECT_MAPPER.writeValueAsString(content);
            SseMessageUtils.sendEvent(userId, SseEventDto.builder()
                .event("mcp")
                .content(json)
                .build());
        } catch (JsonProcessingException e) {
            log.error("序列化 MCP 事件失败: {}", e.getMessage());
        }
    }

    private String truncate(String str, int maxLen) {
        if (str == null) return null;
        return str.length() > maxLen ? str.substring(0, maxLen) + "..." : str;
    }
}
