package org.ruoyi.domain.entity.knowledge;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;
import java.time.LocalDateTime;

import org.ruoyi.common.chat.entity.BaseEntity;

@Data
@TableName("session_upload_record")
public class SessionUploadRecord extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Long sessionId;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private Long ossId;
    private String ossUrl;
    private Integer chunkCount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime uploadTime;
}
