package org.ruoyi.common.chat.domain.dto.vulnerabilities;

import lombok.Data;

import java.util.List;

/**
 * 情报分类结果
 */
@Data
public class ClassifyResultDTO {
    private String conclusion; // 研判结论：金融业
    private Integer confidence; // 置信度：89
    private String reason;      // 判定理由
    private List<MatchDetail> details; // 判定详情列表

    @Data
    public static class MatchDetail {
        private String ruleType;  // 规则类型：关键字匹配
        private String hitContent;// 命中内容：银行
        private String location;  // 匹配位置：漏洞描述
        private boolean matched;  // 是否命中
    }
}
