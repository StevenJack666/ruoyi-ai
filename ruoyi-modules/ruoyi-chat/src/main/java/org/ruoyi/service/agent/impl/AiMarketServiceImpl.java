package org.ruoyi.service.agent.impl;

import java.util.Collection;
import java.util.List;

import org.ruoyi.common.core.utils.MapstructUtils;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.common.mybatis.core.page.PageQuery;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.domain.bo.agent.AiMarketBo;
import org.ruoyi.domain.entity.agent.AiMarket;
import org.ruoyi.domain.vo.agent.AiMarketVo;
import org.ruoyi.mapper.agent.AiMarketMapper;
import org.ruoyi.service.agent.IAiMarketService;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import lombok.RequiredArgsConstructor;

/**
 * Agent 市场服务实现
 */
@RequiredArgsConstructor
@Service
public class AiMarketServiceImpl implements IAiMarketService {

    private final AiMarketMapper baseMapper;

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
    public Boolean insertByBo(AiMarketBo bo) {
        AiMarket add = MapstructUtils.convert(bo, AiMarket.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    @Override
    public Boolean updateByBo(AiMarketBo bo) {
        AiMarket update = MapstructUtils.convert(bo, AiMarket.class);
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
}
