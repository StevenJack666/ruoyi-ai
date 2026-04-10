package org.ruoyi.requirement.domain.bo;

import java.util.Date;

import org.ruoyi.common.core.validate.AddGroup;
import org.ruoyi.common.core.validate.EditGroup;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;
import org.ruoyi.requirement.domain.ReqProjectBug;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = ReqProjectBug.class, reverseConvertGenerate = false)
public class ReqProjectBugBo extends BaseEntity {

    @NotNull(message = "主键不能为空", groups = { EditGroup.class })
    private Long id;

    @NotNull(message = "项目不能为空", groups = { AddGroup.class, EditGroup.class })
    private Long projectId;

    private String bugCode;

    @NotBlank(message = "Bug标题不能为空", groups = { AddGroup.class, EditGroup.class })
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

    private String screenshotUrls;

    private Date resolvedTime;
}
