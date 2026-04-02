package org.ruoyi.service.chat;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.service.chat.routing.AgentIntentRoutingStrategy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Agent 意图路由服务 - 策略调度器
 * <p>
 * 策略选择规则：
 * <ol>
 *   <li>若配置了 {@code ruoyi.agent.routing.strategy}，直接使用指定名称的策略</li>
 *   <li>未配置时，按 {@link AgentIntentRoutingStrategy#getOrder()} 升序依次尝试，
 *       返回第一个命中结果（fallback 链）</li>
 * </ol>
 * <p>
 * 扩展方式：实现 {@link AgentIntentRoutingStrategy} 接口并注册为 Spring Bean 即可，
 * 无需修改本类。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentIntentRoutingService {

    /**
     * 指定使用的路由策略名称，对应 {@link AgentIntentRoutingStrategy#getName()}。
     * 留空则按优先级依次尝试所有策略。
     * 示例：ruoyi.agent.routing.strategy=keyword-scoring
     */
    @Value("${ruoyi.agent.routing.strategy:}")
    private String configuredStrategyName;

    private final List<AgentIntentRoutingStrategy> strategies;

    /**
     * 根据用户输入匹配最合适的 Agent 市场。
     */
    public Optional<AgentMatchResult> selectBestMatch(String userInput) {
        if (StringUtils.isBlank(userInput)) {
            return Optional.empty();
        }
        if (CollectionUtils.isEmpty(strategies)) {
            log.warn("没有可用的意图路由策略");
            return Optional.empty();
        }

        if (StringUtils.isNotBlank(configuredStrategyName)) {
            return runSingleStrategy(userInput, configuredStrategyName);
        }
        return runStrategyChain(userInput);
    }

    /**
     * 直接执行指定名称的策略。
     */
    private Optional<AgentMatchResult> runSingleStrategy(String userInput, String strategyName) {
        Optional<AgentIntentRoutingStrategy> target = strategies.stream()
            .filter(s -> strategyName.equals(s.getName()))
            .findFirst();

        if (target.isEmpty()) {
            log.warn("配置的意图路由策略 '{}' 不存在，已注册策略: {}", strategyName,
                strategies.stream().map(AgentIntentRoutingStrategy::getName).toList());
            return Optional.empty();
        }

        try {
            return target.get().match(userInput).map(r -> {
                log.info("意图路由命中策略={}, marketId={}, marketName={}, score={}",
                    strategyName, r.marketId(), r.marketName(), r.score());
                return new AgentMatchResult(r.marketId(), r.marketName(), r.description(), r.score());
            });
        } catch (Exception e) {
            log.warn("意图路由策略={} 执行异常: {}", strategyName, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * 按优先级依次尝试所有策略，返回第一个命中结果。
     */
    private Optional<AgentMatchResult> runStrategyChain(String userInput) {
        List<AgentIntentRoutingStrategy> ordered = strategies.stream()
            .sorted(Comparator.comparingInt(AgentIntentRoutingStrategy::getOrder))
            .toList();

        for (AgentIntentRoutingStrategy strategy : ordered) {
            try {
                Optional<AgentIntentRoutingStrategy.AgentMatchResult> result = strategy.match(userInput);
                if (result.isPresent()) {
                    AgentIntentRoutingStrategy.AgentMatchResult r = result.get();
                    log.info("意图路由命中策略={}, marketId={}, marketName={}, score={}",
                        strategy.getName(), r.marketId(), r.marketName(), r.score());
                    return Optional.of(new AgentMatchResult(r.marketId(), r.marketName(), r.description(), r.score()));
                }
                log.debug("意图路由策略={} 未命中，继续下一策略", strategy.getName());
            } catch (Exception e) {
                log.warn("意图路由策略={} 执行异常，跳过: {}", strategy.getName(), e.getMessage());
            }
        }

        return Optional.empty();
    }

    /**
     * 向后兼容的匹配结果（对外 API 保持不变）
     */
    public record AgentMatchResult(Long marketId, String marketName, String description, double score) {
    }
}