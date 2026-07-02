package org.ruoyi.support.domain;

import lombok.Data;

/**
 * 资源信息对象 resource_info
 *
 * @author admin
 * @date 2024-10-08
 */
@Data
public class ResourceInfo extends CommonRequest {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    private Long id;

    /**
     * 资源URI
     */
    private String resourceUri;

    /**
     * 资源简称
     */
    private String name;

    /**
     * 状态
     */
    private Integer status;

}
