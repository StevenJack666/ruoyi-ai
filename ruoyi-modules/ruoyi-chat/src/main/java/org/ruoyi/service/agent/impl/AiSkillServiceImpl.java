package org.ruoyi.service.agent.impl;

import java.util.Collection;
import java.util.List;

import org.ruoyi.common.core.utils.MapstructUtils;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.common.mybatis.core.page.PageQuery;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.domain.bo.agent.AiSkillBo;
import org.ruoyi.domain.entity.agent.AiSkill;
import org.ruoyi.domain.vo.agent.AiSkillVo;
import org.ruoyi.mapper.agent.AiSkillMapper;
import org.ruoyi.service.agent.IAiSkillService;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import lombok.RequiredArgsConstructor;

/**
 * Agent 技能服务实现
 */
@RequiredArgsConstructor
@Service
public class AiSkillServiceImpl implements IAiSkillService {

    private final AiSkillMapper baseMapper;

    @Override
    public AiSkillVo queryById(Long id) {
        return baseMapper.selectVoById(id);
    }

    @Override
    public TableDataInfo<AiSkillVo> queryPageList(AiSkillBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<AiSkill> lqw = buildQueryWrapper(bo);
        Page<AiSkillVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    @Override
    public List<AiSkillVo> queryList(AiSkillBo bo) {
        LambdaQueryWrapper<AiSkill> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    @Override
    public Boolean insertByBo(AiSkillBo bo) {
        AiSkill add = MapstructUtils.convert(bo, AiSkill.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    @Override
    public Boolean updateByBo(AiSkillBo bo) {
        AiSkill update = MapstructUtils.convert(bo, AiSkill.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (isValid) {
            // 预留业务校验逻辑
        }
        return baseMapper.deleteByIds(ids) > 0;
    }

    private LambdaQueryWrapper<AiSkill> buildQueryWrapper(AiSkillBo bo) {
        LambdaQueryWrapper<AiSkill> lqw = Wrappers.lambdaQuery();
        lqw.orderByDesc(AiSkill::getUpdateTime);
        lqw.like(StringUtils.isNotBlank(bo.getSkillName()), AiSkill::getSkillName, bo.getSkillName());
        lqw.like(StringUtils.isNotBlank(bo.getSkillCode()), AiSkill::getSkillCode, bo.getSkillCode());
        lqw.like(StringUtils.isNotBlank(bo.getDescription()), AiSkill::getDescription, bo.getDescription());
        return lqw;
    }

    private void validEntityBeforeSave(AiSkill entity) {
        // 预留唯一性等业务校验
    }
}
