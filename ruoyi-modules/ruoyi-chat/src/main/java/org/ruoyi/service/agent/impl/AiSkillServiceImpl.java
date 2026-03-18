package org.ruoyi.service.agent.impl;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.common.core.utils.MapstructUtils;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.common.mybatis.core.page.PageQuery;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.domain.bo.agent.AiSkillBo;
import org.ruoyi.domain.bo.mcp.McpToolBo;
import org.ruoyi.domain.entity.agent.AiMarket;
import org.ruoyi.domain.entity.agent.AiMarketSkill;
import org.ruoyi.domain.entity.agent.AiMarketTool;
import org.ruoyi.domain.entity.agent.AiSkill;
import org.ruoyi.domain.entity.mcp.McpTool;
import org.ruoyi.domain.vo.agent.AiSkillVo;
import org.ruoyi.mapper.agent.AiMarketMapper;
import org.ruoyi.mapper.agent.AiMarketSkillMapper;
import org.ruoyi.mapper.agent.AiSkillMapper;
import org.ruoyi.service.agent.IAiSkillService;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

/**
 * Agent 技能服务实现
 */
@RequiredArgsConstructor
@Service
public class AiSkillServiceImpl implements IAiSkillService {

    private final AiSkillMapper baseMapper;

    private final AiMarketSkillMapper aiMarketSkillMapper;
    private final AiMarketMapper aiMarketMapper;

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
    @Transactional
    public Boolean updateByBo(AiSkillBo bo) {
        AiSkill existingSkill = baseMapper.selectById(bo.getId());
        AiSkill update = MapstructUtils.convert(bo, AiSkill.class);
        validEntityBeforeSave(update);
        // 判断技能名称是否更改
        updateMarketSkillNameIfChanged(bo, existingSkill);
        return baseMapper.updateById(update) > 0;
    }

    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (isValid) {
            // 预留业务校验逻辑
            checkSkillBound(ids);
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

    /**
     * 判断技能名称是否已经发生修改
     * @param bo 参数信息
     * @param existingSkill 旧数据
     */
    private void updateMarketSkillNameIfChanged(AiSkillBo bo, AiSkill existingSkill) {
        // 判断名称是否和以往一致
        if (null == existingSkill){
            throw new ServiceException("技能不存在");
        }
        String existingSkillName = existingSkill.getSkillName();
        if (!existingSkillName.equals(bo.getSkillName())) {
            // 根据主键ID查询AiMarkSkill附属表所有相关联的智能体
            List<AiMarketSkill> relations = aiMarketSkillMapper.selectList(
                new LambdaQueryWrapper<AiMarketSkill>()
                    .select(AiMarketSkill::getMarketId)
                    .eq(AiMarketSkill::getSkillId, bo.getId())
            );

            if (CollectionUtils.isNotEmpty(relations)) {
                // 找到智能体主键ID集合
                Set<Long> marketIds = relations.stream()
                    .map(AiMarketSkill::getMarketId)
                    .collect(Collectors.toSet());

                // 根据智能体主键修改工具名称
                List<AiMarket> aiMarkets = aiMarketMapper.selectByIds(marketIds);
                if (CollectionUtils.isNotEmpty(aiMarkets)){
                    aiMarkets.forEach(market -> {
                        String configJson = market.getConfigJson();
                        market.setConfigJson(configJson.replace(existingSkill.getSkillName(), bo.getSkillName()));
                    });
                    aiMarketMapper.updateBatchById(aiMarkets);
                }
            }
        }
    }

    /**
     * 校验技能是否已经被绑定
     * @param skillIds 技能主键集合
     */
    private void checkSkillBound(Collection<Long> skillIds) {
        // 1. 一次性查询所有传入的 toolIds 是否存在于关联表中
        List<AiMarketSkill> existingRelations = aiMarketSkillMapper.selectList(
            new LambdaQueryWrapper<AiMarketSkill>()
                .select(AiMarketSkill::getMarketId)
                .in(AiMarketSkill::getSkillId, skillIds)
        );

        // 2. 如果查到了数据，说明有工具正在被引用
        if (CollectionUtils.isNotEmpty(existingRelations)) {
            // 3. 查询技能名称
            List<String> skillNames = baseMapper.selectList(
                    new LambdaQueryWrapper<AiSkill>()
                        .select(AiSkill::getSkillName)
                        .in(AiSkill::getId, skillIds)
                ).stream()
                .map(AiSkill::getSkillName)
                .filter(org.springframework.util.StringUtils::hasText)
                .toList();

            // 4. 提取出关联的市场ID
            Set<Long> marketIds = existingRelations.stream()
                .map(AiMarketSkill::getMarketId)
                .collect(Collectors.toSet());

            // 5. 一次性查询所有相关市场的名称
            List<String> marketNames = aiMarketMapper.selectList(
                    new LambdaQueryWrapper<AiMarket>()
                        .select(AiMarket::getMarketName)
                        .in(AiMarket::getId, marketIds)
                ).stream()
                .map(AiMarket::getMarketName)
                .filter(org.springframework.util.StringUtils::hasText)
                .toList();

            // 6. 抛出异常，提示用户
            throw new ServiceException("删除失败：技能名称 [" + String.join(", ", skillNames) +
                "] 已被智能体 [" + String.join(", ", marketNames) + "] 引用，无法删除");
        }
    }
}
