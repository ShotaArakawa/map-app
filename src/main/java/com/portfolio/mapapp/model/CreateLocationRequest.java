package com.portfolio.mapapp.model;

/**
 * POST /locations のリクエストボディ。
 *
 * <pre>{@code
 * { "name": "東京駅", "latitude": 35.6812, "longitude": 139.7671 }
 * }</pre>
 */
public class CreateLocationRequest {

    private String name;
    private Double latitude;
    private Double longitude;
    private Long categoryId;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }
}
