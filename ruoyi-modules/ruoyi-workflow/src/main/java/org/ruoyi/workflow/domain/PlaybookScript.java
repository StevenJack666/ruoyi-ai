package org.ruoyi.workflow.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serial;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.common.tenant.core.TenantEntity;

/**
 * 剧本节点脚本映射 t_playbook_script
 * <p>
 * 一个流程节点对应一个外部 Python 剧本文件，只存储路径与执行参数，不存储脚本内容。
 *
 * @author ruoyi
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_playbook_script")
public class PlaybookScript extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 流程编码
     */
    private String flowCode;

    /**
     * 节点编码
     */
    private String nodeCode;

    /**
     * Python 脚本路径
     */
    private String scriptPath;

    /**
     * 该脚本使用的 Python 解释器路径（如独立 venv 的 python），必填
     */
    private String pythonBin;

    /**
     * 执行超时（秒）
     */
    private Integer timeoutSec;

    /**
     * 派发失败最大重试次数
     */
    private Integer maxRetry;

    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 备注
     */
    private String remark;
}
