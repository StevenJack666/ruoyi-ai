package org.ruoyi.factory;

import org.ruoyi.service.IntelAnalysisService;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 研判分类服务工厂类
 *
 * @author ageerle@163.com
 * @date 2025-12-13
 */
@Component
public class IntelAnalysisStrategyFactory implements ApplicationContextAware {

    private final Map<String, IntelAnalysisService> intelAnalysisServiceMap = new ConcurrentHashMap<>();

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        // 初始化时收集所有IntelAnalysisService的实现
        Map<String, IntelAnalysisService> serviceMap = applicationContext.getBeansOfType(IntelAnalysisService.class);
        for (IntelAnalysisService service : serviceMap.values()) {
            if (service != null ) {
                intelAnalysisServiceMap.put(service.getSceneCode(), service);
            }
        }
    }


    /**
     * 获取原始服务（不包装代理）
     */
    public IntelAnalysisService getOriginalService(String category) {
        IntelAnalysisService service = intelAnalysisServiceMap.get(category);
        if (service == null) {
            throw new IllegalArgumentException("不支持的操作类别: " + category);
        }
        return service;
    }
}
