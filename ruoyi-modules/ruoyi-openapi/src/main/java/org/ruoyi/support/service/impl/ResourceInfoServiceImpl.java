package org.ruoyi.support.service.impl;

import jakarta.annotation.Resource;
import org.ruoyi.support.mapper.ResourceInfoMapper;
import org.ruoyi.support.domain.ResourceInfo;
import org.ruoyi.support.service.IResourceInfoService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 资源信息Service业务层处理
 *
 * @author admin
 * @date 2024-10-08
 */
@Service
public class ResourceInfoServiceImpl implements IResourceInfoService {

    @Resource
    private ResourceInfoMapper resourceInfoMapper;

    /**
     * 查询资源信息
     *
     * @param id 资源信息ID
     * @return 资源信息
     */
    @Override
    public ResourceInfo selectResourceInfoById(Long id) {
        return resourceInfoMapper.selectResourceInfoById(id);
    }

    /**
     * 查询资源信息
     *
     * @param ids 资源信息ID
     * @return 资源信息
     */
    @Override
    public List<ResourceInfo> selectResourceInfoByIds(Long[] ids) {
        return resourceInfoMapper.selectResourceInfoByIds(ids);
    }

    /**
     * 查询资源信息列表
     *
     * @param resourceInfo 资源信息
     * @return 资源信息
     */
    @Override
    public List<ResourceInfo> selectResourceInfoList(ResourceInfo resourceInfo) {
        return resourceInfoMapper.selectResourceInfoList(resourceInfo);
    }

    /**
     * 新增资源信息
     *
     * @param resourceInfo 资源信息
     * @return 结果
     */
    @Override
    public int insertResourceInfo(ResourceInfo resourceInfo) {
        return resourceInfoMapper.insert(resourceInfo);
    }

    /**
     * 修改资源信息
     *
     * @param resourceInfo 资源信息
     * @return 结果
     */
    @Override
    public int updateResourceInfo(ResourceInfo resourceInfo) {
        return resourceInfoMapper.updateById(resourceInfo);
    }

    /**
     * 批量删除资源信息
     *
     * @param ids 需要删除的资源信息ID
     * @return 结果
     */
    @Override
    public int deleteResourceInfoByIds(Long[] ids) {
        return resourceInfoMapper.deleteResourceInfoByIds(ids);
    }

    /**
     * 删除资源信息信息
     *
     * @param id 资源信息ID
     * @return 结果
     */
    @Override
    public int deleteResourceInfoById(Long id) {
        return resourceInfoMapper.deleteResourceInfoById(id);
    }
}
