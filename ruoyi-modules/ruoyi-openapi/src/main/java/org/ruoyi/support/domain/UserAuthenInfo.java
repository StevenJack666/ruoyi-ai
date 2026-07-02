package org.ruoyi.support.domain;

import lombok.Data;

/**
 * 授权应用信息对象 user_authen_info
 *
 * @author admin
 * @date 2024-10-08
 */
@Data
public class UserAuthenInfo extends CommonRequest {

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
     * 未签名KEY
     */
    private String unSecurityKey;
    /**
     * 签名KEY
     */
    private String securityKey;

    /**
     * 签名方式
     */
    private String securityType;

    /**
     * 所属机构
     */
    private String organization;

}
