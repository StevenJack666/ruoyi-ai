package org.ruoyi.service.chat;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.domain.entity.agent.AiMarket;
import org.ruoyi.mapper.agent.AiMarketMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 基于用户输入意图选择最合适的 Agent 市场
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentIntentRoutingService {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("[\\p{IsHan}\\p{Alnum}_-]+");
    private static final int MIN_MATCH_SCORE = 30;
    private static final int QUERY_PAGE_SIZE = 100;

    private final AiMarketMapper aiMarketMapper;

    /**
     * 根据用户输入匹配最合适的 Agent 市场
     */
    public Optional<AgentMatchResult> selectBestMatch(String userInput) {
        if (StringUtils.isBlank(userInput)) {
            return Optional.empty();
        }

        String normalizedInput = normalize(userInput);
        Set<String> inputTokens = tokenize(normalizedInput);

        long currentPage = 1L;
        AgentMatchResult best = null;

        while (true) {
            LambdaQueryWrapper<AiMarket> wrapper = new LambdaQueryWrapper<AiMarket>()
                .eq(AiMarket::getStatus, 1)
                .orderByAsc(AiMarket::getId);

            Page<AiMarket> pageResult = aiMarketMapper.selectPage(new Page<>(currentPage, QUERY_PAGE_SIZE), wrapper);
            List<AiMarket> markets = pageResult.getRecords();
            if (CollectionUtils.isEmpty(markets)) {
                break;
            }

            for (AiMarket market : markets) {
                int score = scoreMarket(market, normalizedInput, inputTokens);
                if (score < MIN_MATCH_SCORE) {
                    continue;
                }
                if (best == null || score > best.score()) {
                    best = new AgentMatchResult(market.getId(), market.getMarketName(), market.getDescription(), score);
                }
            }

            if (currentPage >= pageResult.getPages()) {
                break;
            }
            currentPage++;
        }

        return Optional.ofNullable(best);
    }

    private int scoreMarket(AiMarket market, String normalizedInput, Set<String> inputTokens) {
        String marketName = normalize(market.getMarketName());
        String description = normalize(market.getDescription());
        if (StringUtils.isBlank(marketName)) {
            return 0;
        }

        int score = 0;

        if (normalizedInput.contains(marketName)) {
            score += 120;
        }
        if (marketName.contains(normalizedInput) && normalizedInput.length() >= 2) {
            score += 60;
        }

        for (String token : inputTokens) {
            if (token.length() < 2) {
                continue;
            }
            if (marketName.contains(token)) {
                score += 30;
                continue;
            }
            if (StringUtils.isNotBlank(description) && description.contains(token)) {
                score += 12;
            }
        }

        return score;
    }

    private String normalize(String text) {
        if (text == null) {
            return "";
        }
        return text.toLowerCase(Locale.ROOT).trim().replaceAll("\\s+", " ");
    }

    private Set<String> tokenize(String text) {
        Set<String> tokens = new HashSet<>();
        if (StringUtils.isBlank(text)) {
            return tokens;
        }

        Matcher matcher = TOKEN_PATTERN.matcher(text);
        while (matcher.find()) {
            String token = matcher.group();
            if (StringUtils.isNotBlank(token)) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    public record AgentMatchResult(Long marketId, String marketName, String description, int score) {
    }
}