package org.ruoyi.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.util.Date;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.ruoyi.workflow.common.enums.ScriptOutboxStatus;
import org.ruoyi.workflow.domain.FlwScriptOutbox;

/**
 * 脚本派发 outbox Mapper
 *
 * @author ruoyi
 */
public interface FlwScriptOutboxMapper extends BaseMapper<FlwScriptOutbox> {
    /**
     * 抢占一条待派发记录：状态流转与时间戳在同一 SQL 内完成，避免中途宕机导致记录卡死
     *
     * @param id       主键
     * @param now      派发时间
     * @param deadline 超时截止时间
     * @return 影响行数，0 表示已被其它实例抢占
     */
    @Update(
        "UPDATE t_script_outbox SET status = 1, dispatched_at = #{now}, deadline_at = #{deadline} " +
            "WHERE id = #{id} AND status = 0"
    )
    int claimForDispatch(
        @Param("id") Long id,
        @Param("now") Date now,
        @Param("deadline") Date deadline
    );

    /**
     * 状态流转（CAS），返回影响行数，0 表示状态已被其它线程改变
     *
     * @param id   主键
     * @param from 期望的当前状态
     * @param to   目标状态
     * @return 影响行数
     */
    default int casStatus(Long id, Integer from, Integer to) {
        return update(
            null,
            Wrappers.<FlwScriptOutbox>lambdaUpdate()
                .eq(FlwScriptOutbox::getId, id)
                .eq(FlwScriptOutbox::getStatus, from)
                .set(FlwScriptOutbox::getStatus, to)
        );
    }

    /**
     * 查询待派发记录：状态待派发，且已到重试时间
     *
     * @param limit 批量上限
     * @return 待派发记录
     */
    default List<FlwScriptOutbox> selectDispatchable(int limit) {
        Date now = new Date();
        return selectList(
            Wrappers.<FlwScriptOutbox>lambdaQuery()
                .eq(
                    FlwScriptOutbox::getStatus,
                    ScriptOutboxStatus.PENDING.getCode()
                )
                .and(w ->
                    w
                        .isNull(FlwScriptOutbox::getNextRetryAt)
                        .or()
                        .le(FlwScriptOutbox::getNextRetryAt, now)
                )
                .orderByAsc(FlwScriptOutbox::getId)
                .last("limit " + limit)
        );
    }

    /**
     * 查询已超时的在途记录
     *
     * @param now   当前时间
     * @param limit 批量上限
     * @return 超时记录
     */
    default List<FlwScriptOutbox> selectOverdue(Date now, int limit) {
        return selectList(
            Wrappers.<FlwScriptOutbox>lambdaQuery()
                .eq(
                    FlwScriptOutbox::getStatus,
                    ScriptOutboxStatus.DISPATCHED.getCode()
                )
                .isNotNull(FlwScriptOutbox::getDeadlineAt)
                .le(FlwScriptOutbox::getDeadlineAt, now)
                .orderByAsc(FlwScriptOutbox::getId)
                .last("limit " + limit)
        );
    }

    /**
     * 按 runId 查询
     *
     * @param runId 执行标识
     * @return outbox 记录
     */
    default FlwScriptOutbox selectByRunId(String runId) {
        return selectOne(
            Wrappers.<FlwScriptOutbox>lambdaQuery()
                .eq(FlwScriptOutbox::getRunId, runId)
                .last("limit 1")
        );
    }
}
