package org.ruoyi.workflow.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.workflow.common.ConditionalOnEnable;
import org.ruoyi.workflow.domain.PlaybookScript;
import org.ruoyi.workflow.mapper.PlaybookScriptMapper;
import org.ruoyi.workflow.service.IFlwPlaybookScriptService;
import org.springframework.stereotype.Service;

/**
 * 剧本节点脚本映射 Service 实现
 *
 * @author ruoyi
 */
@ConditionalOnEnable
@Slf4j
@RequiredArgsConstructor
@Service
public class FlwPlaybookScriptServiceImpl implements IFlwPlaybookScriptService {

    private final PlaybookScriptMapper playbookScriptMapper;

    @Override
    public PlaybookScript getByNode(String flowCode, String nodeCode) {
        if (StringUtils.isBlank(flowCode) || StringUtils.isBlank(nodeCode)) {
            return null;
        }
        return playbookScriptMapper.selectOne(
            Wrappers.<PlaybookScript>lambdaQuery()
                .eq(PlaybookScript::getFlowCode, flowCode)
                .eq(PlaybookScript::getNodeCode, nodeCode)
                .last("limit 1")
        );
    }

    @Override
    public boolean isScriptNode(String flowCode, String nodeCode) {
        PlaybookScript config = getByNode(flowCode, nodeCode);
        return config != null && Boolean.TRUE.equals(config.getEnabled());
    }

}
