package org.ruoyi.support.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.ruoyi.support.domain.DataFlowConfig;

import java.util.List;

/**
 * 数据服务流控配置Mapper接口
 *
 * @author admin
 * @date 2024-11-19
 */
@Mapper
public interface DataFlowConfigMapper {

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
     * @param dataFlowConfig 数据服务流控配置
     * @return 数据服务流控配置集合
     */
    List<DataFlowConfig> selectDataFlowConfigList(DataFlowConfig dataFlowConfig);

    /**
     * 新增数据服务流控配置
     *
     * @param dataFlowConfig 数据服务流控配置
     * @return 结果
     */
    int insert(DataFlowConfig dataFlowConfig);

    /**
     * 更新数据服务流控配置
     *
     * @param dataFlowConfig 数据服务流控配置
     * @return 结果
     */
    int updateById(DataFlowConfig dataFlowConfig);

    /**
     * 删除数据服务流控配置
     *
     * @param id 数据服务流控配置ID
     * @return 结果
     */
    int deleteDataFlowConfigById(Long id);

    /**
     * 批量删除数据服务流控配置
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    int deleteDataFlowConfigByIds(Long[] ids);

    /**
     * 查询数据服务流控配置列表(加载到缓存中使用)
     *
     * @param dataFlowConfig 数据服务流控配置
     * @return 数据服务流控配置集合
     */
    List<DataFlowConfig> selectLoadCacheList(DataFlowConfig dataFlowConfig);

    /**
     * 启用禁用数据服务流控配置
     *
     * @param dataFlowConfig 数据服务流控配置
     * @return 结果
     */
    int updateEnableFlagById(DataFlowConfig dataFlowConfig);
}
