package org.ruoyi.requirement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.utils.MapstructUtils;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.common.mybatis.core.page.PageQuery;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.requirement.domain.ReqProject;
import org.ruoyi.requirement.domain.bo.ReqProjectBo;
import org.ruoyi.requirement.domain.vo.ReqProjectVo;
import org.ruoyi.requirement.mapper.ReqProjectMapper;
import org.ruoyi.requirement.service.IReqProjectService;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

@RequiredArgsConstructor
@Service
public class ReqProjectServiceImpl implements IReqProjectService {

    private final ReqProjectMapper baseMapper;

    @Override
    public ReqProjectVo queryById(Long id) {
        return baseMapper.selectVoById(id);
    }

    @Override
    public TableDataInfo<ReqProjectVo> queryPageList(ReqProjectBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<ReqProject> lqw = buildQueryWrapper(bo);
        Page<ReqProjectVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    @Override
    public List<ReqProjectVo> queryList(ReqProjectBo bo) {
        return baseMapper.selectVoList(buildQueryWrapper(bo));
    }

    @Override
    public Boolean insertByBo(ReqProjectBo bo) {
        ReqProject add = MapstructUtils.convert(bo, ReqProject.class);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    @Override
    public Boolean updateByBo(ReqProjectBo bo) {
        ReqProject update = MapstructUtils.convert(bo, ReqProject.class);
        return baseMapper.updateById(update) > 0;
    }

    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        return baseMapper.deleteByIds(ids) > 0;
    }

    private LambdaQueryWrapper<ReqProject> buildQueryWrapper(ReqProjectBo bo) {
        LambdaQueryWrapper<ReqProject> lqw = Wrappers.lambdaQuery();
        lqw.eq(StringUtils.isNotBlank(bo.getProjectCode()), ReqProject::getProjectCode, bo.getProjectCode());
        lqw.like(StringUtils.isNotBlank(bo.getProjectName()), ReqProject::getProjectName, bo.getProjectName());
        lqw.eq(StringUtils.isNotBlank(bo.getStatus()), ReqProject::getStatus, bo.getStatus());
        lqw.eq(bo.getOwnerId() != null, ReqProject::getOwnerId, bo.getOwnerId());
        lqw.orderByDesc(ReqProject::getCreateTime);
        return lqw;
    }
}
