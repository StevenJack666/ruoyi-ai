package org.ruoyi.requirement.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
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
import org.ruoyi.requirement.domain.bo.ReqProjectBo;
import org.ruoyi.requirement.domain.vo.ReqProjectVo;
import org.ruoyi.requirement.service.IReqProjectService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/requirement/project")
public class ReqProjectController extends BaseController {

    private final IReqProjectService projectService;

    @SaCheckPermission("requirement:project:list")
    @GetMapping("/list")
    public TableDataInfo<ReqProjectVo> list(ReqProjectBo bo, PageQuery pageQuery) {
        return projectService.queryPageList(bo, pageQuery);
    }

    @SaCheckPermission("requirement:project:export")
    @Log(title = "需求项目", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(ReqProjectBo bo, HttpServletResponse response) {
        List<ReqProjectVo> list = projectService.queryList(bo);
        ExcelUtil.exportExcel(list, "需求项目", ReqProjectVo.class, response);
    }

    @SaCheckPermission("requirement:project:query")
    @GetMapping("/{id}")
    public R<ReqProjectVo> getInfo(@NotNull(message = "主键不能为空") @PathVariable Long id) {
        return R.ok(projectService.queryById(id));
    }

    @SaCheckPermission("requirement:project:add")
    @Log(title = "需求项目", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping
    public R<Void> add(@Validated(AddGroup.class) @RequestBody ReqProjectBo bo) {
        return toAjax(projectService.insertByBo(bo));
    }

    @SaCheckPermission("requirement:project:edit")
    @Log(title = "需求项目", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody ReqProjectBo bo) {
        return toAjax(projectService.updateByBo(bo));
    }

    @SaCheckPermission("requirement:project:remove")
    @Log(title = "需求项目", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空") @PathVariable Long[] ids) {
        return toAjax(projectService.deleteWithValidByIds(List.of(ids), true));
    }
}
