package org.ruoyi.support;


import jakarta.annotation.PostConstruct;
import org.ruoyi.common.core.utils.openapi.CommonUtils;
import org.ruoyi.common.core.utils.openapi.GenerateKeyUtil;
import org.ruoyi.support.domain.vo.AuthenticationVO;
import org.ruoyi.support.mapper.AuthenticationMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.iv.NoIvGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @Description:
 * @Author: xyn
 * @date: 2020/12/21 9:26
 */
@Component
@Slf4j
public class AuthenticationStorage {

    //是否校验ip: 是：true, 否：false
    @Value("${openapi.signature.isVerifyIp:true}")
    private boolean verifyIp;

    //鉴权信息刷新间隔
    @Value("${openapi.signature.info.refresh:3600}")
    private long refreshTime;

    //是否进行签名校验：是：true,否：false,默认为true
    @Value("${openapi.signature.isVerify:true}")
    public boolean verifySignature;

    @Autowired
    private AuthenticationMapper authenticationMapper;

    //加密工具
    private StandardPBEStringEncryptor standardPBEStringEncryptor;

    /**
     * 鉴权信息缓存列表  KEY为APPID
     */
    private Map<String, AuthenticationVO> authInfoMap = null;

    /**
     * 需要进行签名校验的接口
     */
    private List<String> signatureVerifyList = null;

    /**
     * 接口鉴权签名信息初始化
     */
    @PostConstruct
    public void init() {
        if (!verifySignature) {
            log.info("================签名校验已关闭================");
            return;
        }
        log.info("===========================OpenApi鉴权签名信息初始化===============================");
        authInfoMap = new HashMap<>();
        signatureVerifyList = new ArrayList<>();
        standardPBEStringEncryptor = new StandardPBEStringEncryptor();
        standardPBEStringEncryptor.setIvGenerator(new NoIvGenerator());
        standardPBEStringEncryptor.setAlgorithm("PBEWithMD5AndDES");
        standardPBEStringEncryptor.setPassword(GenerateKeyUtil.SECURITY_KEY_ENCRYPTOR);
        AuthenInfoRefresh refresh = new AuthenInfoRefresh();
        ScheduledExecutorService executorService = new ScheduledThreadPoolExecutor(1);
        executorService.scheduleAtFixedRate(refresh, 0, refreshTime, TimeUnit.SECONDS);
        log.info("===========================OpenApi鉴权签名信息初始化完毕===============================");
    }

    private class AuthenInfoRefresh implements Runnable {
        @Override
        public void run() {
            try {
                log.info("======================OpenApi开始刷新鉴权签名信息数据===================");
                //获取用户信息
                List<AuthenticationVO> list = authenticationMapper.getAuthenInfo();
                //获取需要签名校验的uri
                signatureVerifyList = authenticationMapper.getInterceptorResource();
                authInfoMap.clear();
                if (list != null && list.size() > 0) {
                    list.forEach(x -> {
                                List<String> ipList = authenticationMapper.getIpByAppId(x.getAppId());
                                if (ipList != null && ipList.size() > 0) {
                                    ipList.forEach(ip -> x.getIp().add(CommonUtils.ipCovertToLong(ip)));
                                }
                                x.setSecurityKey(standardPBEStringEncryptor.decrypt(x.getSecurityKey()));
                                x.setUrl(authenticationMapper.getPermissionResourceByAppId(x.getAppId()));
                                authInfoMap.put(x.getAppId(), x);
                            }
                    );
                }
                log.info("======================OpenApi鉴权签名信息数据刷新完毕===================");
            } catch (Exception e) {
                log.error("鉴权签名信息数据刷新失败：" + e.getMessage(), e);
            }
        }
    }

    /**
     * @param appId 应用ID
     * @param ip    IP地址
     * @return 验证结果
     */
    public boolean verifyIp(String appId, String ip) {
        log.info("请求ip:{}", ip);
        if (!verifyIp) {
            return true;
        }
        if (StringUtils.isBlank(ip)) {
            return false;
        }
        AuthenticationVO authenticationVO = authInfoMap.get(appId);
        List<Long> ipList = authenticationVO.getIp();
        if (ipList == null || ipList.size() == 0) {
            return false;
        }

        for (Long allow : ipList) {
            if (allow.equals(CommonUtils.ipCovertToLong(ip))) {
                return true;
            }
        }
        return false;
    }

    public AuthenticationVO getAuthInfo(String appId) {
        if (authInfoMap == null) {
            return null;
        }
        return authInfoMap.get(appId);
    }

    public List<String> getSignatureVerifyList() {
        return signatureVerifyList;
    }
}
