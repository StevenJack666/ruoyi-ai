package org.ruoyi.requirement.domain;

import java.io.Serial;
import java.util.Date;

import org.ruoyi.common.tenant.core.TenantEntity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("req_ext_project_bug")
public class ReqProjectBug extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    private Long id;

    private Long projectId;

    private String bugCode;

    private String title;

    private String severity;

    private String priority;

    private String status;

    private Long ownerId;

    private Long assigneeId;

    private String foundVersion;

    private String fixedVersion;

    private String reproduceSteps;

    private String expectedResult;

    private String actualResult;

    private Date resolvedTime;

    @TableLogic
    private String delFlag;
}
