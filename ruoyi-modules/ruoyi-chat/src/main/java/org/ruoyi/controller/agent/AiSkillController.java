package org.ruoyi.controller.agent;

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
import org.ruoyi.domain.bo.agent.AiSkillBo;
import org.ruoyi.domain.vo.agent.AiSkillVo;
import org.ruoyi.service.agent.IAiSkillService;
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
 * Agent 技能管理
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/agent/skill")
public class AiSkillController extends BaseController {

    private final IAiSkillService aiSkillService;

    @SaCheckPermission("agent:skill:list")
    @GetMapping("/list")
    public TableDataInfo<AiSkillVo> list(AiSkillBo bo, PageQuery pageQuery) {
        return aiSkillService.queryPageList(bo, pageQuery);
    }

    @GetMapping("/all")
    public R<List<AiSkillVo>> all(AiSkillBo bo) {
        return R.ok(aiSkillService.queryList(bo));
    }

    @SaCheckPermission("agent:skill:export")
    @Log(title = "Agent技能", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(AiSkillBo bo, HttpServletResponse response) {
        List<AiSkillVo> list = aiSkillService.queryList(bo);
        ExcelUtil.exportExcel(list, "Agent技能", AiSkillVo.class, response);
    }

    @SaCheckPermission("agent:skill:query")
    @GetMapping("/{id}")
    public R<AiSkillVo> getInfo(@NotNull(message = "主键不能为空") @PathVariable Long id) {
        return R.ok(aiSkillService.queryById(id));
    }

    @SaCheckPermission("agent:skill:add")
    @Log(title = "Agent技能", businessType = BusinessType.INSERT)
    @RepeatSubmit
    @PostMapping
    public R<Void> add(@Validated(AddGroup.class) @RequestBody AiSkillBo bo) {
        return toAjax(aiSkillService.insertByBo(bo));
    }

    @SaCheckPermission("agent:skill:edit")
    @Log(title = "Agent技能", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody AiSkillBo bo) {
        return toAjax(aiSkillService.updateByBo(bo));
    }

    @SaCheckPermission("agent:skill:remove")
    @Log(title = "Agent技能", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空") @PathVariable Long[] ids) {
        return toAjax(aiSkillService.deleteWithValidByIds(List.of(ids), true));
    }
}
