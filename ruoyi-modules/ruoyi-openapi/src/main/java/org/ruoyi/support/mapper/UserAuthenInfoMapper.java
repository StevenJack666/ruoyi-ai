package org.ruoyi.support.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.ruoyi.support.domain.UserAuthenInfo;

import java.util.List;

/**
 * 授权应用信息Mapper接口
 *
 * @author admin
 * @date 2024-10-08
 */
@Mapper
public interface UserAuthenInfoMapper {

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
    int insert(UserAuthenInfo userAuthenInfo);

    /**
     * 更新授权应用信息
     *
     * @param userAuthenInfo 授权应用信息
     * @return 结果
     */
    int updateById(UserAuthenInfo userAuthenInfo);

    /**
     * 删除授权应用信息
     *
     * @param id 授权应用信息ID
     * @return 结果
     */
    int deleteUserAuthenInfoById(Long id);

    /**
     * 批量删除授权应用信息
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    int deleteUserAuthenInfoByIds(Long[] ids);
}
