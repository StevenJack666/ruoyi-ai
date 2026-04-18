package org.ruoyi.requirement.domain.vo;

import java.io.Serial;
import java.io.Serializable;

import org.ruoyi.requirement.domain.ReqNoteAttachment;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

@Data
@AutoMapper(target = ReqNoteAttachment.class)
public class ReqNoteAttachmentVo extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long noteId;

    private String fileUrl;
}
