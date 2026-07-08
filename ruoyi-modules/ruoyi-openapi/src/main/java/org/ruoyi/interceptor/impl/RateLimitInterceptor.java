package org.ruoyi.interceptor.impl;

import com.alibaba.fastjson.JSONObject;
import com.google.common.util.concurrent.RateLimiter;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.ruoyi.config.AppIdContext;
import org.ruoyi.config.RateLimitStorage;
import org.ruoyi.support.domain.DataFlowConfig;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class RateLimitInterceptor implements HandlerInterceptor {

    @Resource
    private RateLimitStorage rateLimitStorage;

    public static final String LIMIT_ERR_INFO = "系统繁忙,拒绝请求请稍后重试！";

    private AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 从ThreadLocal中获取appId
        String appId = AppIdContext.getAppId();
        //appId为空的不限制
        if (StringUtils.isBlank(appId)) {
            return true;
        }
        String requestUri = request.getRequestURI();
        //获取流控配置
        DataFlowConfig dataFlowConfig = this.getFlowConfigByAppIdUri(appId,requestUri);
        if(dataFlowConfig == null || dataFlowConfig.getPermitsPer() <= 0){
            return true;
        }
        log.info("RateLimitInterceptor-appId:{},uri:{}进入流控{}-{}", appId, requestUri,dataFlowConfig.getId(),dataFlowConfig.getPermitsPer());
        RateLimiter rateLimiter = rateLimitStorage.getRateLimiterOrNew(appId,requestUri,dataFlowConfig.getPermitsPer());
        boolean ok ;
        if(dataFlowConfig.getWaitTimeout() > 0 ){
            //尝试等待获取策略
            // 获取自定义配置， 没有则获取默认配置
            ok = rateLimiter.tryAcquire(dataFlowConfig.getWaitTimeout(), TimeUnit.MILLISECONDS);
        }else{ //不等待
            ok = rateLimiter.tryAcquire();
        }
        if(!ok){
            setErrResponse(response,429,LIMIT_ERR_INFO);
            return false;
        }
        return true;
    }

    /**
     * 获取流控最优匹配信息
     * @param appId 应用APPID
     * @param requestUri 请求URI
     * @return 流控配置
     */
    private DataFlowConfig getFlowConfigByAppIdUri(String appId,String requestUri){
        List<DataFlowConfig> dataFlowConfigList = rateLimitStorage.getFlowConfigListByAppId(appId);
        if(dataFlowConfigList == null || dataFlowConfigList.size() <= 0){
            return null;
        }
        if(dataFlowConfigList.size() == 1){
            DataFlowConfig dataFlowConfig = dataFlowConfigList.get(0);
            if(pathMatcher.match(dataFlowConfig.getResourceUri(),requestUri)){
                return dataFlowConfig;
            }
        }else {
            //最优匹配信息
            DataFlowConfig dataFlowConfig = dataFlowConfigList.stream()
                    .filter(config -> pathMatcher.match(config.getResourceUri(),requestUri))
                    .sorted((c1,c2) -> pathMatcher.getPatternComparator(requestUri).compare(c1.getResourceUri(),c2.getResourceUri()))
                    .findFirst().orElse(null);
            return dataFlowConfig;
        }
        return null;
    }


    private void setErrResponse(HttpServletResponse response,Integer errCode,String errMsg){
        log.error("流控限制，错误码:{},错误信息：{}",errCode,errMsg);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("code",errCode);
        jsonObject.put("msg",errMsg);
        try {
            response.setStatus(errCode);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(jsonObject.toJSONString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
