package com.campus.freshman.place;

import com.campus.freshman.common.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "campus_place")
public class Place extends BaseEntity {
    @NotBlank(message = "地点名称不能为空")
    private String name;
    @NotBlank(message = "地点类型不能为空")
    private String category;
    private String building;
    private String floor;
    @Pattern(regexp = "OPEN|MAINTENANCE|CLOSED", message = "地点状态不合法")
    private String status = "OPEN";
    @DecimalMin("-180") @DecimalMax("180")
    private Double longitude;
    @DecimalMin("-90") @DecimalMax("90")
    private Double latitude;
    private String entranceHint;
    private String description;
    @Size(max = 255)
    private String aliases;
    @Size(max = 255)
    private String openingHours;

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
    public String getAliases() { return aliases; }
    public void setAliases(String aliases) { this.aliases = aliases; }
    public String getOpeningHours() { return openingHours; }
    public void setOpeningHours(String openingHours) { this.openingHours = openingHours; }
}
