package org.ruoyi.requirement.controller;

import java.util.List;

import org.ruoyi.common.core.domain.R;
import org.ruoyi.common.core.validate.AddGroup;
import org.ruoyi.common.core.validate.EditGroup;
import org.ruoyi.common.excel.utils.ExcelUtil;
import org.ruoyi.common.idempotent.annotation.RepeatSubmit;
import org.ruoyi.common.log.annotation.Log;
import org.ruoyi.common.log.enums.BusinessType;
import org.ruoyi.common.mybatis.core.page.PageQuery;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.common.web.core.BaseController;
import org.ruoyi.requirement.domain.bo.ReqProjectBugBo;
import org.ruoyi.requirement.domain.bo.ReqProjectBugStatusBo;
import org.ruoyi.requirement.domain.vo.ReqProjectBugVo;
import org.ruoyi.requirement.service.IReqProjectBugService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
/**
 * 项目Bug控制器
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/requirement/bug")
public class ReqProjectBugController extends BaseController {

    private final IReqProjectBugService bugService;

    @SaCheckPermission("requirement:bug:list")
    @GetMapping("/list")
    public TableDataInfo<ReqProjectBugVo> list(ReqProjectBugBo bo, PageQuery pageQuery) {
        return bugService.queryPageList(bo, pageQuery);
    }

    @SaCheckPermission("requirement:bug:export")
    @Log(title = "项目Bug", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(ReqProjectBugBo bo, HttpServletResponse response) {
        List<ReqProjectBugVo> list = bugService.queryList(bo);
        ExcelUtil.exportExcel(list, "项目Bug", ReqProjectBugVo.class, response);
    }

    @SaCheckPermission("requirement:bug:query")
    @GetMapping("/{id}")
    public R<ReqProjectBugVo> getInfo(@NotNull(message = "主键不能为空") @PathVariable Long id) {
        return R.ok(bugService.queryById(id));
    }

    @SaCheckPermission("requirement:bug:add")
    @Log(title = "项目Bug", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping
    public R<Void> add(@Validated(AddGroup.class) @RequestBody ReqProjectBugBo bo) {
        return toAjax(bugService.insertByBo(bo));
    }

    @SaCheckPermission("requirement:bug:edit")
    @Log(title = "项目Bug", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody ReqProjectBugBo bo) {
        return toAjax(bugService.updateByBo(bo));
    }

    @SaCheckPermission("requirement:bug:edit")
    @Log(title = "项目Bug状态流转", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PostMapping("/changeStatus")
    public R<Void> changeStatus(@Validated @RequestBody ReqProjectBugStatusBo bo) {
        return toAjax(bugService.changeStatus(bo.getId(), bo.getStatus(), bo.getRemark()));
    }

    @SaCheckPermission("requirement:bug:remove")
    @Log(title = "项目Bug", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空") @PathVariable Long[] ids) {
        return toAjax(bugService.deleteWithValidByIds(List.of(ids), true));
    }
}
