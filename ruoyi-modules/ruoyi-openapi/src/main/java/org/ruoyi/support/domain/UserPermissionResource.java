package org.ruoyi.support.domain;

import lombok.Data;

/**
 * 资源授权信息对象 user_permission_resource
 *
 * @author admin
 * @date 2024-10-08
 */
@Data
public class UserPermissionResource extends CommonRequest {
    /**
     * 主键
     */
    private Long id;

    /**
     * 应用ID
     */
    private String appId;

    /**
     * 资源URI
     */
    private String resourceUri;

}
