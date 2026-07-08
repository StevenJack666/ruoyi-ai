package org.ruoyi.config;

import org.ruoyi.interceptor.impl.OpenApiInterceptor;
import jakarta.annotation.Resource;
import org.ruoyi.interceptor.impl.RateLimitInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


/**
 * 通用配置
 *
 * @author admin
 */
@Configuration
public class OpenApiResourcesConfig implements WebMvcConfigurer {


    @Resource
    private OpenApiInterceptor openApiInterceptor;

    @Resource
    private RateLimitInterceptor rateLimitInterceptor;

    /**
     * 自定义拦截规则
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(openApiInterceptor)
                .addPathPatterns("/openapi/**");
        registry.addInterceptor(rateLimitInterceptor).addPathPatterns("/openapi/**");
    }
}
