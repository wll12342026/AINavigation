package com.campus.freshman.rag;

import com.campus.freshman.common.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity @Table(name = "rag_knowledge_base")
public class RagBase extends BaseEntity {
    public String name;
    public String description;
    public String campus;
    public String audience;
    public int chunkSize = 800;
    public int overlap = 100;
    public String chunkMode = "GENERAL";
    @Version public Long revision;
}
