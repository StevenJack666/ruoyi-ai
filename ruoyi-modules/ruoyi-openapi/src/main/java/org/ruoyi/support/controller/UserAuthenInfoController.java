package org.ruoyi.support.controller;

import jakarta.annotation.Resource;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.support.domain.UserAuthenInfo;
import org.ruoyi.support.page.TableDataInfo;
import org.ruoyi.support.service.IUserAuthenInfoService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
/**
 * openApi授权应用信息Controller
 *
 * @author admin
 * @date 2024-10-08
 */
@RestController
@RequestMapping("/secApi/userAuthenInfo")
public class UserAuthenInfoController extends BaseController {

    @Resource
    private IUserAuthenInfoService userAuthenInfoService;

    /**
     * 查询openApi授权应用信息列表
     */
    @GetMapping("/list")
    public TableDataInfo list(UserAuthenInfo userAuthenInfo) {
        startPage();
        List<UserAuthenInfo> list = userAuthenInfoService.selectUserAuthenInfoList(userAuthenInfo);
        return getDataTable(list);
    }

    /**
     * 获取openApi授权应用信息详细信息
     */
    @GetMapping(value = "/{id}")
    public R<?> getInfo(@PathVariable("id") Long id) {
        return R.ok(userAuthenInfoService.selectUserAuthenInfoById(id));
    }

    /**
     * 新增openApi授权应用信息
     */
    @PostMapping
    public R<?> add(@RequestBody UserAuthenInfo userAuthenInfo) {
        return toAjax(userAuthenInfoService.insertUserAuthenInfo(userAuthenInfo));
    }

    /**
     * 修改openApi授权应用信息
     */
    @PostMapping("/edit")
    public R<?> edit(@RequestBody UserAuthenInfo userAuthenInfo) {
        return toAjax(userAuthenInfoService.updateUserAuthenInfo(userAuthenInfo));
    }

    /**
     * 删除openApi授权应用信息
     */
    @DeleteMapping("/{ids}")
    public R<?> remove(@PathVariable Long[] ids) {
        return toAjax(userAuthenInfoService.deleteUserAuthenInfoByIds(ids));
    }
    /**
     * 一键生成授权应用信息
     */
    @GetMapping("/createUserAuthenInfo")
    public R<?> createUserAuthenInfo() {
        return toAjax(userAuthenInfoService.createUserAuthenInfo());
    }

}
