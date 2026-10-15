package com.campus.freshman.ai;

import com.campus.freshman.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "ai_query_log")
public class AiQueryLog extends BaseEntity {
    @Column(columnDefinition = "TEXT", nullable = false)
    private String question;
    private String intent;
    private Boolean successful = true;
    private Integer confidence = 0;
    private String responseTitle;

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
    public String getIntent() { return intent; }
    public void setIntent(String intent) { this.intent = intent; }
    public Boolean getSuccessful() { return successful; }
    public void setSuccessful(Boolean successful) { this.successful = successful; }
    public Integer getConfidence() { return confidence; }
    public void setConfidence(Integer confidence) { this.confidence = confidence; }
    public String getResponseTitle() { return responseTitle; }
    public void setResponseTitle(String responseTitle) { this.responseTitle = responseTitle; }
}
