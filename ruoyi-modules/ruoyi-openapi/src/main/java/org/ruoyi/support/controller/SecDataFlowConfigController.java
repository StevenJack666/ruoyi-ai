package org.ruoyi.support.controller;

import jakarta.annotation.Resource;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.config.RateLimitStorage;
import org.ruoyi.support.domain.DataFlowConfig;
import org.ruoyi.support.service.IDataFlowConfigService;
import org.springframework.web.bind.annotation.*;
import org.ruoyi.support.page.TableDataInfo;

import java.util.List;

/**
 * 暴露面openApi流控配置Controller
 *
 * @author admin
 * @date 2024-11-19
 */
@RestController
@RequestMapping("/secApi/dataFlowConfig")
public class SecDataFlowConfigController extends BaseController {

    @Resource
    private IDataFlowConfigService dataFlowConfigService;

    @Resource
    private RateLimitStorage rateLimitStorage;

    /**
     * 查询暴露面openApi流控配置列表
     */
    @GetMapping("/list")
    public TableDataInfo list(DataFlowConfig dataFlowConfig) {
        startPage();
        List<DataFlowConfig> list = dataFlowConfigService.selectDataFlowConfigList(dataFlowConfig);
        return getDataTable(list);
    }

    /**
     * 获取暴露面openApi流控配置详细信息
     */
    @GetMapping(value = "/{id}")
    public R<?> getInfo(@PathVariable("id") Long id) {
        return R.ok(dataFlowConfigService.selectDataFlowConfigById(id));
    }

    /**
     * 新增暴露面openApi流控配置
     */
    @PostMapping
    public R<?> add(@RequestBody DataFlowConfig dataFlowConfig) {
        return toAjax(dataFlowConfigService.insertDataFlowConfig(dataFlowConfig));
    }

    /**
     * 修改暴露面openApi流控配置
     */
    @PostMapping("/edit")
    public R<?> edit(@RequestBody DataFlowConfig dataFlowConfig) {
        return toAjax(dataFlowConfigService.updateDataFlowConfig(dataFlowConfig));
    }

    /**
     * 删除暴露面openApi流控配置
     */
    @DeleteMapping("/{ids}")
    public R<?> remove(@PathVariable Long[] ids) {
        return toAjax(dataFlowConfigService.deleteDataFlowConfigByIds(ids));
    }

    /**
     * 启用禁用暴露面openApi参数配置
     */
    @PostMapping("/editEnableFlagById")
    public R<?> editEnableFlagById(@RequestBody DataFlowConfig dataFlowConfig){
        return toAjax(dataFlowConfigService.updateEnableFlagById(dataFlowConfig));
    }

    /**
     * 清空本地缓存
     * @return 执行结果
     */
    @PostMapping("/invalidateAll")
    public R<?> invalidateAll() {
        //清空本地缓存，所有机器缓存清空通过Nginx轮询清空
        rateLimitStorage.invalidateAllFlowConfig();
        return R.ok();
    }
}
