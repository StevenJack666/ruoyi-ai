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
import org.ruoyi.domain.bo.agent.AiMarketBo;
import org.ruoyi.domain.vo.agent.AiMarketVo;
import org.ruoyi.service.agent.IAiMarketService;
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
 * Agent 市场管理
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/agent/market")
public class AiMarketController extends BaseController {

    private final IAiMarketService aiMarketService;

    @SaCheckPermission("agent:market:list")
    @GetMapping("/list")
    public TableDataInfo<AiMarketVo> list(AiMarketBo bo, PageQuery pageQuery) {
        return aiMarketService.queryPageList(bo, pageQuery);
    }

    @GetMapping("/all")
    public R<List<AiMarketVo>> all(AiMarketBo bo) {
        return R.ok(aiMarketService.queryList(bo));
    }

    @SaCheckPermission("agent:market:export")
    @Log(title = "Agent市场", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(AiMarketBo bo, HttpServletResponse response) {
        List<AiMarketVo> list = aiMarketService.queryList(bo);
        ExcelUtil.exportExcel(list, "Agent市场", AiMarketVo.class, response);
    }

    @SaCheckPermission("agent:market:query")
    @GetMapping("/{id}")
    public R<AiMarketVo> getInfo(@NotNull(message = "主键不能为空") @PathVariable Long id) {
        return R.ok(aiMarketService.queryById(id));
    }

    @SaCheckPermission("agent:market:add")
    @Log(title = "Agent市场", businessType = BusinessType.INSERT)
    @RepeatSubmit
    @PostMapping
    public R<Void> add(@Validated(AddGroup.class) @RequestBody AiMarketBo bo) {
        return toAjax(aiMarketService.insertByBo(bo));
    }

    @SaCheckPermission("agent:market:edit")
    @Log(title = "Agent市场", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody AiMarketBo bo) {
        return toAjax(aiMarketService.updateByBo(bo));
    }

    @SaCheckPermission("agent:market:remove")
    @Log(title = "Agent市场", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空") @PathVariable Long[] ids) {
        return toAjax(aiMarketService.deleteWithValidByIds(List.of(ids), true));
    }
}
