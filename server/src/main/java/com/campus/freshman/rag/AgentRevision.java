package com.campus.freshman.rag;

import com.campus.freshman.common.BaseEntity;
import jakarta.persistence.*;

@Entity @Table(name = "rag_agent_revision")
public class AgentRevision extends BaseEntity {
    public Long agentId;
    public Long baseId;
    public Long modelId;
    @Column(columnDefinition = "TEXT") public String systemPrompt;
    public String releaseNote;
    public String knowledgePolicy = "LATEST_APPROVED";
    public String modelBaseUrl;
    public String providerModelId;
    public String modelKeyEnv;
}
