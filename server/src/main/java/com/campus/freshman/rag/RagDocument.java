package com.campus.freshman.rag;

import com.campus.freshman.common.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity @Table(name = "rag_document")
public class RagDocument extends BaseEntity {
    public Long baseId;
    public Long rootId;
    public Long legacyId;
    public int versionNumber = 1;
    public String title;
    public String source;
    public String originalName;
    public String storageName;
    public String fileHash;
    public long fileSize;
    public String format = "TEXT";
    @Column(columnDefinition = "LONGTEXT") public String content;
    public String processing = "QUEUED";
    public String status = "DRAFT";
    @Column(length = 1000) public String error;
    public String warning;
    public LocalDate effectiveAt;
    public LocalDate expiresAt;
    public int attempts;
    public long generation;
    @Version public Long revision;
}
