package org.ruoyi.support.service;

import org.ruoyi.support.domain.UserPermissionIp;

import java.util.List;

/**
 * 授权IP白名单Service接口
 *
 * @author admin
 * @date 2024-10-08
 */
public interface IUserPermissionIpService {

    /**
     * 查询授权IP白名单
     *
     * @param id 授权IP白名单ID
     * @return 授权IP白名单
     */
    UserPermissionIp selectUserPermissionIpById(Long id);

    /**
     * 查询授权IP白名单
     *
     * @param ids 授权IP白名单ID
     * @return 授权IP白名单
     */
    List<UserPermissionIp> selectUserPermissionIpByIds(Long[] ids);

    /**
     * 查询授权IP白名单列表
     *
     * @param userPermissionIp 授权IP白名单
     * @return 授权IP白名单集合
     */
    List<UserPermissionIp> selectUserPermissionIpList(UserPermissionIp userPermissionIp);

    /**
     * 新增授权IP白名单
     *
     * @param userPermissionIp 授权IP白名单
     * @return 结果
     */
    int insertUserPermissionIp(UserPermissionIp userPermissionIp);

    /**
     * 修改授权IP白名单
     *
     * @param userPermissionIp 授权IP白名单
     * @return 结果
     */
    int updateUserPermissionIp(UserPermissionIp userPermissionIp);

    /**
     * 批量删除授权IP白名单
     *
     * @param ids 需要删除的授权IP白名单ID
     * @return 结果
     */
    int deleteUserPermissionIpByIds(Long[] ids);

    /**
     * 删除授权IP白名单信息
     *
     * @param id 授权IP白名单ID
     * @return 结果
     */
    int deleteUserPermissionIpById(Long id);
}
