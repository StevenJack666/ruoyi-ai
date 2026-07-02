/**
 * Copyright by www.tienon.com
 * All right reserved.
 */
package org.ruoyi.interceptor.impl;

import cn.hutool.core.codec.Base64Decoder;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.ruoyi.common.core.utils.openapi.Basecrypt;
import org.ruoyi.config.AppIdContext;
import org.ruoyi.config.OpenApiConfig;
import org.ruoyi.support.AuthenticationStorage;
import org.ruoyi.support.domain.vo.AuthenticationVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.AsyncHandlerInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * @author zouhuaqiang
 * @Description openApi拦截器
 * @date 2022/02/28
 */
@Component
@Slf4j
public class OpenApiInterceptor implements AsyncHandlerInterceptor {

    public static final int TOKEN_PARAMS_LEN = 3;

    public static final String CACHE_KEY_PRE = "openapi:uuid:";
    public static final String X_REQ_TIMESTAMP = "X-Req-Timestamp";

    public static final String SPLIT_CHAR = "\\|";

    public static final String LOG_ERR_1 = "缺失必要请求参数！";
    public static final String LOG_ERR_2 = "token规则不正确！";
    public static final String LOG_ERR_3 = "请勿发送重复请求！";
    public static final String LOG_ERR_4 = "appid未注册！";
    public static final String LOG_ERR_5 = "系统异常！";
    public static final String LOG_ERR_6 = "签名错误！";
    public static final String LOG_ERR_7 = "IP address verification failed";
    public static final String LOG_ERR_8 = "There is no permission to access this resource";

    AntPathMatcher pathMatcher = new AntPathMatcher();

    @Resource
    private OpenApiConfig openApiConfig;


    @Resource
    private AuthenticationStorage authenticationStorage;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {

        //是否开启鉴权
        if(!authenticationStorage.verifySignature){
            return true;
        }
        String realIp = request.getHeader("X-Forwarded-For");
        log.info("处理前ip:{}",realIp);
        //添加对客户端多个ip的处理
        //针对多个ip处理
        if (StringUtils.isNotBlank(realIp)&&realIp.contains(Constants.COMMA)) {
            String[] split = realIp.split(Constants.COMMA);
            if (split.length > 0) {
                realIp = split[0];
            }
        }
        String requestURI = request.getRequestURI();
        //不需要进行签名校验直接返回ture
        if(verifyUri(request.getRequestURI())){
            return true;
        }

        // 获取token base64Encode(APPID|UUID|checkValue,UTF-8)=TOKEN
        String token = request.getHeader("token");
        if (StringUtils.isBlank(token)){
            setErrResponse(response,403,LOG_ERR_1);
            return false;
        }
        String timestampStr = request.getHeader(X_REQ_TIMESTAMP);
        if (StringUtils.isBlank(timestampStr)) {
            setErrResponse(response,403,"未找到请求时间戳");
            return false;
        }
        //验证签名是否过期
        long time = System.currentTimeMillis()- Long.parseLong(timestampStr);
        if(time > openApiConfig.getExpiredTime() * 1000){
            setErrResponse(response,403,"签名已失效");
            return false;
        }
        String tokenDecodeStr = Base64Decoder.decodeStr(token, StandardCharsets.UTF_8);
        log.info("原始token:{}",tokenDecodeStr);
        String[] tokenParams = tokenDecodeStr.split(SPLIT_CHAR);
        if (tokenParams.length != TOKEN_PARAMS_LEN){
            setErrResponse(response,403,LOG_ERR_2);
            return false;
        }
        // 校验token组成参数
        String appid = tokenParams[0];
        if (StringUtils.isBlank(appid)){
            log.error("appId为空！");
            setErrResponse(response,403,LOG_ERR_2);
            return false;
        }

        // 将appId存储在ThreadLocal中
        AppIdContext.setAppId(appid);

        String uuid = tokenParams[1];
        if (StringUtils.isBlank(uuid)){
            log.error("uuid为空！");
            setErrResponse(response,403,LOG_ERR_2);
            return false;
        }

        String checkValue = tokenParams[2];
        if (StringUtils.isBlank(checkValue)){
            log.error("checkValue为空！");
            setErrResponse(response,403,LOG_ERR_2);
            return false;
        }
        /** 取APPID查secret
         * appId改为数据库获取,对appId的url和ip进行限制,拦截路径:/openapi/**(排除/openapi/isoc/**)
         * 20250626
         */
        AuthenticationVO authInfo = authenticationStorage.getAuthInfo(appid);
        if (authInfo == null){
            log.error("appId:{}未注册",appid);
            setErrResponse(response,403,LOG_ERR_4);
            return false;
        }

        log.info("appId:{},ip:{},uri:{}", appid, realIp, requestURI);

        String secret = authInfo.getSecurityKey();
        if (StringUtils.isBlank(secret)){
            log.error("appId:{}的秘钥为空",appid);
            setErrResponse(response,403,LOG_ERR_5);
            return false;
        }

        //校验ip
        if (!authenticationStorage.verifyIp(appid, realIp)) {
            log.error("ip:{}校验失败",realIp);
            setErrResponse(response,403,LOG_ERR_7);
            return false;
        }

        //判断是否有权限访问该uri
        if (authInfo.getUrl() == null || authInfo.getUrl().size() == 0
                || !matchURI(requestURI, authInfo.getUrl())) {
            setErrResponse(response,403,LOG_ERR_8);
            return false;
        }
        // 计算checkValue  (请求体json串 + APPID + SECRET + UUID)
        String checkValueTemp = Basecrypt.sm3(concatStr(appid, secret, uuid,timestampStr));
        // 比较checkValue
        if (!checkValue.equals(checkValueTemp)){
            setErrResponse(response,403,LOG_ERR_6);
            return false;
        }
        return true;
    }

    private static String concatStr(String appid,String secret,String uuid,String timestampStr){
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(appid).append(secret).append(uuid).append(timestampStr);
        return stringBuffer.toString();
    }

    private static String createCacheKey(String appid,String uuid){
        return CACHE_KEY_PRE.concat(appid).concat(uuid);
    }

    private void setErrResponse(HttpServletResponse response,Integer errCode,String errMsg){
        log.error("openapi验证失败，错误码:{},错误信息：{}",errCode,errMsg);
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

    /**
     * @param requestURI 访问的url
     * @param list
     * @return
     * @Description 匹配uri，匹配上返回true，否则false
     */
    private boolean matchURI(String requestURI, List<String> list) {
        for (String uri : list) {
            if (pathMatcher.match(uri, requestURI)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 校验URI是否需要鉴权验签
     * @param requestURI
     * @return
     */
    private boolean verifyUri(String requestURI){
        if(!authenticationStorage.verifySignature){
            return true;
        }
        List<String> signatureVerifyList = authenticationStorage.getSignatureVerifyList();
        if(signatureVerifyList == null || signatureVerifyList.size() <= 0){
            return true;
        }
        if(!matchURI(requestURI,signatureVerifyList)){
            return true;
        }
        return false;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 清除ThreadLocal中的appId
        AppIdContext.clear();
    }

}
