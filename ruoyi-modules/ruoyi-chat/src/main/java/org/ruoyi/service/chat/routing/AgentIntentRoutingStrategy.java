package org.ruoyi.service.chat.routing;

import java.util.Optional;

/**
 * Agent 意图路由策略接口
 * <p>
 * 实现此接口以提供不同的意图匹配算法（关键词评分、向量检索、LLM 分类等）。
 * 所有策略通过 Spring 注入后由 {@link org.ruoyi.service.chat.AgentIntentRoutingService} 统一调度。
 */
public interface AgentIntentRoutingStrategy {

    /**
     * 策略优先级，数字越小优先级越高。
     * 调度器会按优先级依次尝试，第一个返回非空结果的策略胜出。
     */
    int getOrder();

    /**
     * 策略名称，用于日志与监控区分。
     */
    String getName();

    /**
     * 根据用户输入匹配最合适的 Agent 市场。
     *
     * @param userInput 用户原始输入
     * @return 匹配结果，无法匹配时返回 {@link Optional#empty()}
     */
    Optional<AgentMatchResult> match(String userInput);

    /**
     * 意图匹配结果
     *
     * @param marketId    Agent 市场主键
     * @param marketName  Agent 名称
     * @param description Agent 描述
     * @param score       匹配得分（不同策略的量纲可能不同，仅供日志参考）
     */
    record AgentMatchResult(Long marketId, String marketName, String description, double score) {
    }
}
