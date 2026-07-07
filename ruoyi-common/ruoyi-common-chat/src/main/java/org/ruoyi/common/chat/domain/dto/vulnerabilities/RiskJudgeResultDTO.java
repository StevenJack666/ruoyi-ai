package org.ruoyi.common.chat.domain.dto.vulnerabilities;

import lombok.Data;

import java.util.List;

/**
 * 风险研判结果
 */
@Data
public class RiskJudgeResultDTO {
    private String conclusion; // 真实风险
    private Integer confidence;
    private String reason;
    private String falsePositiveType; // 误报类型
    private List<AssetMatchDetail> assetMatches; // 资产匹配详情

    @Data
    public static class AssetMatchDetail {
        private String matchItem;   // 匹配项：厂商匹配
        private String intelInfo;   // 情报信息：Apache
        private String assetInfo;   // 资产信息：Apache
        private String matchResult; // 匹配结果：匹配/在范围内
    }
}
