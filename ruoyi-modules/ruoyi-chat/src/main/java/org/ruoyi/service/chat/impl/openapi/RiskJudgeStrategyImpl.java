package org.ruoyi.service.chat.impl.openapi;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.chat.domain.dto.vulnerabilities.RiskJudgeResultDTO;
import org.springframework.stereotype.Service;


/**
 * 风险研判
 *
 * @author ageerle@163.com
 * @date 2025/12/13
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RiskJudgeStrategyImpl extends AbstractIntelAnalysisService<RiskJudgeResultDTO> {

    @Override
    public String getSceneCode() {
        return "RISK_JUDGE";
    }

    @Override
    public String getSessionConfigKey() {
        return "riskJudge.session.config";
    }

    @Override
    protected String getSystemPrompt() {
        return """
            你是一个资深的安全运营中心（SOC）研判专家。你的任务是精准对比“情报信息”与“资产信息”，研判该情报对当前资产是否构成真实风险。

            【研判维度】
            请从以下维度进行交叉比对：
            1. 厂商/产品匹配：情报中的厂商或产品是否与资产一致。
            2. 版本/影响范围：资产的具体版本是否在情报声明的受影响版本范围内。
            3. 组件/模块匹配：情报提及的特定组件或模块是否在资产中存在。

            【输出要求】
            1. 必须且仅能返回一个合法的 JSON 对象，绝对不能包含任何 Markdown 标记（如 ```json）、解释性文字或其他废话。
            2. JSON 必须严格包含以下五个字段：
               - "conclusion": 只能是 "真实风险"、"潜在风险" 或 "误报" 这三个字符串之一。
               - "confidence": 0 到 100 之间的整数，表示你的确信程度。
               - "reason": 一句话简述研判理由（不超过50字）。
               - "falsePositiveType": 如果判定为误报，请指出原因（如："厂商不匹配"、"版本不在影响范围内"、"组件未安装"）；如果不是误报，请输出 "无"。
               - "assetMatches": 一个数组，列出你进行比对的具体依据。数组中每个对象必须包含以下四个字段：
                 * "matchItem": 匹配项（如："厂商匹配"、"版本比对"、"组件匹配"）。
                 * "intelInfo": 情报中提取的相关信息（如："D-Link"、"<1.09b03"）。
                 * "assetInfo": 资产中提取的相关信息（如："D-Link DAP-1325"、"1.08b01"）。
                 * "matchResult": 匹配结果（只能是："匹配"、"在范围内"、"不匹配"、"不在范围内"、"未知"）。

            【输出示例】
            {
                "conclusion": "真实风险",
                "confidence": 98,
                "reason": "资产版本在漏洞影响范围内，且厂商与产品完全匹配。",
                "falsePositiveType": "无",
                "assetMatches": [
                    {
                        "matchItem": "厂商匹配",
                        "intelInfo": "D-Link",
                        "assetInfo": "D-Link DAP-1325",
                        "matchResult": "匹配"
                    },
                    {
                        "matchItem": "版本比对",
                        "intelInfo": "<1.09b03",
                        "assetInfo": "1.08b01",
                        "matchResult": "在范围内"
                    }
                ]
            }
            """;
    }

    @Override
    protected Class<RiskJudgeResultDTO> getResultClass() {
        return RiskJudgeResultDTO.class;
    }
}
