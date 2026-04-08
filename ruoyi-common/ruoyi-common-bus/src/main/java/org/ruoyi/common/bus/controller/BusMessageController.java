package org.ruoyi.common.bus.controller;

import java.util.List;

import org.ruoyi.common.bus.domain.BusMessageHistory;
import org.ruoyi.common.bus.service.IBusMessageService;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.common.mybatis.core.page.PageQuery;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.common.satoken.utils.LoginHelper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

/**
 * 消息中心控制器
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/bus/message")
public class BusMessageController {

    private final IBusMessageService busMessageService;

    /**
     * 查询当前用户未读消息
     */
    @GetMapping("/unread")
    public R<List<BusMessageHistory>> unread() {
        Long userId = LoginHelper.getUserId();
        return R.ok(busMessageService.listUnread(userId));
    }

    /**
     * 查询消息历史
     * readStatus: 0=未读 1=已读 不传=全部
     */
    @GetMapping("/history")
    public TableDataInfo<BusMessageHistory> history(@RequestParam(required = false) Integer readStatus, PageQuery pageQuery) {
        Long userId = LoginHelper.getUserId();
        return busMessageService.pageHistory(userId, readStatus, pageQuery);
    }

    /**
     * 消息已读回执(按消息ID)
     */
    @PostMapping("/ack")
    public R<Void> ack(@RequestBody List<String> messageIds) {
        Long userId = LoginHelper.getUserId();
        busMessageService.ackMessages(userId, messageIds);
        return R.ok();
    }

    /**
     * 全部已读
     */
    @PostMapping("/ackAll")
    public R<Void> ackAll() {
        Long userId = LoginHelper.getUserId();
        busMessageService.ackAll(userId);
        return R.ok();
    }

    /**
     * 删除消息历史
     * readStatus: 0=删除未读 1=删除已读 不传=删除全部
     */
    @PostMapping("/delete")
    public R<Void> delete(@RequestParam(required = false) Integer readStatus) {
        Long userId = LoginHelper.getUserId();
        busMessageService.deleteHistory(userId, readStatus);
        return R.ok();
    }
}
