package org.ruoyi.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * @author zouhuaqiang
 * @Description openapi配置
 * @date 2022/02/28
 */
@Configuration
@ConfigurationProperties(prefix = "openapi")
@Getter
@Setter
public class OpenApiConfig {

    //签名过期时间，单位：s,默认10s
    private long expiredTime = 10;

    private Map<String, String> appids;

    /** openid uuid缓存时间 */
    private Integer cacheUuidTime;
    /**分页查询每页返回最大数*/
    private int maxPageSize = 1000;
}


