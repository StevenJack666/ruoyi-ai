package org.ruoyi.requirement.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.ruoyi.requirement.domain.ReqRequirementComment;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
@AutoMapper(target = ReqRequirementComment.class)
public class ReqRequirementCommentVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long requirementId;

    private String content;

    private String createBy;

    private Date createTime;
}
