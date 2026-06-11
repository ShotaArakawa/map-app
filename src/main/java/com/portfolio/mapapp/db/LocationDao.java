package com.portfolio.mapapp.db;

import com.portfolio.mapapp.model.Location;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * locations テーブルへのデータアクセス。
 *
 * <p>座標は PostGIS の geography(Point,4326) で保存しているため、
 * 書き込み時は {@code ST_SetSRID(ST_MakePoint(経度, 緯度), 4326)}、
 * 読み込み時は {@code ST_Y/ST_X} で緯度経度へ変換する。</p>
 */
public class LocationDao {

    private static final String SELECT_ALL = """
            SELECT id,
                   name,
                   ST_Y(geom::geometry) AS latitude,
                   ST_X(geom::geometry) AS longitude,
                   created_at,
                   category_id
            FROM locations
            ORDER BY id
            """;

    private static final String SELECT_BY_ID = """
            SELECT id,
                   name,
                   ST_Y(geom::geometry) AS latitude,
                   ST_X(geom::geometry) AS longitude,
                   created_at,
                   category_id
            FROM locations
            WHERE id = ?
            """;

    private static final String SELECT_NEARBY = """
            SELECT id,
                   name,
                   ST_Y(geom::geometry) AS latitude,
                   ST_X(geom::geometry) AS longitude,
                   created_at,
                   category_id
            FROM locations
            WHERE ST_DWithin(geom, ST_SetSRID(ST_MakePoint(?, ?), 4326)::geography, ?)
            ORDER BY id
            """;

    private static final String INSERT = """
            INSERT INTO locations (name, geom, category_id)
            VALUES (?, ST_SetSRID(ST_MakePoint(?, ?), 4326), ?)
            RETURNING id,
                      name,
                      ST_Y(geom::geometry) AS latitude,
                      ST_X(geom::geometry) AS longitude,
                      created_at,
                      category_id
            """;

    private static final String UPDATE = """
            UPDATE locations
            SET name = ?, category_id = ?
            WHERE id = ?
            RETURNING id,
                      name,
                      ST_Y(geom::geometry) AS latitude,
                      ST_X(geom::geometry) AS longitude,
                      created_at,
                      category_id
            """;

    private static final String DELETE = "DELETE FROM locations WHERE id = ?";

    public List<Location> findAll() {
        List<Location> results = new ArrayList<>();
        try (Connection conn = Database.dataSource().getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_ALL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                results.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("locations の取得に失敗しました", e);
        }
        return results;
    }

    public List<Location> findNearby(double lat, double lng, double radiusMeters) {
        List<Location> results = new ArrayList<>();
        try (Connection conn = Database.dataSource().getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_NEARBY)) {
            ps.setDouble(1, lng); // ST_MakePoint(経度, 緯度) の順
            ps.setDouble(2, lat);
            ps.setDouble(3, radiusMeters);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("nearby locations の取得に失敗しました", e);
        }
        return results;
    }

    public Optional<Location> findById(long id) {
        try (Connection conn = Database.dataSource().getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BY_ID)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("location(id=" + id + ") の取得に失敗しました", e);
        }
    }

    public Optional<Location> update(long id, String name, Long categoryId) {
        try (Connection conn = Database.dataSource().getConnection();
             PreparedStatement ps = conn.prepareStatement(UPDATE, Statement.NO_GENERATED_KEYS)) {
            ps.setString(1, name);
            if (categoryId != null) {
                ps.setLong(2, categoryId);
            } else {
                ps.setNull(2, Types.BIGINT);
            }
            ps.setLong(3, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("location(id=" + id + ") の更新に失敗しました", e);
        }
    }

    public boolean delete(long id) {
        try (Connection conn = Database.dataSource().getConnection();
             PreparedStatement ps = conn.prepareStatement(DELETE)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("location(id=" + id + ") の削除に失敗しました", e);
        }
    }

    public Location insert(String name, double latitude, double longitude, Long categoryId) {
        try (Connection conn = Database.dataSource().getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT, Statement.NO_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setDouble(2, longitude); // ST_MakePoint(経度, 緯度) の順に注意
            ps.setDouble(3, latitude);
            if (categoryId != null) {
                ps.setLong(4, categoryId);
            } else {
                ps.setNull(4, Types.BIGINT);
            }
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return mapRow(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("location の登録に失敗しました", e);
        }
    }

    private Location mapRow(ResultSet rs) throws SQLException {
        long rawCategoryId = rs.getLong("category_id");
        Long categoryId = rs.wasNull() ? null : rawCategoryId;
        return new Location(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getDouble("latitude"),
                rs.getDouble("longitude"),
                rs.getObject("created_at", OffsetDateTime.class),
                categoryId
        );
    }
}
