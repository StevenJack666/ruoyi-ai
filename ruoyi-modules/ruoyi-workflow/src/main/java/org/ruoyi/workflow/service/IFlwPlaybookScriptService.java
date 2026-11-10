package org.ruoyi.workflow.service;

import org.ruoyi.workflow.domain.PlaybookScript;

/**
 * 剧本节点脚本映射 Service
 *
 * @author ruoyi
 */
public interface IFlwPlaybookScriptService {

    /**
     * 按流程编码与节点编码查询脚本配置
     *
     * @param flowCode 流程编码
     * @param nodeCode 节点编码
     * @return 脚本配置，不存在返回 null
     */
    PlaybookScript getByNode(String flowCode, String nodeCode);

    /**
     * 判断节点是否为已启用的脚本节点
     *
     * @param flowCode 流程编码
     * @param nodeCode 节点编码
     * @return true 表示该节点需要派发脚本
     */
    boolean isScriptNode(String flowCode, String nodeCode);

}
