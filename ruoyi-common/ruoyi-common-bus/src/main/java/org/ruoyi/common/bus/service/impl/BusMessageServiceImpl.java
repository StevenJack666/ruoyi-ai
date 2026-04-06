package org.ruoyi.common.bus.service.impl;

import java.util.Date;
import java.util.List;

import org.ruoyi.common.bus.domain.BusMessageHistory;
import org.ruoyi.common.bus.mapper.BusMessageHistoryMapper;
import org.ruoyi.common.bus.service.IBusMessageService;
import org.ruoyi.common.mybatis.core.page.PageQuery;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import lombok.RequiredArgsConstructor;

/**
 * 消息中心服务实现
 */
@Service
@RequiredArgsConstructor
public class BusMessageServiceImpl implements IBusMessageService {

    private final BusMessageHistoryMapper busMessageHistoryMapper;

    @Override
    public void saveMessage(Long userId, String messageId, String message) {
        BusMessageHistory history = new BusMessageHistory();
        history.setUserId(userId);
        history.setMessageId(messageId);
        history.setMessage(message);
        history.setReadStatus(0);
        busMessageHistoryMapper.upsert(history);
    }

    @Override
    public List<BusMessageHistory> listUnread(Long userId) {
        return busMessageHistoryMapper.selectList(new LambdaQueryWrapper<BusMessageHistory>()
            .eq(BusMessageHistory::getUserId, userId)
            .eq(BusMessageHistory::getReadStatus, 0)
            .orderByAsc(BusMessageHistory::getCreateTime)
            .orderByAsc(BusMessageHistory::getId));
    }

    @Override
    public void ackMessages(Long userId, List<String> messageIds) {
        if (messageIds == null || messageIds.isEmpty()) {
            return;
        }
        LambdaUpdateWrapper<BusMessageHistory> updateWrapper = new LambdaUpdateWrapper<BusMessageHistory>()
            .eq(BusMessageHistory::getUserId, userId)
            .in(BusMessageHistory::getMessageId, messageIds)
            .eq(BusMessageHistory::getReadStatus, 0)
            .set(BusMessageHistory::getReadStatus, 1)
            .set(BusMessageHistory::getUpdateTime, new Date());
        busMessageHistoryMapper.update(null, updateWrapper);
    }

    @Override
    public void ackAll(Long userId) {
        LambdaUpdateWrapper<BusMessageHistory> updateWrapper = new LambdaUpdateWrapper<BusMessageHistory>()
            .eq(BusMessageHistory::getUserId, userId)
            .eq(BusMessageHistory::getReadStatus, 0)
            .set(BusMessageHistory::getReadStatus, 1)
            .set(BusMessageHistory::getUpdateTime, new Date());
        busMessageHistoryMapper.update(null, updateWrapper);
    }

    @Override
    public TableDataInfo<BusMessageHistory> pageHistory(Long userId, PageQuery pageQuery) {
        Page<BusMessageHistory> page = busMessageHistoryMapper.selectPage(pageQuery.build(),
            new LambdaQueryWrapper<BusMessageHistory>()
                .eq(BusMessageHistory::getUserId, userId)
                .orderByDesc(BusMessageHistory::getCreateTime)
                .orderByDesc(BusMessageHistory::getId));
        return new TableDataInfo<>(page.getRecords(), page.getTotal());
    }
}
