package org.ruoyi.support.service.impl;

import jakarta.annotation.Resource;
import org.ruoyi.support.mapper.UserPermissionIpMapper;
import org.ruoyi.support.domain.UserPermissionIp;
import org.ruoyi.support.service.IUserPermissionIpService;
import org.springframework.stereotype.Service;
import java.util.List;

/**
 * 授权IP白名单Service业务层处理
 *
 * @author admin
 * @date 2024-10-08
 */
@Service
public class UserPermissionIpServiceImpl implements IUserPermissionIpService {

    @Resource
    private UserPermissionIpMapper userPermissionIpMapper;

    /**
     * 查询授权IP白名单
     *
     * @param id 授权IP白名单ID
     * @return 授权IP白名单
     */
    @Override
    public UserPermissionIp selectUserPermissionIpById(Long id) {
        return userPermissionIpMapper.selectUserPermissionIpById(id);
    }

    /**
     * 查询授权IP白名单
     *
     * @param ids 授权IP白名单ID
     * @return 授权IP白名单
     */
    @Override
    public List<UserPermissionIp> selectUserPermissionIpByIds(Long[] ids) {
        return userPermissionIpMapper.selectUserPermissionIpByIds(ids);
    }

    /**
     * 查询授权IP白名单列表
     *
     * @param userPermissionIp 授权IP白名单
     * @return 授权IP白名单
     */
    @Override
    public List<UserPermissionIp> selectUserPermissionIpList(UserPermissionIp userPermissionIp) {
        return userPermissionIpMapper.selectUserPermissionIpList(userPermissionIp);
    }

    /**
     * 新增授权IP白名单
     *
     * @param userPermissionIp 授权IP白名单
     * @return 结果
     */
    @Override
    public int insertUserPermissionIp(UserPermissionIp userPermissionIp) {
        return userPermissionIpMapper.insert(userPermissionIp);
    }

    /**
     * 修改授权IP白名单
     *
     * @param userPermissionIp 授权IP白名单
     * @return 结果
     */
    @Override
    public int updateUserPermissionIp(UserPermissionIp userPermissionIp) {
        return userPermissionIpMapper.updateById(userPermissionIp);
    }

    /**
     * 批量删除授权IP白名单
     *
     * @param ids 需要删除的授权IP白名单ID
     * @return 结果
     */
    @Override
    public int deleteUserPermissionIpByIds(Long[] ids) {
        return userPermissionIpMapper.deleteUserPermissionIpByIds(ids);
    }

    /**
     * 删除授权IP白名单信息
     *
     * @param id 授权IP白名单ID
     * @return 结果
     */
    @Override
    public int deleteUserPermissionIpById(Long id) {
        return userPermissionIpMapper.deleteUserPermissionIpById(id);
    }
}
