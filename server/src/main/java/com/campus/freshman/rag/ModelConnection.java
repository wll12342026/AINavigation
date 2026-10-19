package com.campus.freshman.rag;

import com.campus.freshman.common.BaseEntity;
import jakarta.persistence.*;

@Entity @Table(name = "rag_model_connection")
public class ModelConnection extends BaseEntity {
    public String name;
    public String kind = "CHAT";
    public String baseUrl;
    public String modelId;
    public String apiKeyEnv;
    public String testStatus = "UNTESTED";
    public String testMessage;
    @Version public Long revision;
}
