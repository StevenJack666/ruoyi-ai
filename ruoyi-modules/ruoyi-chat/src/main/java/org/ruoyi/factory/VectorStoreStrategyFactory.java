package org.ruoyi.factory;

import lombok.extern.slf4j.Slf4j;
import org.ruoyi.config.VectorStoreProperties;
import org.ruoyi.service.vector.VectorStoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 向量库策略工厂
 * <p>
 * 自动注入所有 VectorStoreService 实现，bean name 作为策略 key。
 * 新增策略只需 @Component("name") + 实现 VectorStoreService，无需改工厂。
 *
 * @author Yzm
 */
@Slf4j
@Component
public class VectorStoreStrategyFactory {

    private final VectorStoreProperties vectorStoreProperties;
    private final Map<String, VectorStoreService> strategies;

    @Autowired
    public VectorStoreStrategyFactory(VectorStoreProperties vectorStoreProperties,
                                      Map<String, VectorStoreService> strategies) {
        this.vectorStoreProperties = vectorStoreProperties;
        this.strategies = strategies;
        log.info("向量库策略工厂初始化完成，支持的策略: {}", strategies.keySet());
    }

    /**
     * 获取当前配置的向量库策略
     */
    public VectorStoreService getStrategy() {
        String vectorStoreType = vectorStoreProperties.getType();
        if (vectorStoreType == null || vectorStoreType.trim().isEmpty()) {
            vectorStoreType = "weaviate";
        }
        VectorStoreService strategy = strategies.get(vectorStoreType.toLowerCase());
        if (strategy == null) {
            log.warn("未找到向量库策略: {}, 使用默认: weaviate", vectorStoreType);
            strategy = strategies.get("weaviate");
        }
        log.debug("使用向量库策略: {}", vectorStoreType);
        return strategy;
    }
}
