package org.ruoyi.workflow.workflow.node.documentParser;

import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.chat.utils.ResourceLoaderUtils;
import org.ruoyi.workflow.entity.WorkflowComponent;
import org.ruoyi.workflow.entity.WorkflowNode;
import org.ruoyi.workflow.workflow.NodeProcessResult;
import org.ruoyi.workflow.workflow.WfNodeState;
import org.ruoyi.workflow.workflow.WfState;
import org.ruoyi.workflow.workflow.data.NodeIOData;
import org.ruoyi.workflow.workflow.node.AbstractWfNode;
import org.ruoyi.workflow.workflow.node.enmus.NodeMessageTemplateEnum;

import static org.ruoyi.workflow.cosntant.AdiConstant.WorkflowConstant.NODE_PROCESS_STATUS_FAIL;
import static org.ruoyi.workflow.cosntant.AdiConstant.WorkflowConstant.NODE_PROCESS_STATUS_SUCCESS;

/**
 * 【节点】文档提取 <br/>
 * 节点内容固定格式：{"filePath":"...."}
 */
@Slf4j
public class DocumentExtractorNode extends AbstractWfNode {

    public DocumentExtractorNode(WorkflowComponent wfComponent, WorkflowNode nodeDef, WfState wfState, WfNodeState nodeState) {
        super(wfComponent, nodeDef, wfState, nodeState);
    }

    @Override
    protected NodeProcessResult onProcess() {
        try {
            String inputText = getFirstInputText();
            String filePath = inputText.replaceAll("[\\[\\]\"]", "");
            // 解析读取文件内容
            String document = ResourceLoaderUtils.load(filePath);
            // 获取节点模板提示词信息
            String nodeMessageTemplate = getNodeMessageTemplate(NodeMessageTemplateEnum.DOCUMENT_EXTRACTOR.getValue());
            // 保存成功信息且发送驱动消息事件
            String message = nodeMessageTemplate + document;
            notifyAndStoreMessage(wfState, message);
            // 创建节点参数对象
            NodeIOData nodeIOData = NodeIOData.createByText("output", "documentParser", document);
            // 添加到输出列表以便给后续节点使用
            state.getOutputs().add(nodeIOData);
            // 设置为成功状态
            state.setProcessStatus(NODE_PROCESS_STATUS_SUCCESS);
            return new NodeProcessResult();
        }catch (Exception e){
            state.setProcessStatus(NODE_PROCESS_STATUS_FAIL);
            return new NodeProcessResult();
        }
    }
}
