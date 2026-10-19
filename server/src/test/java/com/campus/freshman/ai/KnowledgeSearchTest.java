package com.campus.freshman.ai;

import com.campus.freshman.knowledge.KnowledgeItem;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class KnowledgeSearchTest {
    private final KnowledgeSearch search = new KnowledgeSearch();
    private final LocalDate today = LocalDate.of(2026, 10, 18);

    @Test
    void filtersUnpublishedAndExpiredDocuments() {
        KnowledgeItem valid = item("校园卡领取", "凭身份证到校园卡中心领取", "PUBLISHED", today);
        KnowledgeItem expired = item("校园卡领取旧版", "旧流程", "PUBLISHED", today.minusDays(1));
        KnowledgeItem pending = item("校园卡领取草稿", "未经核验", "PENDING", null);
        assertThat(search.search("怎么领卡", List.of(expired, pending, valid), today)).containsExactly(valid);
    }

    @Test
    void aSingleSharedCharacterIsNotEvidence() {
        assertThat(search.search("音乐会安排", List.of(item("校园卡领取", "到图书馆的大厅办理", "PUBLISHED", null)), today)).isEmpty();
    }

    @Test
    void ranksTitleMatchBeforeBodyMatch() {
        KnowledgeItem first = item("宿舍入住材料", "身份证、报到凭证", "PUBLISHED", null);
        KnowledgeItem other = item("校园综合服务说明", "宿舍在校园北部", "PUBLISHED", null);
        assertThat(search.search("宿舍入住材料", List.of(other, first), today)).startsWith(first);
    }

    private KnowledgeItem item(String title, String content, String status, LocalDate expiresAt) {
        KnowledgeItem item = new KnowledgeItem(); item.setTitle(title); item.setContent(content);
        item.setStatus(status); item.setExpiresAt(expiresAt); return item;
    }
}
