package org.ruoyi.requirement.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.common.core.validate.AddGroup;
import org.ruoyi.common.core.validate.EditGroup;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;
import org.ruoyi.requirement.domain.ReqProject;

@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = ReqProject.class, reverseConvertGenerate = false)
public class ReqProjectBo extends BaseEntity {

    @NotNull(message = "主键不能为空", groups = { EditGroup.class })
    private Long id;

    @NotBlank(message = "项目编码不能为空", groups = { AddGroup.class, EditGroup.class })
    private String projectCode;

    @NotBlank(message = "项目名称不能为空", groups = { AddGroup.class, EditGroup.class })
    private String projectName;

    private Long ownerId;

    private String status;

    private String description;
}
