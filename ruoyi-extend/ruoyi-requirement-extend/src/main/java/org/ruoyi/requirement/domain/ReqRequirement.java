package org.ruoyi.requirement.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.common.tenant.core.TenantEntity;

import java.io.Serial;
import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("req_ext_requirement")
public class ReqRequirement extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    private Long id;

    private Long projectId;

    private String reqCode;

    private String title;

    private String type;

    private String priority;

    private String status;

    private Long ownerId;

    private Long assigneeId;

    private String source;

    private Date planStartTime;

    private Date planEndTime;

    private String content;

    private String flowCode;

    private Long processInstanceId;

    @TableLogic
    private String delFlag;
}
