package com.campus.freshman.rag;

import com.campus.freshman.common.BaseEntity;
import jakarta.persistence.*;

@Entity @Table(name = "rag_chunk")
public class RagChunk extends BaseEntity {
    public Long baseId;
    public Long documentId;
    public Long parentId;
    public int position;
    public Integer page;
    public String section;
    public String kind = "CHILD";
    @Column(columnDefinition = "TEXT") public String content;
    public boolean enabled = true;
    public String indexType = "FULLTEXT";
    @Version public Long revision;
}
