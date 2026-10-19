package com.campus.freshman.ai;

import com.campus.freshman.knowledge.KnowledgeItem;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Pattern;

/** 本地检索基线：只匹配有效已发布资料，不把单个汉字作为相关证据。 */
@Component
public class KnowledgeSearch {
    private static final Pattern FILLERS = Pattern.compile("请问|请帮我|帮我|我想|我要|怎么|如何|什么|哪些|哪里|在哪|可以|需要|一下|吗|呢|的|了|是|我");

    public List<KnowledgeItem> search(String query, List<KnowledgeItem> items, LocalDate today) {
        Set<String> terms = terms(query);
        return items.stream().filter(item -> "PUBLISHED".equals(item.getStatus()))
                .filter(item -> item.getExpiresAt() == null || !item.getExpiresAt().isBefore(today))
                .map(item -> new Match(item, score(terms, item)))
                .filter(match -> match.score() > 0)
                .sorted(Comparator.comparingInt(Match::score).reversed()
                        .thenComparing(match -> Objects.toString(match.item().getTitle(), "")))
                .limit(3).map(Match::item).toList();
    }

    private Set<String> terms(String query) {
        String text = FILLERS.matcher(query.toLowerCase(Locale.ROOT)).replaceAll(" ");
        Set<String> terms = new LinkedHashSet<>();
        for (String part : text.split("[^\\p{L}\\p{N}]+")) {
            if (part.length() < 2) continue;
            terms.add(part);
            if (part.chars().allMatch(ch -> ch > 127)) {
                for (int i = 0; i < part.length() - 1; i++) terms.add(part.substring(i, i + 2));
            }
        }
        if (query.contains("领卡") || query.contains("饭卡") || query.contains("一卡通")) terms.add("校园卡");
        if (query.contains("交学费")) terms.add("缴费");
        if (query.contains("住寝室")) terms.add("宿舍");
        return terms;
    }

    private int score(Set<String> terms, KnowledgeItem item) {
        String title = Objects.toString(item.getTitle(), "").toLowerCase(Locale.ROOT);
        String content = Objects.toString(item.getContent(), "").toLowerCase(Locale.ROOT);
        return terms.stream().mapToInt(term -> (title.contains(term) ? 3 : 0) + (content.contains(term) ? 1 : 0)).sum();
    }

    private record Match(KnowledgeItem item, int score) {}
}
