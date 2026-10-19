package com.campus.freshman.rag;

import com.campus.freshman.common.BaseEntity;
import jakarta.persistence.*;

@Entity @Table(name = "rag_run")
public class RagRun extends BaseEntity {
    public Long agentId;
    public Long baseId;
    public Long releaseId;
    public String mode;
    public String status;
    public String traffic = "DEBUG";
    @Column(columnDefinition = "TEXT") public String question;
    @Column(columnDefinition = "TEXT") public String answer;
    @Column(columnDefinition = "TEXT") public String sourceIds;
    @Column(columnDefinition = "TEXT") public String promptSnapshot;
    public String modelName;
    public long elapsedMs;
    public Integer inputTokens;
    public Integer outputTokens;
}
