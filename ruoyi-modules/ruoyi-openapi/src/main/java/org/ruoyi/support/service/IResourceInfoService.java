package org.ruoyi.support.service;

import org.ruoyi.support.domain.ResourceInfo;

import java.util.List;

/**
 * 资源信息Service接口
 *
 * @author admin
 * @date 2024-10-08
 */
public interface IResourceInfoService {

    /**
     * 查询resourceInfo
     *
     * @param id resourceInfoID
     * @return resourceInfo
     */
    ResourceInfo selectResourceInfoById(Long id);

    /**
     * 查询resourceInfo
     *
     * @param ids resourceInfoID
     * @return resourceInfo
     */
    List<ResourceInfo> selectResourceInfoByIds(Long[] ids);

    /**
     * 查询resourceInfo列表
     *
     * @param resourceInfo resourceInfo
     * @return resourceInfo集合
     */
    List<ResourceInfo> selectResourceInfoList(ResourceInfo resourceInfo);

    /**
     * 新增resourceInfo
     *
     * @param resourceInfo resourceInfo
     * @return 结果
     */
    int insertResourceInfo(ResourceInfo resourceInfo);

    /**
     * 修改resourceInfo
     *
     * @param resourceInfo resourceInfo
     * @return 结果
     */
    int updateResourceInfo(ResourceInfo resourceInfo);

    /**
     * 批量删除resourceInfo
     *
     * @param ids 需要删除的resourceInfoID
     * @return 结果
     */
    int deleteResourceInfoByIds(Long[] ids);

    /**
     * 删除resourceInfo信息
     *
     * @param id resourceInfoID
     * @return 结果
     */
    int deleteResourceInfoById(Long id);
}
