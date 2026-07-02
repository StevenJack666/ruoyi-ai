package org.ruoyi.support.service;


import org.ruoyi.support.domain.UserAuthenInfo;

import java.util.List;

/**
 * 授权应用信息Service接口
 *
 * @author admin
 * @date 2024-10-08
 */
public interface IUserAuthenInfoService {

    /**
     * 查询授权应用信息
     *
     * @param id 授权应用信息ID
     * @return 授权应用信息
     */
    UserAuthenInfo selectUserAuthenInfoById(Long id);

    /**
     * 查询授权应用信息
     *
     * @param ids 授权应用信息ID
     * @return 授权应用信息
     */
    List<UserAuthenInfo> selectUserAuthenInfoByIds(Long[] ids);

    /**
     * 查询授权应用信息列表
     *
     * @param userAuthenInfo 授权应用信息
     * @return 授权应用信息集合
     */
    List<UserAuthenInfo> selectUserAuthenInfoList(UserAuthenInfo userAuthenInfo);

    /**
     * 新增授权应用信息
     *
     * @param userAuthenInfo 授权应用信息
     * @return 结果
     */
    int insertUserAuthenInfo(UserAuthenInfo userAuthenInfo);

    /**
     * 一键生成授权应用信息
     *
     * @return 结果
     */
    int createUserAuthenInfo();

    /**
     * 修改授权应用信息
     *
     * @param userAuthenInfo 授权应用信息
     * @return 结果
     */
    int updateUserAuthenInfo(UserAuthenInfo userAuthenInfo);

    /**
     * 批量删除授权应用信息
     *
     * @param ids 需要删除的授权应用信息ID
     * @return 结果
     */
    int deleteUserAuthenInfoByIds(Long[] ids);

    /**
     * 删除授权应用信息信息
     *
     * @param id 授权应用信息ID
     * @return 结果
     */
    int deleteUserAuthenInfoById(Long id);
}
