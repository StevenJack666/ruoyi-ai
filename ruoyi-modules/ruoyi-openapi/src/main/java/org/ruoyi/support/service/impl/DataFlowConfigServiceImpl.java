package org.ruoyi.support.service.impl;

import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.support.domain.DataFlowConfig;
import org.ruoyi.support.mapper.DataFlowConfigMapper;
import org.ruoyi.support.service.IDataFlowConfigService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 数据服务流控配置Service业务层处理
 *
 * @author admin
 * @date 2024-11-19
 */
@Service
public class DataFlowConfigServiceImpl implements IDataFlowConfigService {

    @Resource
    private DataFlowConfigMapper dataFlowConfigMapper;

    /**
     * 查询数据服务流控配置
     *
     * @param id 数据服务流控配置ID
     * @return 数据服务流控配置
     */
    @Override
    public DataFlowConfig selectDataFlowConfigById(Long id) {
        return dataFlowConfigMapper.selectDataFlowConfigById(id);
    }

    /**
     * 查询数据服务流控配置
     *
     * @param ids 数据服务流控配置ID
     * @return 数据服务流控配置
     */
    @Override
    public List<DataFlowConfig> selectDataFlowConfigByIds(Long[] ids) {
        return dataFlowConfigMapper.selectDataFlowConfigByIds(ids);
    }

    /**
     * 查询数据服务流控配置列表
     *
     * @param dataFlowConfig 数据服务流控配置
     * @return 数据服务流控配置
     */
    @Override
    public List<DataFlowConfig> selectDataFlowConfigList(DataFlowConfig dataFlowConfig) {
        if(StringUtils.isBlank(dataFlowConfig.getIsDeleted())){
            dataFlowConfig.setIsDeleted("N");
        }
        return dataFlowConfigMapper.selectDataFlowConfigList(dataFlowConfig);
    }

    /**
     * 新增数据服务流控配置
     *
     * @param dataFlowConfig 数据服务流控配置
     * @return 结果
     */
    @Override
    public int insertDataFlowConfig(DataFlowConfig dataFlowConfig) {
        if(StringUtils.isBlank(dataFlowConfig.getAppId())){
            throw new ServiceException("应用ID不能为空!");
        }
        if(StringUtils.isBlank(dataFlowConfig.getResourceUri())){
            throw new ServiceException("资源URI不能为空!");
        }
        if(dataFlowConfig.getPermitsPer() == null || dataFlowConfig.getPermitsPer() < 0){
            throw new ServiceException("并发数不能为空或大于等于0的数字!");
        }
        if(dataFlowConfig.getWaitTimeout() != null && dataFlowConfig.getWaitTimeout() < 0){
            throw new ServiceException("等待时间大于等于0的数字!");
        }
        if(StringUtils.isBlank(dataFlowConfig.getEnableFlag())){
            dataFlowConfig.setEnableFlag("Y");
        }
        if(StringUtils.isBlank(dataFlowConfig.getIsDeleted())){
            dataFlowConfig.setIsDeleted("N");
        }
        if(dataFlowConfig.getCreateTime() == null){
            dataFlowConfig.setCreateTime(new Date());
        }
        if(dataFlowConfig.getUpdateTime() == null){
            dataFlowConfig.setUpdateTime(new Date());
        }
        return dataFlowConfigMapper.insert(dataFlowConfig);
    }

    /**
     * 修改数据服务流控配置
     *
     * @param dataFlowConfig 数据服务流控配置
     * @return 结果
     */
    @Override
    public int updateDataFlowConfig(DataFlowConfig dataFlowConfig) {
        if (dataFlowConfig == null || dataFlowConfig.getId() == null || dataFlowConfig.getId() == 0) {
            return 0;
        }
        if(StringUtils.isBlank(dataFlowConfig.getAppId())){
            throw new ServiceException("应用ID不能为空!");
        }
        if(StringUtils.isBlank(dataFlowConfig.getResourceUri())){
            throw new ServiceException("资源URI不能为空!");
        }
        if(dataFlowConfig.getPermitsPer() == null || dataFlowConfig.getPermitsPer() < 0){
            throw new ServiceException("并发数不能为空或大于等于0的数字!");
        }
        if(dataFlowConfig.getWaitTimeout() != null && dataFlowConfig.getWaitTimeout() < 0){
            throw new ServiceException("等待时间大于等于0的数字!");
        }
        if(StringUtils.isBlank(dataFlowConfig.getEnableFlag())){
            dataFlowConfig.setEnableFlag("Y");
        }
        if(StringUtils.isBlank(dataFlowConfig.getIsDeleted())){
            dataFlowConfig.setIsDeleted("N");
        }
        if(dataFlowConfig.getCreateTime() == null){
            dataFlowConfig.setCreateTime(new Date());
        }
        if(dataFlowConfig.getUpdateTime() == null){
            dataFlowConfig.setUpdateTime(new Date());
        }
        if(dataFlowConfig.getWaitTimeout() == null){
            dataFlowConfig.setWaitTimeout(0L);
        }
        return dataFlowConfigMapper.updateById(dataFlowConfig);
    }

    /**
     * 批量删除数据服务流控配置
     *
     * @param ids 需要删除的数据服务流控配置ID
     * @return 结果
     */
    @Override
    public int deleteDataFlowConfigByIds(Long[] ids) {
        return dataFlowConfigMapper.deleteDataFlowConfigByIds(ids);
    }

    /**
     * 删除数据服务流控配置信息
     *
     * @param id 数据服务流控配置ID
     * @return 结果
     */
    @Override
    public int deleteDataFlowConfigById(Long id) {
        return dataFlowConfigMapper.deleteDataFlowConfigById(id);
    }

    /**
     * 根据APPID加载数据服务流控配置列表
     * @param appId 应用ID
     * @return 流控配置列表
     */
    @Override
    public List<DataFlowConfig> loadCacheListByAppId(String appId){
        if(StringUtils.isBlank(appId)){
            return null;
        }
        DataFlowConfig reqDataFlowConfig = new DataFlowConfig();
        reqDataFlowConfig.setAppId(appId);
        reqDataFlowConfig.setIsDeleted("N");
        reqDataFlowConfig.setEnableFlag("Y");
        List<DataFlowConfig> resultList = dataFlowConfigMapper.selectLoadCacheList(reqDataFlowConfig);
        if(resultList == null){
            resultList = new ArrayList<>();
        }
        if(resultList.size() > 0){
            resultList = resultList.stream()
                    //过滤参数值为空
                    .filter(config -> StringUtils.isNotBlank(config.getResourceUri()))
                    .filter(dataFlowConfig -> dataFlowConfig.getPermitsPer() != null && dataFlowConfig.getPermitsPer() > 0)
                    .map(config -> {
                        if(config.getWaitTimeout() == null || config.getWaitTimeout() < 0){
                            config.setWaitTimeout(0L);
                        }
                        config.setRemark(null);
                        return config;
                    })
                    //根据uri长度进行排序
                    .sorted(Comparator.comparingInt(c -> c.getResourceUri().length()))
                    .collect(Collectors.toList());
        }
        return resultList;
    }

    /**
     * 启用禁用数据服务流控配置
     *
     * @param dataFlowConfig 数据服务流控配置
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateEnableFlagById(DataFlowConfig dataFlowConfig){
        if (dataFlowConfig == null || dataFlowConfig.getId() == null || dataFlowConfig.getId() == 0) {
            return 0;
        }
        if(StringUtils.isBlank(dataFlowConfig.getEnableFlag())){
            throw new ServiceException("是否启用参数不能为空");
        }
        int result =  dataFlowConfigMapper.updateEnableFlagById(dataFlowConfig);
        return result;
    }
}
