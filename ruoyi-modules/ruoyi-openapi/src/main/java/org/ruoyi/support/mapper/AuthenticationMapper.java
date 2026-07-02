package org.ruoyi.support.mapper;


import org.apache.ibatis.annotations.Mapper;
import org.ruoyi.support.domain.vo.AuthenticationVO;

import java.util.List;

/**
 * @Description: 接口
 * @Author: xyn
 * @date: 2021/1/19 10:24
 */
@Mapper
public interface AuthenticationMapper {

    /**
     * 获取认证信息
     *
     * @return 认证信息列表
     */
    List<AuthenticationVO> getAuthenInfo();

    /**
     * 根据应用ID获取权限资源
     *
     * @param appId 应用ID
     * @return 权限资源列表
     */
    List<String> getPermissionResourceByAppId(String appId);

    /**
     * 根据应用ID获取IP
     *
     * @param appId 应用ID
     * @return IP列表
     */
    List<String> getIpByAppId(String appId);

    /**
     * 获取需要拦截的资源
     *
     * @return 拦截资源列表
     */
    List<String> getInterceptorResource();
}
