package org.ruoyi.requirement.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.ruoyi.requirement.domain.ReqRequirementHistory;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
@AutoMapper(target = ReqRequirementHistory.class)
public class ReqRequirementHistoryVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long requirementId;

    private String actionType;

    private String fieldName;

    private String oldValue;

    private String newValue;

    private String actionRemark;

    private Long operatorId;

    private Date createTime;
}
