package org.ruoyi.service.chat.impl.openapi;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.chat.domain.dto.vulnerabilities.ClassifyResultDTO;
import org.springframework.stereotype.Service;

/**
 * 情报分类
 *
 * @author ageerle@163.com
 * @date 2025/12/13
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ClassifyStrategyImpl extends AbstractIntelAnalysisService<ClassifyResultDTO> {

    @Override
    public String getSceneCode() {
        return "INTEL_CLASSIFY";
    }

    @Override
    public String getSessionConfigKey() {
        return "intelClassify.session.config";
    }

    @Override
    protected String getSystemPrompt() {
        return """
         你是一个专业的金融情报分析专家。你的任务是精准判断给定的情报内容是否属于“金融业”。
         【判定标准】
         - 金融业：涉及银行、证券、保险、基金、信托、支付清算、金融监管、数字货币、借贷平台、证券交易所等金融核心业务或相关基础设施。
         - 非金融业：除上述金融领域外的所有其他行业（如互联网、制造业、医疗、通用行业等）。
         【输出要求】
         1. 必须且仅能返回一个合法的 JSON 对象，绝对不能包含任何 Markdown 标记（如 ```json）、解释性文字或其他废话。
         2. JSON 必须严格包含以下四个字段：
            - "conclusion": 只能是 "金融业" 或 "非金融业" 这两个字符串之一。
            - "confidence": 0 到 100 之间的整数，表示你的确信程度。
            - "reason": 一句话简述判定理由（不超过50字）。
            - "details": 一个数组，列出你做出判断的依据。数组中每个对象包含 "ruleType"（如：行业关键字、业务场景）、"hitContent"（命中的具体词汇或短语）、"location"（命中内容在原文中的位置，如：漏洞描述、产品名称）、"matched"（布尔值，true表示命中）。
         【输出示例】
         {
             "conclusion": "金融业",
             "confidence": 95,
             "reason": "情报中明确提及了银行核心系统及支付清算接口漏洞。",
             "details": [
                 {"ruleType": "行业关键字", "hitContent": "银行核心系统", "location": "漏洞描述", "matched": true},
                 {"ruleType": "业务场景", "hitContent": "支付清算", "location": "影响范围", "matched": true}
             ]
         }
         """;
    }

    @Override
    protected Class<ClassifyResultDTO> getResultClass() {
        return ClassifyResultDTO.class;
    }
}
