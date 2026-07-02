package org.ruoyi.support.controller;

import jakarta.annotation.Resource;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.support.domain.UserPermissionIp;
import org.ruoyi.support.page.TableDataInfo;
import org.ruoyi.support.service.IUserPermissionIpService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
/**
 * openApi授权IP白名单Controller
 *
 * @author admin
 * @date 2024-10-08
 */
@RestController
@RequestMapping("/secApi/userPermissionIp")
public class UserPermissionIpController extends BaseController {

    @Resource
    private IUserPermissionIpService userPermissionIpService;

    /**
     * 查询openApi授权IP白名单列表
     */
    @GetMapping("/list")
    public TableDataInfo list(UserPermissionIp userPermissionIp) {
        startPage();
        List<UserPermissionIp> list = userPermissionIpService.selectUserPermissionIpList(userPermissionIp);
        return getDataTable(list);
    }

    /**
     * 获取openApi授权IP白名单详细信息
     */
    @GetMapping(value = "/{id}")
    public R<?> getInfo(@PathVariable("id") Long id) {
        return R.ok(userPermissionIpService.selectUserPermissionIpById(id));
    }

    /**
     * 新增openApi授权IP白名单
     */
    @PostMapping
    public R<?> add(@RequestBody UserPermissionIp userPermissionIp) {
        return toAjax(userPermissionIpService.insertUserPermissionIp(userPermissionIp));
    }

    /**
     * 修改openApi授权IP白名单
     */
    @PostMapping("/edit")
    public R<?> edit(@RequestBody UserPermissionIp userPermissionIp) {
        return toAjax(userPermissionIpService.updateUserPermissionIp(userPermissionIp));
    }

    /**
     * 删除openApi授权IP白名单
     */
    @DeleteMapping("/{ids}")
    public R<?> remove(@PathVariable Long[] ids) {
        return toAjax(userPermissionIpService.deleteUserPermissionIpByIds(ids));
    }


}
