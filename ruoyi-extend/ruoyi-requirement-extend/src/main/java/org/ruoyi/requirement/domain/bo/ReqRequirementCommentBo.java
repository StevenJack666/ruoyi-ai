package org.ruoyi.requirement.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.common.core.validate.AddGroup;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;
import org.ruoyi.requirement.domain.ReqRequirementComment;

@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = ReqRequirementComment.class, reverseConvertGenerate = false)
public class ReqRequirementCommentBo extends BaseEntity {

    @NotNull(message = "需求ID不能为空", groups = { AddGroup.class })
    private Long requirementId;

    @NotBlank(message = "评论内容不能为空", groups = { AddGroup.class })
    private String content;
}
