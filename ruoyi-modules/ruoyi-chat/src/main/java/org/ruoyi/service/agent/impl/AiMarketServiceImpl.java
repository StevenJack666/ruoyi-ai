package org.ruoyi.service.agent.impl;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import org.ruoyi.common.core.utils.MapstructUtils;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.common.mybatis.core.page.PageQuery;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.domain.bo.agent.AiMarketBo;
import org.ruoyi.domain.entity.agent.AiMarket;
import org.ruoyi.domain.entity.agent.AiMarketSkill;
import org.ruoyi.domain.entity.agent.AiMarketTool;
import org.ruoyi.domain.vo.agent.AiMarketVo;
import org.ruoyi.mapper.agent.AiMarketMapper;
import org.ruoyi.mapper.agent.AiMarketSkillMapper;
import org.ruoyi.mapper.agent.AiMarketToolMapper;
import org.ruoyi.service.agent.IAiMarketService;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

/**
 * Agent 市场服务实现
 */
@RequiredArgsConstructor
@Service
public class AiMarketServiceImpl implements IAiMarketService {

    private final AiMarketMapper baseMapper;

    private final AiMarketToolMapper aiMarketToolMapper;

    private final AiMarketSkillMapper aiMarketSkillMapper;

    @Override
    public AiMarketVo queryById(Long id) {
        return baseMapper.selectVoById(id);
    }

    @Override
    public TableDataInfo<AiMarketVo> queryPageList(AiMarketBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<AiMarket> lqw = buildQueryWrapper(bo);
        Page<AiMarketVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    @Override
    public List<AiMarketVo> queryList(AiMarketBo bo) {
        LambdaQueryWrapper<AiMarket> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }


    @Override
    @Transactional
    public Boolean insertByBo(AiMarketBo bo) {
        AiMarket add = MapstructUtils.convert(bo, AiMarket.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            Long markId = add.getId();
            bo.setId(markId);
            handleMarketRelations(markId, bo.getToolIds(), bo.getSkillIds());
        }
        return flag;
    }

    @Override
    @Transactional
    public Boolean updateByBo(AiMarketBo bo) {
        AiMarket update = MapstructUtils.convert(bo, AiMarket.class);
        validEntityBeforeSave(update);
        boolean flag = baseMapper.updateById(update) > 0;
        if (flag) {
            Long marketId = update.getId();
            // 调用公共方法处理关联关系
            handleMarketRelations(marketId, bo.getToolIds(), bo.getSkillIds());
        }
        return flag;
    }

    @Override
    @Transactional
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (isValid) {
            // 预留业务校验逻辑

        }
        boolean flag = baseMapper.deleteByIds(ids) > 0;
        if (flag){
            deleteMarketAssociations(ids);
        }
        return flag;
    }

    /**
     * 删除智能体附属信息（Tool和Skill）
     * @param ids 智能体主键ID
     */
    private void deleteMarketAssociations(Collection<Long> ids) {
        if (CollectionUtils.isNotEmpty(ids)) {
            aiMarketToolMapper.delete(new LambdaQueryWrapper<AiMarketTool>()
                .in(AiMarketTool::getMarketId, ids));
            aiMarketSkillMapper.delete(new LambdaQueryWrapper<AiMarketSkill>()
                .eq(AiMarketSkill::getMarketId, ids));
        }
    }

    private LambdaQueryWrapper<AiMarket> buildQueryWrapper(AiMarketBo bo) {
        LambdaQueryWrapper<AiMarket> lqw = Wrappers.lambdaQuery();
        lqw.orderByDesc(AiMarket::getUpdateTime);
        lqw.like(StringUtils.isNotBlank(bo.getMarketName()), AiMarket::getMarketName, bo.getMarketName());
        lqw.eq(bo.getStatus() != null, AiMarket::getStatus, bo.getStatus());
        lqw.like(StringUtils.isNotBlank(bo.getDescription()), AiMarket::getDescription, bo.getDescription());
        return lqw;
    }

    private void validEntityBeforeSave(AiMarket entity) {
        // 预留唯一性等业务校验
    }

    /**
     * 关联Agent和工具的关系
     * @param marketId agentID
     * @param toolIds 工具ID
     */
    private void saveMarketTools(Long marketId, List<String> toolIds) {
        if (marketId == null) {
            return;
        }

        aiMarketToolMapper.delete(new LambdaQueryWrapper<AiMarketTool>()
            .eq(AiMarketTool::getMarketId, marketId));

        if (toolIds == null || toolIds.isEmpty()) {
            return;
        }

        List<AiMarketTool> marketTools = toolIds.stream()
            .filter(StringUtils::isNotBlank)
            .map(toolId -> {
                AiMarketTool marketTool = new AiMarketTool();
                marketTool.setMarketId(marketId);
                marketTool.setToolId(Long.valueOf(toolId));
                return marketTool;
            })
            .collect(Collectors.toList());

        if (!marketTools.isEmpty()) {
            aiMarketToolMapper.insertBatch(marketTools);
        }
    }


    /**
     * 关联Agent和技能的关系
     * @param marketId agentID
     * @param skillIds 技能ID
     */
    private void saveMarketSkills(Long marketId, List<String> skillIds) {
        if (marketId == null) {
            return;
        }

        aiMarketSkillMapper.delete(new LambdaQueryWrapper<AiMarketSkill>()
            .eq(AiMarketSkill::getMarketId, marketId));

        if (skillIds == null || skillIds.isEmpty()) {
            return;
        }

        List<AiMarketSkill> marketSkills = skillIds.stream()
            .filter(StringUtils::isNotBlank)
            .map(skillId -> {
                AiMarketSkill marketSkill = new AiMarketSkill();
                marketSkill.setMarketId(marketId);
                marketSkill.setSkillId(Long.valueOf(skillId));
                return marketSkill;
            })
            .collect(Collectors.toList());

        if (!marketSkills.isEmpty()) {
            aiMarketSkillMapper.insertBatch(marketSkills);
        }
    }

    /**
     * 处理市场条目与工具、技能的关联关系
     * @param marketId 市场ID
     * @param toolIds 工具ID列表
     * @param skillIds 技能ID列表
     */
    private void handleMarketRelations(Long marketId, List<String> toolIds, List<String> skillIds) {
        saveMarketTools(marketId, toolIds);
        saveMarketSkills(marketId, skillIds);
    }

}
