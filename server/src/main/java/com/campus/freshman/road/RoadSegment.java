package com.campus.freshman.road;

import com.campus.freshman.common.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "road_segment")
public class RoadSegment extends BaseEntity {
    @NotBlank(message = "道路名称不能为空")
    private String name;
    private String fromNode;
    private String toNode;
    @Min(0)
    private Integer lengthMeters = 0;
    private String status = "OPEN";
    private String restrictionReason;
    @Column(name = "is_accessible")
    private Boolean accessible = true;

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
}
