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
import org.ruoyi.requirement.domain.bo.ReqRequirementBo;
import org.ruoyi.requirement.domain.bo.ReqRequirementCommentBo;
import org.ruoyi.requirement.domain.bo.ReqRequirementStatusBo;
import org.ruoyi.requirement.domain.vo.ReqRequirementCommentVo;
import org.ruoyi.requirement.domain.vo.ReqRequirementHistoryVo;
import org.ruoyi.requirement.domain.vo.ReqRequirementVo;
import org.ruoyi.requirement.service.IReqRequirementService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/requirement/item")
public class ReqRequirementController extends BaseController {

    private final IReqRequirementService requirementService;

    @SaCheckPermission("requirement:item:list")
    @GetMapping("/list")
    public TableDataInfo<ReqRequirementVo> list(ReqRequirementBo bo, PageQuery pageQuery) {
        return requirementService.queryPageList(bo, pageQuery);
    }

    @SaCheckPermission("requirement:item:export")
    @Log(title = "需求项", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(ReqRequirementBo bo, HttpServletResponse response) {
        List<ReqRequirementVo> list = requirementService.queryList(bo);
        ExcelUtil.exportExcel(list, "需求项", ReqRequirementVo.class, response);
    }

    @SaCheckPermission("requirement:item:query")
    @GetMapping("/{id}")
    public R<ReqRequirementVo> getInfo(@NotNull(message = "主键不能为空") @PathVariable Long id) {
        return R.ok(requirementService.queryById(id));
    }

    @SaCheckPermission("requirement:item:add")
    @Log(title = "需求项", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping
    public R<Void> add(@Validated(AddGroup.class) @RequestBody ReqRequirementBo bo) {
        return toAjax(requirementService.insertByBo(bo));
    }

    @SaCheckPermission("requirement:item:edit")
    @Log(title = "需求项", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody ReqRequirementBo bo) {
        return toAjax(requirementService.updateByBo(bo));
    }

    @SaCheckPermission("requirement:item:edit")
    @Log(title = "需求项状态流转", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PostMapping("/changeStatus")
    public R<Void> changeStatus(@Validated @RequestBody ReqRequirementStatusBo bo) {
        return toAjax(requirementService.changeStatus(bo.getId(), bo.getStatus(), bo.getRemark()));
    }

    @SaCheckPermission("requirement:item:remove")
    @Log(title = "需求项", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空") @PathVariable Long[] ids) {
        return toAjax(requirementService.deleteWithValidByIds(List.of(ids), true));
    }

    @SaCheckPermission("requirement:comment:add")
    @Log(title = "需求评论", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping("/comment")
    public R<Void> addComment(@Validated(AddGroup.class) @RequestBody ReqRequirementCommentBo bo) {
        return toAjax(requirementService.addComment(bo));
    }

    @SaCheckPermission("requirement:comment:list")
    @GetMapping("/{id}/comments")
    public R<List<ReqRequirementCommentVo>> listComments(@NotNull(message = "主键不能为空") @PathVariable("id") Long id) {
        return R.ok(requirementService.listComments(id));
    }

    @SaCheckPermission("requirement:item:query")
    @GetMapping("/{id}/history")
    public R<List<ReqRequirementHistoryVo>> listHistory(@NotNull(message = "主键不能为空") @PathVariable("id") Long id) {
        return R.ok(requirementService.listHistory(id));
    }
}
