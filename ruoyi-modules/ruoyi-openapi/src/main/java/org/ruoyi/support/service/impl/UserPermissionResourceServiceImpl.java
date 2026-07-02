package org.ruoyi.support.service.impl;

import jakarta.annotation.Resource;
import org.ruoyi.support.mapper.UserPermissionResourceMapper;
import org.ruoyi.support.domain.UserPermissionResource;
import org.ruoyi.support.service.IUserPermissionResourceService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 资源授权信息Service业务层处理
 *
 * @author admin
 * @date 2024-10-08
 */
@Service
public class UserPermissionResourceServiceImpl implements IUserPermissionResourceService {

    @Resource
    private UserPermissionResourceMapper userPermissionResourceMapper;

    /**
     * 查询资源授权信息
     *
     * @param id 资源授权信息ID
     * @return 资源授权信息
     */
    @Override
    public UserPermissionResource selectUserPermissionResourceById(Long id) {
        return userPermissionResourceMapper.selectUserPermissionResourceById(id);
    }

    /**
     * 查询资源授权信息
     *
     * @param ids 资源授权信息ID
     * @return 资源授权信息
     */
    @Override
    public List<UserPermissionResource> selectUserPermissionResourceByIds(Long[] ids) {
        return userPermissionResourceMapper.selectUserPermissionResourceByIds(ids);
    }

    /**
     * 查询资源授权信息列表
     *
     * @param userPermissionResource 资源授权信息
     * @return 资源授权信息
     */
    @Override
    public List<UserPermissionResource> selectUserPermissionResourceList(UserPermissionResource userPermissionResource) {
        return userPermissionResourceMapper.selectUserPermissionResourceList(userPermissionResource);
    }

    /**
     * 新增资源授权信息
     *
     * @param userPermissionResource 资源授权信息
     * @return 结果
     */
    @Override
    public int insertUserPermissionResource(UserPermissionResource userPermissionResource) {
        return userPermissionResourceMapper.insert(userPermissionResource);
    }

    /**
     * 修改资源授权信息
     *
     * @param userPermissionResource 资源授权信息
     * @return 结果
     */
    @Override
    public int updateUserPermissionResource(UserPermissionResource userPermissionResource) {
        return userPermissionResourceMapper.updateById(userPermissionResource);
    }

    /**
     * 批量删除资源授权信息
     *
     * @param ids 需要删除的资源授权信息ID
     * @return 结果
     */
    @Override
    public int deleteUserPermissionResourceByIds(Long[] ids) {
        return userPermissionResourceMapper.deleteUserPermissionResourceByIds(ids);
    }

    /**
     * 删除资源授权信息信息
     *
     * @param id 资源授权信息ID
     * @return 结果
     */
    @Override
    public int deleteUserPermissionResourceById(Long id) {
        return userPermissionResourceMapper.deleteUserPermissionResourceById(id);
    }
}
