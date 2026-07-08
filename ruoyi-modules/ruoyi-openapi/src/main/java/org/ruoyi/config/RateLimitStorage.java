package org.ruoyi.config;

import cn.hutool.extra.spring.SpringUtil;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.RateLimiter;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.ruoyi.support.domain.DataFlowConfig;
import org.ruoyi.support.service.IDataFlowConfigService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 限流信息容器
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
@Slf4j
public class RateLimitStorage {

    /** 内存中存储限流对象 */
    private Map<String , RateLimiter> limiterMap = new ConcurrentHashMap<>();

    //数据服务参数配置表刷新时间,默认1小时(3600)
    @Value("${openApi.guava.cache.refresh.data-param-config:3600}")
    private long dataFlowConfigRefreshSecond;

    /**
     * 流控配置缓存列表  KEY为APPID
     */
    private LoadingCache<String, List<DataFlowConfig>> dataFlowConfigListCache;

    @PostConstruct
    public void init() {
        log.info("===================OpenApi流控参数配置缓存初始化start=============================");
        log.info("dataFlowConfig数据参数配置刷新时间:{}",dataFlowConfigRefreshSecond);
        dataFlowConfigListCache = CacheBuilder.newBuilder().refreshAfterWrite(dataFlowConfigRefreshSecond, TimeUnit.SECONDS).build(
                new CacheLoader<String, List<DataFlowConfig>>() {
                    @Override
                    public List<DataFlowConfig> load(String key) throws Exception {
                        log.info("======刷新dataFlowConfigListCache数据:[{}]start======",key);
                        if(StringUtils.isBlank(key)){
                            log.info("======刷新dataFlowConfigListCache数据:[{}]-返回空end======",key);
                            return null;
                        }
                        List<DataFlowConfig> resultList = SpringUtil.getBean(IDataFlowConfigService.class).loadCacheListByAppId(key);
                        log.info("======刷新dataFlowConfigListCache数据:[{}]-size:[{}]end======",key,resultList == null ? 0 : resultList.size());
                        return resultList;
                    }
                }
        );
        log.info("===================OpenApi流控参数配置缓存初始化end=============================");
    }

    /**
     *
     * @param appId 应用ID
     * @param requestUri 限流对象
     * @param permitsPerSecond 初始限流并发数
     * @return
     */
    public RateLimiter getRateLimiterOrNew(String appId,String requestUri,double permitsPerSecond){
        //组装KEY
        String key = this.genLimiterKey(appId,requestUri);
        RateLimiter rateLimiter = limiterMap.get(key);
        if(rateLimiter == null){
            rateLimiter = RateLimiter.create(permitsPerSecond);
            limiterMap.put(key,rateLimiter);
        }else {
            //重置限流速率
            if(rateLimiter.getRate() != permitsPerSecond){
                log.info("{}-修改速率,从{}修改为{}",key,rateLimiter.getRate(),permitsPerSecond);
                rateLimiter.setRate(permitsPerSecond);
                limiterMap.put(key,rateLimiter);
            }
        }
        return rateLimiter;
    }

    /**
     * 变更限流对象速率
     * @param key 存储KEY
     * @param permitsPerSecond 速率
     */
    public void setRate(String key,double permitsPerSecond){
        if(limiterMap.containsKey(key)){
            limiterMap.get(key).setRate(permitsPerSecond);
        }
    }

    /**
     * 查询配置列表
     * @param appId 应用ID
     * @return 返回配置列表
     */
    public List<DataFlowConfig> getFlowConfigListByAppId(String appId){
        try{
            return this.dataFlowConfigListCache.get(appId);
        }catch (Exception e){
            log.error("GuavaCache缓存获取getFlowConfigListByAppId方法异常:",e);
        }
        return null;
    }

    public void invalidateAllFlowConfig(){
        this.dataFlowConfigListCache.invalidateAll();
    }

    public String genLimiterKey(String appId,String requestUri){
        return String.format("%s-%s",appId,requestUri);
    }
}
