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
    private String fileName;
    private Long fileSize;
    private Integer imageWidth;
    private Integer imageHeight;
    private String fileHash;
    private String visualFingerprint;
    private Integer brightnessScore;
    private Integer sharpnessScore;
    private String inspectionEvidence;

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
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    public Integer getImageWidth() { return imageWidth; }
    public void setImageWidth(Integer imageWidth) { this.imageWidth = imageWidth; }
    public Integer getImageHeight() { return imageHeight; }
    public void setImageHeight(Integer imageHeight) { this.imageHeight = imageHeight; }
    public String getFileHash() { return fileHash; }
    public void setFileHash(String fileHash) { this.fileHash = fileHash; }
    public String getVisualFingerprint() { return visualFingerprint; }
    public void setVisualFingerprint(String visualFingerprint) { this.visualFingerprint = visualFingerprint; }
    public Integer getBrightnessScore() { return brightnessScore; }
    public void setBrightnessScore(Integer brightnessScore) { this.brightnessScore = brightnessScore; }
    public Integer getSharpnessScore() { return sharpnessScore; }
    public void setSharpnessScore(Integer sharpnessScore) { this.sharpnessScore = sharpnessScore; }
    public String getInspectionEvidence() { return inspectionEvidence; }
    public void setInspectionEvidence(String inspectionEvidence) { this.inspectionEvidence = inspectionEvidence; }
}
