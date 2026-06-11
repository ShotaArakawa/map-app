package com.portfolio.mapapp.db;

import com.portfolio.mapapp.model.Category;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CategoryDao {

    private static final String SELECT_ALL =
            "SELECT id, name, color FROM categories ORDER BY id";

    private static final String INSERT =
            "INSERT INTO categories (name, color) VALUES (?, ?) RETURNING id, name, color";

    private static final String UPDATE =
            "UPDATE categories SET name = ?, color = ? WHERE id = ? RETURNING id, name, color";

    private static final String DELETE_CATEGORY =
            "DELETE FROM categories WHERE id = ?";

    private static final String NULLIFY_CATEGORY_FK =
            "UPDATE locations SET category_id = NULL WHERE category_id = ?";

    public List<Category> findAll() {
        List<Category> results = new ArrayList<>();
        try (Connection conn = Database.dataSource().getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_ALL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                results.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("categories の取得に失敗しました", e);
        }
        return results;
    }

    public Category insert(String name, String color) {
        try (Connection conn = Database.dataSource().getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT, Statement.NO_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setString(2, color);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return mapRow(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("category の登録に失敗しました", e);
        }
    }

    public Optional<Category> update(long id, String name, String color) {
        try (Connection conn = Database.dataSource().getConnection();
             PreparedStatement ps = conn.prepareStatement(UPDATE, Statement.NO_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setString(2, color);
            ps.setLong(3, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("category の更新に失敗しました", e);
        }
    }

    public boolean delete(long id) {
        try (Connection conn = Database.dataSource().getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(NULLIFY_CATEGORY_FK)) {
                ps.setLong(1, id);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(DELETE_CATEGORY)) {
                ps.setLong(1, id);
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("category(id=" + id + ") の削除に失敗しました", e);
        }
    }

    private Category mapRow(ResultSet rs) throws SQLException {
        return new Category(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("color")
        );
    }
}
