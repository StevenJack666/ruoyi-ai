package org.ruoyi.requirement.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.common.core.validate.AddGroup;
import org.ruoyi.common.core.validate.EditGroup;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;
import org.ruoyi.requirement.domain.ReqRequirement;

import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = ReqRequirement.class, reverseConvertGenerate = false)
public class ReqRequirementBo extends BaseEntity {

    @NotNull(message = "主键不能为空", groups = { EditGroup.class })
    private Long id;

    @NotNull(message = "项目不能为空", groups = { AddGroup.class, EditGroup.class })
    private Long projectId;

    private String reqCode;

    @NotBlank(message = "需求标题不能为空", groups = { AddGroup.class, EditGroup.class })
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
}
