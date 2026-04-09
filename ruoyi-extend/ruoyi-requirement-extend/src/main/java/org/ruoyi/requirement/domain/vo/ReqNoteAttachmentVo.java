package org.ruoyi.requirement.domain.vo;

import java.io.Serial;
import java.io.Serializable;

import org.ruoyi.requirement.domain.ReqNoteAttachment;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

@Data
@AutoMapper(target = ReqNoteAttachment.class)
public class ReqNoteAttachmentVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long noteId;

    private String fileUrl;
}
