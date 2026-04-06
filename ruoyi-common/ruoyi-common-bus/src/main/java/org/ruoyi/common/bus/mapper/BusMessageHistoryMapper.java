package org.ruoyi.common.bus.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.ruoyi.common.bus.domain.BusMessageHistory;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * 消息历史 Mapper
 */
@Mapper
public interface BusMessageHistoryMapper extends BaseMapper<BusMessageHistory> {

    @Insert("""
        INSERT INTO sse_message_history(user_id, message_id, message, read_status, remark, tenant_id, create_time, update_time)
        VALUES(#{userId}, #{messageId}, #{message}, #{readStatus}, #{remark}, IFNULL(#{tenantId}, 0), NOW(), NOW())
        ON DUPLICATE KEY UPDATE
            message = VALUES(message),
            remark = VALUES(remark),
            update_time = NOW()
        """)
    int upsert(BusMessageHistory entity);
}
