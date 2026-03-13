package org.ruoyi.domain.bo.agent;

import org.ruoyi.common.core.validate.AddGroup;
import org.ruoyi.common.core.validate.EditGroup;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;
import org.ruoyi.domain.entity.agent.AiSkill;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Agent 技能业务对象
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = AiSkill.class, reverseConvertGenerate = false)
public class AiSkillBo extends BaseEntity {

    @NotNull(message = "主键不能为空", groups = {EditGroup.class})
    private Long id;

    @NotBlank(message = "技能名称不能为空", groups = {AddGroup.class, EditGroup.class})
    private String skillName;

    @NotBlank(message = "技能编码不能为空", groups = {AddGroup.class, EditGroup.class})
    private String skillCode;

    private String description;

    private String remark;
}
