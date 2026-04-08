package org.ruoyi.requirement.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.common.tenant.core.TenantEntity;

import java.io.Serial;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("req_ext_requirement_history")
public class ReqRequirementHistory extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    private Long id;

    private Long requirementId;

    private String actionType;

    private String fieldName;

    private String oldValue;

    private String newValue;

    private String actionRemark;

    private Long operatorId;

    @TableLogic
    private String delFlag;
}
