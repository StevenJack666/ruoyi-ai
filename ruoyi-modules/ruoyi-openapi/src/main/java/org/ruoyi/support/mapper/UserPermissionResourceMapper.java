package org.ruoyi.support.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.ruoyi.support.domain.UserPermissionResource;

import java.util.List;

/**
 * 资源授权信息Mapper接口
 *
 * @author admin
 * @date 2024-10-08
 */
@Mapper
public interface UserPermissionResourceMapper {

    /**
     * 查询资源授权信息
     *
     * @param id 资源授权信息ID
     * @return 资源授权信息
     */
    UserPermissionResource selectUserPermissionResourceById(Long id);

    /**
     * 查询资源授权信息
     *
     * @param ids 资源授权信息ID
     * @return 资源授权信息
     */
    List<UserPermissionResource> selectUserPermissionResourceByIds(Long[] ids);

    /**
     * 查询资源授权信息列表
     *
     * @param userPermissionResource 资源授权信息
     * @return 资源授权信息集合
     */
    List<UserPermissionResource> selectUserPermissionResourceList(UserPermissionResource userPermissionResource);

    /**
     * 新增资源授权信息
     *
     * @param userPermissionResource 资源授权信息
     * @return 结果
     */
    int insert(UserPermissionResource userPermissionResource);

    /**
     * 更新资源授权信息
     *
     * @param userPermissionResource 资源授权信息
     * @return 结果
     */
    int updateById(UserPermissionResource userPermissionResource);

    /**
     * 删除资源授权信息
     *
     * @param id 资源授权信息ID
     * @return 结果
     */
    int deleteUserPermissionResourceById(Long id);

    /**
     * 批量删除资源授权信息
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    int deleteUserPermissionResourceByIds(Long[] ids);
}
