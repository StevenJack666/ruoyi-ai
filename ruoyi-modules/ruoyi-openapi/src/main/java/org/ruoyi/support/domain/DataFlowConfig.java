package org.ruoyi.support.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;


/**
 * 数据服务流控配置对象 rds_data_flow_config
 *
 * @author admin
 * @date 2024-11-19
 */
@Data
public class DataFlowConfig extends CommonRequest {


    private static final long serialVersionUID = 1L;

    /**
     * 参数配置ID
     */
    private Long id;

    /**
     * 应用ID
     */
    private String appId;

    /**
     * 请求RUI路径
     */
    private String resourceUri;

    /**
     * 并发数(每秒)
     */
    private Double permitsPer;

    /**
     * 等待时间(毫秒)
     */
    private Long waitTimeout;

    /**
     * 是否启用
     */
    private String enableFlag;

    /**
     * 创建者
     */
    private String createBy;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /**
     * 更新者
     */
    private String updateBy;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

    /**
     * 是否已删除
     */
    private String isDeleted;

    /**
     * 备注
     */
    private String remark;
}
