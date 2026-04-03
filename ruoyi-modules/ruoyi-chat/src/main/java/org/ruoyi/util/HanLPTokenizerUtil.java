package org.ruoyi.util;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.hankcs.hanlp.HanLP;
import org.ruoyi.common.core.utils.StringUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * HanLP 分词工具类 - 专注于中文分词和关键词提取
 *
 * @author ageerle@163.com
 * @date 2025/04/03
 */
@Slf4j
public class HanLPTokenizerUtil {

    /**
     * 领域特定停用词（在 HanLP 基础上扩展）
     * 针对 AI Agent 场景的特殊过滤词
     */
    private static final Set<String> DOMAIN_STOP_WORDS = Set.of(
        // 礼貌用语（不影响意图）
        "请", "帮", "麻烦", "辛苦", "劳驾", "拜托",

        // 通用动词（无明确意图）
        "问", "说", "讲", "弄", "搞", "做", "整",

        // 语气助词
        "啦", "啊", "呢", "吧", "嘛", "呀", "哇", "哦", "哎",

        // 指示代词（过于宽泛）
        "这个", "那个", "这些", "那些", "啥", "某",

        // 程度副词（对匹配无帮助）
        "非常", "特别", "十分", "极其", "格外", "分外",

        // AI 对话常见冗余词
        "想要", "需要", "希望", "打算", "准备", "想",

        // 结构助词
        "的", "之", "乎", "者", "也", "而", "且", "或"
    );

    /**
     * 关键单字白名单（即使 HanLP 分词也要保留）
     */
    private static final Set<String> SINGLE_CHAR_WHITELIST = Set.of(
        // 数量词
        "5", "6", "7", "8", "9",
        // 量词
        "首", "部", "篇", "张", "款", "种", "类", "型",
        // 特殊字母组合
        "AI", "NBA", "CBA", "KTV", "PPT", "PDF", "VIP", "APP"
    );

    /**
     * 私有构造函数，防止实例化
     */
    private HanLPTokenizerUtil() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * 使用 HanLP 智能分词 + 三层过滤策略
     *
     * @param text 待分词的文本
     * @return 分词结果集合
     */
    public static Set<String> tokenize(String text) {
        Set<String> tokens = new HashSet<>();
        if (StringUtils.isBlank(text)) {
            return tokens;
        }

        try {
            // Step 1: HanLP 智能分词（自动识别词性）
            List<String> wordList = HanLP.segment(text).stream()
                .map(term -> term.word.trim())
                .filter(StringUtils::isNotBlank)
                .toList();

            log.debug("HanLP 原始分词：input={}, words={}", text, wordList);

            // Step 2: 应用三层过滤规则
            for (String word : wordList) {
                // 第一层：白名单直接保留
                if (isInWhitelist(word)) {
                    tokens.add(word);
                    continue;
                }

                // 第二层：多字符词检查停用词
                if (word.length() >= 2) {
                    if (!DOMAIN_STOP_WORDS.contains(word)) {
                        tokens.add(word);
                    }
                    continue;
                }

                // 第三层：单字符特殊处理
                if (shouldKeepSingleChar(word.charAt(0))) {
                    tokens.add(word);
                }
            }

            log.debug("最终分词：input={}, tokens={}", text, tokens);

        } catch (Exception e) {
            log.error("HanLP 分词失败，降级到正则分词：{}", e.getMessage());
            // 降级方案：使用正则分词
            return tokenizeByRegex(text);
        }

        return tokens;
    }

    /**
     * 判断字符是否在白名单中
     */
    private static boolean isInWhitelist(String word) {
        return SINGLE_CHAR_WHITELIST.contains(word);
    }

    /**
     * 判断单字符是否应该保留
     */
    private static boolean shouldKeepSingleChar(char c) {
        // 数字保留
        if (Character.isDigit(c)) {
            return true;
        }

        // 英文字母保留（排除汉字）
        if (isEnglishLetter(c)) {
            return true;
        }

        // 量词保留
        if (isMeasureWord(c)) {
            return true;
        }

        // 其他单字不保留
        return false;
    }

    /**
     * 判断是否为英文字母（排除汉字）
     */
    private static boolean isEnglishLetter(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    /**
     * 判断是否为常用量词
     */
    private static boolean isMeasureWord(char c) {
        return switch (c) {
            case '首', '部', '篇', '张', '款', '种', '类', '型',
                 '个', '只', '条', '件', '朵', '本', '台',
                 '辆', '匹', '面', '口', '座' -> true;
            default -> false;
        };
    }

    /**
     * 降级方案：基于正则的分词（当 HanLP 不可用时）
     */
    private static Set<String> tokenizeByRegex(String text) {
        Set<String> tokens = new HashSet<>();
        Pattern pattern = Pattern.compile(
            "[\\p{IsHan}]{2,}" +      // 2 个以上汉字
                "|[\\p{Alpha}]{1,}" +     // 字母
                "|[0-9]{1,}" +            // 数字
                "|[_-]{1,}"               // 连接符
        );

        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            String token = matcher.group().trim();
            if (StringUtils.isNotBlank(token) && !DOMAIN_STOP_WORDS.contains(token)) {
                tokens.add(token);
            }
        }

        log.debug("正则降级分词：input={}, tokens={}", text, tokens);
        return tokens;
    }
}
