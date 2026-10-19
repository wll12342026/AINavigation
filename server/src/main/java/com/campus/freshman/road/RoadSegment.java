package com.campus.freshman.road;

import com.campus.freshman.common.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.AssertTrue;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "road_segment")
public class RoadSegment extends BaseEntity {
    @NotBlank(message = "道路名称不能为空")
    private String name;
    @NotBlank(message = "起点节点不能为空")
    private String fromNode;
    @NotBlank(message = "终点节点不能为空")
    private String toNode;
    @Min(value = 1, message = "路段长度必须大于零")
    @NotNull(message = "请填写路段长度")
    private Integer lengthMeters = 0;
    @Pattern(regexp = "OPEN|RESTRICTED|CLOSED", message = "道路状态不合法")
    @NotNull(message = "请填写道路状态")
    private String status = "OPEN";
    private String restrictionReason;
    @Column(name = "is_accessible")
    private Boolean accessible = true;
    private Boolean bidirectional = true;
    @Min(value = 1, message = "通行时间必须大于零")
    private Integer travelSeconds;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getFromNode() { return fromNode; }
    public void setFromNode(String fromNode) { this.fromNode = fromNode; }
    public String getToNode() { return toNode; }
    public void setToNode(String toNode) { this.toNode = toNode; }
    public Integer getLengthMeters() { return lengthMeters; }
    public void setLengthMeters(Integer lengthMeters) { this.lengthMeters = lengthMeters; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getRestrictionReason() { return restrictionReason; }
    public void setRestrictionReason(String restrictionReason) { this.restrictionReason = restrictionReason; }
    public Boolean getAccessible() { return accessible; }
    public void setAccessible(Boolean accessible) { this.accessible = accessible; }
    public Boolean getBidirectional() { return bidirectional; }
    public void setBidirectional(Boolean bidirectional) { this.bidirectional = bidirectional; }
    public Integer getTravelSeconds() { return travelSeconds; }
    public void setTravelSeconds(Integer travelSeconds) { this.travelSeconds = travelSeconds; }
    @AssertTrue(message = "起点和终点不能相同")
    @JsonIgnore
    public boolean isEndpointsDistinct() {
        return fromNode == null || toNode == null || !fromNode.trim().equals(toNode.trim());
    }
}
