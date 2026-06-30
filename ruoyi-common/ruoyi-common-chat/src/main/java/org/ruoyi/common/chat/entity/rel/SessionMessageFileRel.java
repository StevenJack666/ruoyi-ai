package org.ruoyi.common.chat.entity.rel;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.common.chat.entity.BaseEntity;

import java.io.Serial;

/**
 * 会话消息文件对象 chat_message
 *
 * @author ageerle
 * @date 2025-12-14
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("session_message_file_rel")
public class SessionMessageFileRel extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 对话消息ID
     */
    private Long messageId;


    /**
     * 对话ID
     */
    private Long sessionId;

    /**
     * 文件ID
     */
    private Long ossFileId;
}
