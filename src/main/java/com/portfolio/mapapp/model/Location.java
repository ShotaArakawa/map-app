package com.portfolio.mapapp.model;

import java.time.OffsetDateTime;

/**
 * locations テーブルの1レコードを表すモデル。
 *
 * <p>DB上は PostGIS の geography(Point, 4326) で座標を保持しているが、
 * APIのJSON上では扱いやすいよう latitude / longitude に分解して公開する。</p>
 */
public class Location {

    private Long id;
    private String name;
    private Double latitude;   // 緯度
    private Double longitude;  // 経度
    private OffsetDateTime createdAt;
    private Long categoryId;

    public Location() {
    }

    public Location(Long id, String name, Double latitude, Double longitude, OffsetDateTime createdAt, Long categoryId) {
        this.id = id;
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.createdAt = createdAt;
        this.categoryId = categoryId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }
}
