package org.ruoyi.support.controller;

import jakarta.annotation.Resource;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.support.domain.ResourceInfo;
import org.ruoyi.support.page.TableDataInfo;
import org.ruoyi.support.service.IResourceInfoService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * openApi资源信息Controller
 *
 * @author admin
 * @date 2024-10-08
 */
@RestController
@RequestMapping("/secApi/resourceInfo")
public class ResourceInfoController extends BaseController {

    @Resource
    private IResourceInfoService resourceInfoService;

    /**
     * 查询openApi资源信息列表
     */
    @GetMapping("/list")
    public TableDataInfo list(ResourceInfo resourceInfo) {
        startPage();
        List<ResourceInfo> list = resourceInfoService.selectResourceInfoList(resourceInfo);
        return getDataTable(list);
    }

    /**
     * 获取openApi资源信息详细信息
     */
    @GetMapping(value = "/{id}")
    public R<?> getInfo(@PathVariable("id") Long id) {
        return R.ok(resourceInfoService.selectResourceInfoById(id));
    }

    /**
     * 新增openApi资源信息
     */
    @PostMapping
    public R<?> add(@RequestBody ResourceInfo resourceInfo) {
        return toAjax(resourceInfoService.insertResourceInfo(resourceInfo));
    }

    /**
     * 修改openApi资源信息
     */
    @PostMapping("/edit")
    public R<?> edit(@RequestBody ResourceInfo resourceInfo) {
        return toAjax(resourceInfoService.updateResourceInfo(resourceInfo));
    }

    /**
     * 删除openApi资源信息
     */
    @DeleteMapping("/{ids}")
    public R<?> remove(@PathVariable Long[] ids) {
        return toAjax(resourceInfoService.deleteResourceInfoByIds(ids));
    }


}
