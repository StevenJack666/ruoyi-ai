package org.ruoyi.support.service;


import org.ruoyi.support.domain.DataFlowConfig;

import java.util.List;

/**
 * 数据服务流控配置Service接口
 *
 * @author admin
 * @date 2024-11-19
 */
public interface IDataFlowConfigService {

    /**
     * 查询数据服务流控配置
     *
     * @param id 数据服务流控配置ID
     * @return 数据服务流控配置
     */
    DataFlowConfig selectDataFlowConfigById(Long id);

    /**
     * 查询数据服务流控配置
     *
     * @param ids 数据服务流控配置ID
     * @return 数据服务流控配置
     */
    List<DataFlowConfig> selectDataFlowConfigByIds(Long[] ids);

    /**
     * 查询数据服务流控配置列表
     *
     * @param DataFlowConfig 数据服务流控配置
     * @return 数据服务流控配置集合
     */
    List<DataFlowConfig> selectDataFlowConfigList(DataFlowConfig DataFlowConfig);

    /**
     * 新增数据服务流控配置
     *
     * @param DataFlowConfig 数据服务流控配置
     * @return 结果
     */
    int insertDataFlowConfig(DataFlowConfig DataFlowConfig);

    /**
     * 修改数据服务流控配置
     *
     * @param DataFlowConfig 数据服务流控配置
     * @return 结果
     */
    int updateDataFlowConfig(DataFlowConfig DataFlowConfig);

    /**
     * 批量删除数据服务流控配置
     *
     * @param ids 需要删除的数据服务流控配置ID
     * @return 结果
     */
    int deleteDataFlowConfigByIds(Long[] ids);

    /**
     * 删除数据服务流控配置信息
     *
     * @param id 数据服务流控配置ID
     * @return 结果
     */
    int deleteDataFlowConfigById(Long id);

    /**
     * 根据APPID加载数据服务流控配置列表
     * @param appId 应用ID
     * @return 流控配置列表
     */
    List<DataFlowConfig> loadCacheListByAppId(String appId);

    /**
     * 启用禁用数据服务流控配置
     *
     * @param dataFlowConfig 数据服务流控配置
     * @return 结果
     */
    int updateEnableFlagById(DataFlowConfig dataFlowConfig);

}
