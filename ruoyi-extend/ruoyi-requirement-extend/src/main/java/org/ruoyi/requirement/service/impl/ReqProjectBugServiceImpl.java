package org.ruoyi.requirement.service.impl;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.common.core.utils.MapstructUtils;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.common.mybatis.core.page.PageQuery;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.requirement.constant.ProjectBugStatusConstants;
import org.ruoyi.requirement.domain.ReqProjectBug;
import org.ruoyi.requirement.domain.bo.ReqProjectBugBo;
import org.ruoyi.requirement.domain.vo.ReqProjectBugVo;
import org.ruoyi.requirement.mapper.ReqProjectBugMapper;
import org.ruoyi.requirement.service.IReqProjectBugService;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class ReqProjectBugServiceImpl implements IReqProjectBugService {

    private final ReqProjectBugMapper bugMapper;

    @Override
    public ReqProjectBugVo queryById(Long id) {
        return bugMapper.selectVoById(id);
    }

    @Override
    public TableDataInfo<ReqProjectBugVo> queryPageList(ReqProjectBugBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<ReqProjectBug> lqw = buildQueryWrapper(bo);
        Page<ReqProjectBugVo> result = bugMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    @Override
    public List<ReqProjectBugVo> queryList(ReqProjectBugBo bo) {
        return bugMapper.selectVoList(buildQueryWrapper(bo));
    }

    @Override
    public Boolean insertByBo(ReqProjectBugBo bo) {
        ReqProjectBug add = MapstructUtils.convert(bo, ReqProjectBug.class);
        if (StringUtils.isBlank(add.getStatus())) {
            add.setStatus(ProjectBugStatusConstants.OPEN);
        }
        validStatus(add.getStatus());
        if (StringUtils.isBlank(add.getBugCode())) {
            add.setBugCode("BUG-" + IdWorker.getIdStr());
        }
        boolean ok = bugMapper.insert(add) > 0;
        if (ok) {
            bo.setId(add.getId());
        }
        return ok;
    }

    @Override
    public Boolean updateByBo(ReqProjectBugBo bo) {
        ReqProjectBug current = bugMapper.selectById(bo.getId());
        if (current == null) {
            throw new ServiceException("Bug不存在");
        }
        ReqProjectBug update = MapstructUtils.convert(bo, ReqProjectBug.class);
        if (StringUtils.isNotBlank(update.getStatus())) {
            validStatus(update.getStatus());
            if (ProjectBugStatusConstants.OPEN.equals(current.getStatus())
                && ProjectBugStatusConstants.CLOSED.equals(update.getStatus())) {
                throw new ServiceException("未处理的Bug不可直接关闭");
            }
        }
        if (Objects.equals(update.getStatus(), ProjectBugStatusConstants.RESOLVED)
            && update.getResolvedTime() == null) {
            update.setResolvedTime(new java.util.Date());
        }
        return bugMapper.updateById(update) > 0;
    }

    @Override
    public Boolean changeStatus(Long id, String status, String remark) {
        validStatus(status);
        ReqProjectBug current = bugMapper.selectById(id);
        if (current == null) {
            throw new ServiceException("Bug不存在");
        }
        if (ProjectBugStatusConstants.OPEN.equals(current.getStatus())
            && ProjectBugStatusConstants.CLOSED.equals(status)) {
            throw new ServiceException("未处理的Bug不可直接关闭");
        }
        ReqProjectBug update = new ReqProjectBug();
        update.setId(id);
        update.setStatus(status);
        if (ProjectBugStatusConstants.RESOLVED.equals(status)) {
            update.setResolvedTime(new java.util.Date());
        }
        update.setRemark(StringUtils.defaultIfBlank(remark, current.getRemark()));
        return bugMapper.updateById(update) > 0;
    }

    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        return bugMapper.deleteByIds(ids) > 0;
    }

    private LambdaQueryWrapper<ReqProjectBug> buildQueryWrapper(ReqProjectBugBo bo) {
        LambdaQueryWrapper<ReqProjectBug> lqw = Wrappers.lambdaQuery();
        lqw.eq(bo.getProjectId() != null, ReqProjectBug::getProjectId, bo.getProjectId());
        lqw.eq(StringUtils.isNotBlank(bo.getBugCode()), ReqProjectBug::getBugCode, bo.getBugCode());
        lqw.like(StringUtils.isNotBlank(bo.getTitle()), ReqProjectBug::getTitle, bo.getTitle());
        lqw.eq(StringUtils.isNotBlank(bo.getSeverity()), ReqProjectBug::getSeverity, bo.getSeverity());
        lqw.eq(StringUtils.isNotBlank(bo.getPriority()), ReqProjectBug::getPriority, bo.getPriority());
        lqw.eq(StringUtils.isNotBlank(bo.getStatus()), ReqProjectBug::getStatus, bo.getStatus());
        lqw.eq(bo.getOwnerId() != null, ReqProjectBug::getOwnerId, bo.getOwnerId());
        lqw.eq(bo.getAssigneeId() != null, ReqProjectBug::getAssigneeId, bo.getAssigneeId());
        lqw.orderByDesc(ReqProjectBug::getCreateTime);
        return lqw;
    }

    private void validStatus(String status) {
        if (StringUtils.isBlank(status) || !ProjectBugStatusConstants.ALL.contains(status)) {
            throw new ServiceException("非法Bug状态: " + status);
        }
    }
}
