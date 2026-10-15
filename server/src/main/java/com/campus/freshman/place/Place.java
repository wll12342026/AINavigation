package com.campus.freshman.place;

import com.campus.freshman.common.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "campus_place")
public class Place extends BaseEntity {
    @NotBlank(message = "地点名称不能为空")
    private String name;
    @NotBlank(message = "地点类型不能为空")
    private String category;
    private String building;
    private String floor;
    private String status = "OPEN";
    private Double longitude;
    private Double latitude;
    private String entranceHint;
    private String description;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getBuilding() { return building; }
    public void setBuilding(String building) { this.building = building; }
    public String getFloor() { return floor; }
    public void setFloor(String floor) { this.floor = floor; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public String getEntranceHint() { return entranceHint; }
    public void setEntranceHint(String entranceHint) { this.entranceHint = entranceHint; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
