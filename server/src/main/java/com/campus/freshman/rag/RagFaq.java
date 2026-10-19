package com.campus.freshman.rag;

import com.campus.freshman.common.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity @Table(name = "rag_faq")
public class RagFaq extends BaseEntity {
    public Long baseId;
    public String question;
    @Column(columnDefinition = "TEXT") public String aliases;
    @Column(columnDefinition = "TEXT") public String answer;
    public String category;
    public String source;
    public String campus;
    public String audience;
    public String conditions;
    public String answerMode = "DIRECT";
    public String status = "DRAFT";
    public LocalDate effectiveAt;
    public LocalDate expiresAt;
    @Version public Long revision;
}
