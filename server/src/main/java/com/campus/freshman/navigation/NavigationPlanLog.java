package com.campus.freshman.navigation;

import com.campus.freshman.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "navigation_plan_log")
public class NavigationPlanLog extends BaseEntity {
    private String originName;
    private String destinationName;
    private Integer distanceMeters;
    private Integer estimatedMinutes;
    private Boolean accessibleOnly;
    private Boolean avoidRestricted;
    private String status;
    private String optimization;
    @Column(columnDefinition = "TEXT")
    private String routeSummary;

    public String getOriginName() { return originName; }
    public void setOriginName(String originName) { this.originName = originName; }
    public String getDestinationName() { return destinationName; }
    public void setDestinationName(String destinationName) { this.destinationName = destinationName; }
    public Integer getDistanceMeters() { return distanceMeters; }
    public void setDistanceMeters(Integer distanceMeters) { this.distanceMeters = distanceMeters; }
    public Integer getEstimatedMinutes() { return estimatedMinutes; }
    public void setEstimatedMinutes(Integer estimatedMinutes) { this.estimatedMinutes = estimatedMinutes; }
    public Boolean getAccessibleOnly() { return accessibleOnly; }
    public void setAccessibleOnly(Boolean accessibleOnly) { this.accessibleOnly = accessibleOnly; }
    public Boolean getAvoidRestricted() { return avoidRestricted; }
    public void setAvoidRestricted(Boolean avoidRestricted) { this.avoidRestricted = avoidRestricted; }
    public String getStatus() { return status; }
    public String getOptimization() { return optimization; }
    public void setOptimization(String optimization) { this.optimization = optimization; }
    public void setStatus(String status) { this.status = status; }
    public String getRouteSummary() { return routeSummary; }
    public void setRouteSummary(String routeSummary) { this.routeSummary = routeSummary; }
}
