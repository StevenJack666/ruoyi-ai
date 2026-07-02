package org.ruoyi.support.domain;

import lombok.Data;

/**
 * 授权IP白名单对象 user_permission_ip
 *
 * @author admin
 * @date 2024-10-08
 */
@Data
public class UserPermissionIp extends CommonRequest {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    private Long id;

    /**
     * 应用ID
     */
    private String appId;

    /**
     * IP地址
     */
    private String ip;


}
