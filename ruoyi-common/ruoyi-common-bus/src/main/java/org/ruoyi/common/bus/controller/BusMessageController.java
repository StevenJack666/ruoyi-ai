package org.ruoyi.common.bus.controller;

import lombok.RequiredArgsConstructor;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
     * 查询消息历史(包含已读)
     */
    @GetMapping("/history")
    public TableDataInfo<BusMessageHistory> history(PageQuery pageQuery) {
        Long userId = LoginHelper.getUserId();
        return busMessageService.pageHistory(userId, pageQuery);
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
}
