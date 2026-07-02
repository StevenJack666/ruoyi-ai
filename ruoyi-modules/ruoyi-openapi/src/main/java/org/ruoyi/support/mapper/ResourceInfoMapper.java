package org.ruoyi.support.mapper;

import org.ruoyi.support.domain.ResourceInfo;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 资源信息Mapper接口
 *
 * @author admin
 * @date 2024-10-08
 */
@Mapper
public interface ResourceInfoMapper {

    /**
     * 查询资源信息
     *
     * @param id 资源信息ID
     * @return 资源信息
     */
    ResourceInfo selectResourceInfoById(Long id);

    /**
     * 查询资源信息
     *
     * @param ids 资源信息ID
     * @return 资源信息
     */
    List<ResourceInfo> selectResourceInfoByIds(Long[] ids);

    /**
     * 查询资源信息列表
     *
     * @param resourceInfo 资源信息
     * @return 资源信息集合
     */
    List<ResourceInfo> selectResourceInfoList(ResourceInfo resourceInfo);

    /**
     * 新增资源信息
     *
     * @param resourceInfo 资源信息
     * @return 结果
     */
    int insert(ResourceInfo resourceInfo);

    /**
     * 更新资源信息
     *
     * @param resourceInfo 资源信息
     * @return 结果
     */
    int updateById(ResourceInfo resourceInfo);

    /**
     * 删除资源信息
     *
     * @param id 资源信息ID
     * @return 结果
     */
    int deleteResourceInfoById(Long id);

    /**
     * 批量删除资源信息
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    int deleteResourceInfoByIds(Long[] ids);
}
