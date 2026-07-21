package org.ruoyi.observability;

import org.ruoyi.domain.result.InterceptResult;

/**
 * 工具拦截策略接口
 */
public interface ToolInterceptStrategy {
    /**
     * 判断是否匹配当前工具
     */
    boolean matches(String toolName);

    /**
     * 前置拦截（执行前）
     */
    InterceptResult onBeforeExecute(String toolName, String argumentsJson, Long userId);

    /**
     * 后置增强（执行后）
     * @param toolName 工具名称
     * @param result 原始 MCP 工具返回的结果
     * @param userId 当前用户ID
     */
    default void onAfterExecute(String toolName, String result, Long userId) {}
}
