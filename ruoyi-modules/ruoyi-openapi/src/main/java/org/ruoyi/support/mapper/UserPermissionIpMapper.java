package org.ruoyi.support.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.ruoyi.support.domain.UserPermissionIp;

import java.util.List;

/**
 * 授权IP白名单Mapper接口
 *
 * @author admin
 * @date 2024-10-08
 */
@Mapper
public interface UserPermissionIpMapper {

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
    int insert(UserPermissionIp userPermissionIp);

    /**
     * 更新授权IP白名单
     *
     * @param userPermissionIp 授权IP白名单
     * @return 结果
     */
    int updateById(UserPermissionIp userPermissionIp);

    /**
     * 删除授权IP白名单
     *
     * @param id 授权IP白名单ID
     * @return 结果
     */
    int deleteUserPermissionIpById(Long id);

    /**
     * 批量删除授权IP白名单
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    int deleteUserPermissionIpByIds(Long[] ids);
}
