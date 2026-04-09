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
import org.ruoyi.requirement.domain.bo.ReqNoteBo;
import org.ruoyi.requirement.domain.vo.ReqNoteAttachmentVo;
import org.ruoyi.requirement.domain.vo.ReqNoteVo;
import org.ruoyi.requirement.service.IReqNoteService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;

/**
 * 记事控制器
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/requirement/note")
public class ReqNoteController extends BaseController {

    private final IReqNoteService noteService;

    @SaCheckPermission("requirement:note:list")
    @GetMapping("/list")
    public TableDataInfo<ReqNoteVo> list(ReqNoteBo bo, PageQuery pageQuery) {
        return noteService.queryPageList(bo, pageQuery);
    }

    @SaCheckPermission("requirement:note:export")
    @Log(title = "项目记事", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(ReqNoteBo bo, HttpServletResponse response) {
        List<ReqNoteVo> list = noteService.queryList(bo);
        ExcelUtil.exportExcel(list, "项目记事", ReqNoteVo.class, response);
    }

    @SaCheckPermission("requirement:note:query")
    @GetMapping("/{id}")
    public R<ReqNoteVo> getInfo(@NotNull(message = "主键不能为空") @PathVariable Long id) {
        return R.ok(noteService.queryById(id));
    }

    @SaCheckPermission("requirement:note:add")
    @Log(title = "项目记事", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping
    public R<Void> add(@Validated(AddGroup.class) @RequestBody ReqNoteBo bo) {
        return toAjax(noteService.insertByBo(bo));
    }

    @SaCheckPermission("requirement:note:edit")
    @Log(title = "项目记事", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody ReqNoteBo bo) {
        return toAjax(noteService.updateByBo(bo));
    }

    @SaCheckPermission("requirement:note:remove")
    @Log(title = "项目记事", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空") @PathVariable Long[] ids) {
        return toAjax(noteService.deleteWithValidByIds(List.of(ids), true));
    }

    @SaCheckPermission("requirement:note:edit")
    @Log(title = "记事附件关联", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping("/{id}/attachment")
    public R<ReqNoteAttachmentVo> uploadAttachment(@PathVariable("id") Long noteId,
        @RequestParam("url") String fileUrl) {
        return R.ok(noteService.uploadAttachment(noteId, fileUrl));
    }

    @SaCheckPermission("requirement:note:query")
    @GetMapping("/{id}/attachments")
    public R<List<ReqNoteAttachmentVo>> listAttachments(@PathVariable("id") Long noteId) {
        return R.ok(noteService.listAttachments(noteId));
    }

    @SaCheckPermission("requirement:note:edit")
    @Log(title = "记事附件删除", businessType = BusinessType.DELETE)
    @DeleteMapping("/attachment/{attachmentId}")
    public R<Void> removeAttachment(@PathVariable Long attachmentId) {
        return toAjax(noteService.removeAttachment(attachmentId));
    }
}
