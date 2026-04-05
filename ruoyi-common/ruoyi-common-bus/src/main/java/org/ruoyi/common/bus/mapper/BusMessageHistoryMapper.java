package org.ruoyi.common.bus.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.ruoyi.common.bus.domain.BusMessageHistory;

/**
 * 消息历史 Mapper
 */
@Mapper
public interface BusMessageHistoryMapper extends BaseMapper<BusMessageHistory> {

    @Insert("""
        INSERT INTO sse_message_history(user_id, message_id, message, send_time, read_status, remark, tenant_id, create_time, update_time)
        VALUES(#{userId}, #{messageId}, #{message}, #{sendTime}, #{readStatus}, #{remark}, IFNULL(#{tenantId}, 0), NOW(), NOW())
        ON DUPLICATE KEY UPDATE
            message = VALUES(message),
            send_time = VALUES(send_time),
            remark = VALUES(remark),
            update_time = NOW()
        """)
    int upsert(BusMessageHistory entity);
}
