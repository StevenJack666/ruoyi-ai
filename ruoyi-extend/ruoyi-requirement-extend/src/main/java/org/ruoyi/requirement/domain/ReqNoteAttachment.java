package org.ruoyi.requirement.domain;

import java.io.Serial;

import org.ruoyi.common.tenant.core.TenantEntity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("req_ext_note_attachment")
public class ReqNoteAttachment extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    private Long id;

    private Long noteId;

    private String fileUrl;

    @TableLogic
    private String delFlag;
}
