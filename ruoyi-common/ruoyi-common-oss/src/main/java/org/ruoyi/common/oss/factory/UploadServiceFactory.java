package org.ruoyi.common.oss.factory;

import org.ruoyi.common.oss.UploadModeType;
import org.ruoyi.common.oss.service.IUploadService;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 上传文件服务工厂类
 *
 * @author Zengxb
 * @date 2026-02-28
 */
@Component
public class UploadServiceFactory implements ApplicationContextAware {

    private final Map<String, IUploadService> uploadServiceMap = new ConcurrentHashMap<>();

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        // 初始化时收集所有IChatService的实现
        Map<String, IUploadService> serviceMap = applicationContext.getBeansOfType(IUploadService.class);
        for (IUploadService service : serviceMap.values()) {
            if (service != null) {
                uploadServiceMap.put(service.getProviderName(), service);
            }
        }
    }

    /**
     * 获取原始服务（不包装代理）
     */
    public IUploadService getOriginalService(String providerCode) {
        IUploadService service = uploadServiceMap.get(providerCode);
        if (service == null) {
            throw new IllegalArgumentException("不支持的上传服务提供商: " + providerCode);
        }
        return service;
    }
}
