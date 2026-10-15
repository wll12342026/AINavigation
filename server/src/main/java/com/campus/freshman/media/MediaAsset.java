package com.campus.freshman.media;

import com.campus.freshman.common.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Entity
@Table(name = "media_asset")
public class MediaAsset extends BaseEntity {
    @NotBlank(message = "素材名称不能为空")
    private String title;
    private Long placeId;
    private String placeName;
    private String type = "PHOTO";
    private String url;
    @Min(0) @Max(100)
    private Integer qualityScore;
    private String reviewStatus = "PENDING";
    private String issueType;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public Long getPlaceId() { return placeId; }
    public void setPlaceId(Long placeId) { this.placeId = placeId; }
    public String getPlaceName() { return placeName; }
    public void setPlaceName(String placeName) { this.placeName = placeName; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public Integer getQualityScore() { return qualityScore; }
    public void setQualityScore(Integer qualityScore) { this.qualityScore = qualityScore; }
    public String getReviewStatus() { return reviewStatus; }
    public void setReviewStatus(String reviewStatus) { this.reviewStatus = reviewStatus; }
    public String getIssueType() { return issueType; }
    public void setIssueType(String issueType) { this.issueType = issueType; }
}
