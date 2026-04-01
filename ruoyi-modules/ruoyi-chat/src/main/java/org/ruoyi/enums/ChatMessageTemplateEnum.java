package org.ruoyi.enums;

import lombok.Getter;

@Getter
public enum ChatMessageTemplateEnum {

    /**
     * 文件分析助手模板
     * 用于文件对话场景，告诉 LLM 如何基于文档内容回答问题
     */
    DOCUMENT_ANALYZER("chat.file.analyzer.template");

    private final String value;

    ChatMessageTemplateEnum(String value) {
        this.value = value;
    }
}
