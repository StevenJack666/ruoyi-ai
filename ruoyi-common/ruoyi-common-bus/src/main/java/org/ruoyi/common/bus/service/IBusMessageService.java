package org.ruoyi.common.bus.service;

import java.util.List;

import org.ruoyi.common.bus.domain.BusMessageHistory;
import org.ruoyi.common.mybatis.core.page.PageQuery;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;

/**
 * 消息中心服务
 */
public interface IBusMessageService {

    void saveMessage(Long userId, String messageId, String message);

    List<BusMessageHistory> listUnread(Long userId);

    void ackMessages(Long userId, List<String> messageIds);

    void ackAll(Long userId);

    void deleteHistory(Long userId, Integer readStatus);

    TableDataInfo<BusMessageHistory> pageHistory(Long userId, Integer readStatus, PageQuery pageQuery);
}
