package org.ruoyi.workflow.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.warm.flow.core.entity.Definition;
import org.dromara.warm.flow.core.entity.Instance;
import org.dromara.warm.flow.core.entity.Node;
import org.dromara.warm.flow.core.entity.Task;
import org.dromara.warm.flow.core.listener.Listener;
import org.dromara.warm.flow.core.listener.ListenerVariable;
import org.ruoyi.workflow.common.ConditionalOnEnable;
import org.ruoyi.workflow.domain.PlaybookScript;
import org.ruoyi.workflow.service.IFlwPlaybookScriptService;
import org.ruoyi.workflow.service.impl.ScriptDispatchService;
import org.springframework.stereotype.Component;

/**
 * 剧本脚本派发监听器
 * <p>
 * 挂载方式：在设计器「监听器」页签中配置
 * <pre>
 *     类型：create（任务创建时）
 *     路径：org.ruoyi.workflow.listener.ScriptDispatchListener
 * </pre>
 * <p>
 * 注意：路径必须是<b>类的全限定名</b>。warm-flow 内部用 {@code Class.forName(listenerPath)}
 * 解析监听器，填 Spring Bean 名（如 {@code scriptDispatchListener}）会抛
 * {@code ClassNotFoundException} 并被静默吞掉，表现为监听器完全不执行。
 * <p>
 * 职责边界：只把待派发任务写入 outbox，<b>禁止在此调用外部 HTTP</b>。
 * 监听器运行在流程事务内，同步调用分钟级脚本会长时间占用数据库连接并放大回滚范围。
 *
 * @author ruoyi
 */
@ConditionalOnEnable
@Slf4j
@Component("scriptDispatchListener")
@RequiredArgsConstructor
public class ScriptDispatchListener implements Listener {

    private final IFlwPlaybookScriptService playbookScriptService;
    private final ScriptDispatchService scriptDispatchService;

    @Override
    public void notify(ListenerVariable listenerVariable) {
        Node node = listenerVariable.getNode();
        Instance instance = listenerVariable.getInstance();
        Definition definition = listenerVariable.getDefinition();
        Task task = listenerVariable.getTask();

        // 不同监听阶段填充的字段不同，先做完整性校验，避免空指针
        if (
            node == null ||
            instance == null ||
            definition == null ||
            task == null
        ) {
            log.warn(
                "[剧本派发] 监听器上下文不完整，跳过。node={}, instance={}, definition={}, task={}",
                node,
                instance,
                definition,
                task
            );
            return;
        }

        // 注意：Instance 接口没有 flowCode 字段，必须通过 Definition 获取
        String flowCode = definition.getFlowCode();
        String nodeCode = node.getNodeCode();

        PlaybookScript config = playbookScriptService.getByNode(
            flowCode,
            nodeCode
        );
        if (config == null) {
            // 监听到了节点但查不到剧本映射：脚本节点就是漏配，流程会停在该节点。
            // 用 warn 而不是 debug，避免静默卡死时日志里什么都查不到
            log.warn(
                "[剧本派发] 节点未配置剧本映射，脚本不会执行, flowCode={}, nodeCode={}, listenerPath={}",
                flowCode,
                nodeCode,
                node.getListenerPath()
            );
            return;
        }
        if (!Boolean.TRUE.equals(config.getEnabled())) {
            // 已停用的映射不会派发，流程会停在该节点，这里显式报错便于排查
            log.error(
                "[剧本派发] 剧本映射已停用，流程将停在该节点, flowCode={}, nodeCode={}, script={}",
                flowCode,
                nodeCode,
                config.getScriptPath()
            );
            return;
        }

        String runId = scriptDispatchService.enqueue(
            config,
            instance,
            task,
            listenerVariable.getVariable()
        );
        log.info(
            "[剧本派发] 监听器处理完成, runId={}, flowCode={}, nodeCode={}, businessId={}",
            runId,
            flowCode,
            nodeCode,
            instance.getBusinessId()
        );
    }
}
