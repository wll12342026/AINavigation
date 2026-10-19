package com.campus.freshman.rag;

import com.campus.freshman.common.BaseEntity;
import jakarta.persistence.*;

@Entity @Table(name = "rag_agent")
public class CampusAgent extends BaseEntity {
    public String name;
    public Long baseId;
    public Long modelId;
    @Column(columnDefinition = "TEXT") public String systemPrompt;
    public Long publishedRevisionId;
    @Version public Long revision;
}
