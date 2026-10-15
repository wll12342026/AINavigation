package com.campus.freshman.knowledge;

import com.campus.freshman.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

@Entity
@Table(name = "knowledge_item")
public class KnowledgeItem extends BaseEntity {
    @NotBlank(message = "知识标题不能为空")
    private String title;
    private String category;
    @Column(columnDefinition = "TEXT")
    private String content;
    private String status = "PUBLISHED";
    private String source;
    private LocalDate expiresAt;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public LocalDate getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDate expiresAt) { this.expiresAt = expiresAt; }
}
