package org.ruoyi.support.service.impl;

import jakarta.annotation.Resource;
import org.ruoyi.common.core.utils.openapi.GenerateKeyUtil;
import org.ruoyi.support.mapper.UserAuthenInfoMapper;
import org.ruoyi.support.domain.UserAuthenInfo;
import org.ruoyi.support.service.IUserAuthenInfoService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 授权应用信息Service业务层处理
 *
 * @author admin
 * @date 2024-10-08
 */
@Service
public class UserAuthenInfoServiceImpl implements IUserAuthenInfoService {

    @Resource
    private UserAuthenInfoMapper userAuthenInfoMapper;

    /**
     * 查询授权应用信息
     *
     * @param id 授权应用信息ID
     * @return 授权应用信息
     */
    @Override
    public UserAuthenInfo selectUserAuthenInfoById(Long id) {
        return userAuthenInfoMapper.selectUserAuthenInfoById(id);
    }

    /**
     * 查询授权应用信息
     *
     * @param ids 授权应用信息ID
     * @return 授权应用信息
     */
    @Override
    public List<UserAuthenInfo> selectUserAuthenInfoByIds(Long[] ids) {
        return userAuthenInfoMapper.selectUserAuthenInfoByIds(ids);
    }

    /**
     * 查询授权应用信息列表
     *
     * @param userAuthenInfo 授权应用信息
     * @return 授权应用信息
     */
    @Override
    public List<UserAuthenInfo> selectUserAuthenInfoList(UserAuthenInfo userAuthenInfo) {
        return userAuthenInfoMapper.selectUserAuthenInfoList(userAuthenInfo);
    }

    /**
     * 新增授权应用信息
     *
     * @param userAuthenInfo 授权应用信息
     * @return 结果
     */
    @Override
    public int insertUserAuthenInfo(UserAuthenInfo userAuthenInfo) {
        return userAuthenInfoMapper.insert(userAuthenInfo);
    }

    /**
     * 一键生成授权应用信息
     *
     * @return 结果
     */
    @Override
    public int createUserAuthenInfo() {
        UserAuthenInfo userAuthenInfo=new UserAuthenInfo();
        Map<String,String> rstMap = GenerateKeyUtil.generateAppIdAndSecurityKey();
        userAuthenInfo.setAppId(rstMap.get("appId"));
        userAuthenInfo.setUnSecurityKey(rstMap.get("unSecurityKey"));
        userAuthenInfo.setSecurityKey(rstMap.get("securityKey"));
        return userAuthenInfoMapper.insert(userAuthenInfo);
    }

    /**
     * 修改授权应用信息
     *
     * @param userAuthenInfo 授权应用信息
     * @return 结果
     */
    @Override
    public int updateUserAuthenInfo(UserAuthenInfo userAuthenInfo) {
        String securityKey = userAuthenInfo.getSecurityKey();
        userAuthenInfo.setUnSecurityKey(GenerateKeyUtil.getInnerKey(securityKey));
        return userAuthenInfoMapper.updateById(userAuthenInfo);
    }

    /**
     * 批量删除授权应用信息
     *
     * @param ids 需要删除的授权应用信息ID
     * @return 结果
     */
    @Override
    public int deleteUserAuthenInfoByIds(Long[] ids) {
        return userAuthenInfoMapper.deleteUserAuthenInfoByIds(ids);
    }

    /**
     * 删除授权应用信息信息
     *
     * @param id 授权应用信息ID
     * @return 结果
     */
    @Override
    public int deleteUserAuthenInfoById(Long id) {
        return userAuthenInfoMapper.deleteUserAuthenInfoById(id);
    }
}
