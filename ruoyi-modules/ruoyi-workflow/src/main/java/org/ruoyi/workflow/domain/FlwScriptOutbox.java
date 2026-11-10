package org.ruoyi.workflow.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serial;
import java.util.Date;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.common.tenant.core.TenantEntity;

/**
 * 脚本派发 outbox t_script_outbox
 * <p>
 * 与流程任务在同一事务内写入，保证"任务创建"与"脚本派发"不会出现单边成功。
 *
 * @author ruoyi
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_script_outbox")
public class FlwScriptOutbox extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 本次脚本执行唯一标识，回调幂等键
     */
    private String runId;

    /**
     * 流程实例 id
     */
    private Long instanceId;

    /**
     * 流程任务 id
     */
    private Long taskId;

    /**
     * 业务 id（告警 id）
     */
    private String businessId;

    /**
     * 流程编码
     */
    private String flowCode;

    /**
     * 节点编码
     */
    private String nodeCode;

    /**
     * 脚本绝对路径
     */
    private String scriptPath;

    /**
     * Python 解释器路径（入队时从剧本映射快照）
     */
    private String pythonBin;

    /**
     * 派发时计算的脚本文件哈希
     */
    private String scriptSha256;

    /**
     * 脚本输出（stdout 解析出的 output），JSON 文本
     */
    private String output;

    /**
     * 执行轨迹（脚本输出里的 trace 数组），JSON 文本
     */
    private String trace;

    /**
     * 派发入参（JSON）
     */
    private String payload;

    /**
     * 执行超时（秒），入队时从配置快照，避免运行期改配置影响在途任务
     */
    private Integer timeoutSec;

    /**
     * 状态，见 {@link org.ruoyi.workflow.common.enums.ScriptOutboxStatus}
     */
    private Integer status;

    /**
     * 已重试次数
     */
    private Integer retryCount;

    /**
     * 下次重试时间
     */
    private Date nextRetryAt;

    /**
     * 失败原因
     */
    private String errorMsg;

    /**
     * 派发时间
     */
    private Date dispatchedAt;

    /**
     * 超时截止时间 = 派发时间 + 超时秒数
     */
    private Date deadlineAt;

    /**
     * 完成时间
     */
    private Date finishedAt;
}
