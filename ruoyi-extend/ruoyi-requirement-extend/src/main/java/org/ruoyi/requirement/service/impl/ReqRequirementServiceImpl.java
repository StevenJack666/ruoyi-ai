package org.ruoyi.requirement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.common.core.utils.MapstructUtils;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.common.mybatis.core.page.PageQuery;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.common.satoken.utils.LoginHelper;
import org.ruoyi.requirement.constant.RequirementStatusConstants;
import org.ruoyi.requirement.domain.ReqRequirement;
import org.ruoyi.requirement.domain.ReqRequirementComment;
import org.ruoyi.requirement.domain.ReqRequirementHistory;
import org.ruoyi.requirement.domain.bo.ReqRequirementBo;
import org.ruoyi.requirement.domain.bo.ReqRequirementCommentBo;
import org.ruoyi.requirement.domain.vo.ReqRequirementCommentVo;
import org.ruoyi.requirement.domain.vo.ReqRequirementHistoryVo;
import org.ruoyi.requirement.domain.vo.ReqRequirementVo;
import org.ruoyi.requirement.mapper.ReqRequirementCommentMapper;
import org.ruoyi.requirement.mapper.ReqRequirementHistoryMapper;
import org.ruoyi.requirement.mapper.ReqRequirementMapper;
import org.ruoyi.requirement.service.IReqRequirementService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

@RequiredArgsConstructor
@Service
public class ReqRequirementServiceImpl implements IReqRequirementService {

    private final ReqRequirementMapper requirementMapper;
    private final ReqRequirementCommentMapper commentMapper;
    private final ReqRequirementHistoryMapper historyMapper;

    @Override
    public ReqRequirementVo queryById(Long id) {
        return requirementMapper.selectVoById(id);
    }

    @Override
    public TableDataInfo<ReqRequirementVo> queryPageList(ReqRequirementBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<ReqRequirement> lqw = buildQueryWrapper(bo);
        Page<ReqRequirementVo> result = requirementMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    @Override
    public List<ReqRequirementVo> queryList(ReqRequirementBo bo) {
        return requirementMapper.selectVoList(buildQueryWrapper(bo));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean insertByBo(ReqRequirementBo bo) {
        ReqRequirement add = MapstructUtils.convert(bo, ReqRequirement.class);
        if (StringUtils.isBlank(add.getStatus())) {
            add.setStatus(RequirementStatusConstants.DRAFT);
        }
        validStatus(add.getStatus());
        if (StringUtils.isBlank(add.getReqCode())) {
            add.setReqCode("REQ-" + IdWorker.getIdStr());
        }
        boolean flag = requirementMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
            appendHistory(add.getId(), "create", "status", null, add.getStatus(), "创建需求");
        }
        return flag;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateByBo(ReqRequirementBo bo) {
        ReqRequirement current = requirementMapper.selectById(bo.getId());
        if (current == null) {
            throw new ServiceException("需求不存在");
        }
        ReqRequirement update = MapstructUtils.convert(bo, ReqRequirement.class);
        validStatus(update.getStatus());
        boolean ok = requirementMapper.updateById(update) > 0;
        if (ok) {
            if (!Objects.equals(current.getStatus(), update.getStatus())) {
                appendHistory(update.getId(), "status_change", "status", current.getStatus(), update.getStatus(), "编辑需求状态");
            }
            if (!Objects.equals(current.getTitle(), update.getTitle())) {
                appendHistory(update.getId(), "field_change", "title", current.getTitle(), update.getTitle(), "编辑需求标题");
            }
        }
        return ok;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean changeStatus(Long id, String status, String remark) {
        validStatus(status);
        ReqRequirement current = requirementMapper.selectById(id);
        if (current == null) {
            throw new ServiceException("需求不存在");
        }
        if (RequirementStatusConstants.CLOSED.equals(current.getStatus()) && RequirementStatusConstants.DRAFT.equals(status)) {
            throw new ServiceException("已关闭需求不可直接回到草稿");
        }
        ReqRequirement update = new ReqRequirement();
        update.setId(id);
        update.setStatus(status);
        boolean ok = requirementMapper.updateById(update) > 0;
        if (ok) {
            appendHistory(id, "status_change", "status", current.getStatus(), status, StringUtils.defaultIfBlank(remark, "状态流转"));
        }
        return ok;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        return requirementMapper.deleteByIds(ids) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean addComment(ReqRequirementCommentBo bo) {
        ReqRequirement req = requirementMapper.selectById(bo.getRequirementId());
        if (req == null) {
            throw new ServiceException("需求不存在");
        }
        ReqRequirementComment add = MapstructUtils.convert(bo, ReqRequirementComment.class);
        boolean ok = commentMapper.insert(add) > 0;
        if (ok) {
            appendHistory(bo.getRequirementId(), "comment", "comment", null, bo.getContent(), "新增评论");
        }
        return ok;
    }

    @Override
    public List<ReqRequirementCommentVo> listComments(Long requirementId) {
        return commentMapper.selectVoList(Wrappers.<ReqRequirementComment>lambdaQuery()
            .eq(ReqRequirementComment::getRequirementId, requirementId)
            .orderByAsc(ReqRequirementComment::getCreateTime));
    }

    @Override
    public List<ReqRequirementHistoryVo> listHistory(Long requirementId) {
        return historyMapper.selectVoList(Wrappers.<ReqRequirementHistory>lambdaQuery()
            .eq(ReqRequirementHistory::getRequirementId, requirementId)
            .orderByDesc(ReqRequirementHistory::getCreateTime));
    }

    private LambdaQueryWrapper<ReqRequirement> buildQueryWrapper(ReqRequirementBo bo) {
        LambdaQueryWrapper<ReqRequirement> lqw = Wrappers.lambdaQuery();
        lqw.eq(bo.getProjectId() != null, ReqRequirement::getProjectId, bo.getProjectId());
        lqw.eq(StringUtils.isNotBlank(bo.getReqCode()), ReqRequirement::getReqCode, bo.getReqCode());
        lqw.like(StringUtils.isNotBlank(bo.getTitle()), ReqRequirement::getTitle, bo.getTitle());
        lqw.eq(StringUtils.isNotBlank(bo.getType()), ReqRequirement::getType, bo.getType());
        lqw.eq(StringUtils.isNotBlank(bo.getPriority()), ReqRequirement::getPriority, bo.getPriority());
        lqw.eq(StringUtils.isNotBlank(bo.getStatus()), ReqRequirement::getStatus, bo.getStatus());
        lqw.eq(bo.getOwnerId() != null, ReqRequirement::getOwnerId, bo.getOwnerId());
        lqw.eq(bo.getAssigneeId() != null, ReqRequirement::getAssigneeId, bo.getAssigneeId());
        lqw.orderByDesc(ReqRequirement::getCreateTime);
        return lqw;
    }

    private void validStatus(String status) {
        if (StringUtils.isBlank(status) || !RequirementStatusConstants.ALL.contains(status)) {
            throw new ServiceException("非法需求状态: " + status);
        }
    }

    private void appendHistory(Long requirementId, String actionType, String fieldName, String oldValue, String newValue, String remark) {
        ReqRequirementHistory history = new ReqRequirementHistory();
        history.setRequirementId(requirementId);
        history.setActionType(actionType);
        history.setFieldName(fieldName);
        history.setOldValue(oldValue);
        history.setNewValue(newValue);
        history.setActionRemark(remark);
        Long userId = LoginHelper.getUserId();
        history.setOperatorId(userId);
        historyMapper.insert(history);
    }
}
