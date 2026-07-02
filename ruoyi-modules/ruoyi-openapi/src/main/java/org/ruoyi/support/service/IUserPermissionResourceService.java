package org.ruoyi.support.service;

import org.ruoyi.support.domain.UserPermissionResource;

import java.util.List;

/**
 * 资源授权信息Service接口
 *
 * @author admin
 * @date 2024-10-08
 */
public interface IUserPermissionResourceService {

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
    int insertUserPermissionResource(UserPermissionResource userPermissionResource);

    /**
     * 修改资源授权信息
     *
     * @param userPermissionResource 资源授权信息
     * @return 结果
     */
    int updateUserPermissionResource(UserPermissionResource userPermissionResource);

    /**
     * 批量删除资源授权信息
     *
     * @param ids 需要删除的资源授权信息ID
     * @return 结果
     */
    int deleteUserPermissionResourceByIds(Long[] ids);

    /**
     * 删除资源授权信息信息
     *
     * @param id 资源授权信息ID
     * @return 结果
     */
    int deleteUserPermissionResourceById(Long id);
}
