package org.ruoyi.support.controller;

import jakarta.annotation.Resource;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.support.domain.UserPermissionResource;
import org.ruoyi.support.page.TableDataInfo;
import org.ruoyi.support.service.IUserPermissionResourceService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
/**
 * 资源授权信息Controller
 *
 * @author admin
 * @date 2024-10-08
 */
@RestController
@RequestMapping("/secApi/userPermissionResource")
public class UserPermissionResourceController extends BaseController {

    @Resource
    private IUserPermissionResourceService userPermissionResourceService;

    /**
     * 查询资源授权信息列表
     */
    @GetMapping("/list")
    public TableDataInfo list(UserPermissionResource userPermissionResource) {
        startPage();
        List<UserPermissionResource> list = userPermissionResourceService.selectUserPermissionResourceList(userPermissionResource);
        return getDataTable(list);
    }

    /**
     * 获取资源授权信息详细信息
     */
    @GetMapping(value = "/{id}")
    public R<?> getInfo(@PathVariable("id") Long id) {
        return R.ok(userPermissionResourceService.selectUserPermissionResourceById(id));
    }

    /**
     * 新增资源授权信息
     */
    @PostMapping
    public R<?> add(@RequestBody UserPermissionResource userPermissionResource) {
        return toAjax(userPermissionResourceService.insertUserPermissionResource(userPermissionResource));
    }

    /**
     * 修改资源授权信息
     */
    @PostMapping("/edit")
    public R<?> edit(@RequestBody UserPermissionResource userPermissionResource) {
        return toAjax(userPermissionResourceService.updateUserPermissionResource(userPermissionResource));
    }

    /**
     * 删除资源授权信息
     */
    @DeleteMapping("/{ids}")
    public R<?> remove(@PathVariable Long[] ids) {
        return toAjax(userPermissionResourceService.deleteUserPermissionResourceByIds(ids));
    }

}
