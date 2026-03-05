package org.ruoyi.common.chat.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 消息枚举
 *
 * @author zengxb
 * @date 2026-03-04
 */
@Getter
@AllArgsConstructor
public enum MessageType {

    TEXT("text"),
    FILE("file"),
    WORKFLOW("workflow");

    private final String name;
}
