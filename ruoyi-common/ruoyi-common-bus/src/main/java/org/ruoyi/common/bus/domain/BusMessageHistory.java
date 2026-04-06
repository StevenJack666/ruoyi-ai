package org.ruoyi.common.bus.domain;

import java.io.Serial;

import org.ruoyi.common.mybatis.core.domain.BaseEntity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;
import lombok.EqualsAndHashCode;
/**
 * 消息历史对象
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sse_message_history")
public class BusMessageHistory extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String messageId;

    private String message;

    /**
     * 0=未读 1=已读
     */
    private Integer readStatus;

    /**
     * 备注
     */
    private String remark;

    /**
     * 租户Id
     */
    private Long tenantId;
}
