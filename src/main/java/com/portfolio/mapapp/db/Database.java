package com.portfolio.mapapp.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

/**
 * アプリ全体で共有するコネクションプール(HikariCP)を提供するシングルトン。
 *
 * <p>接続情報は環境変数で上書きできる。未設定時は docker-compose.yml の
 * デフォルト値(localhost:5432 / shota / password / map_db)を使う。</p>
 */
public final class Database {

    private static final DataSource DATA_SOURCE = build();

    private Database() {
    }

    public static DataSource dataSource() {
        return DATA_SOURCE;
    }

    private static DataSource build() {
        String host = env("DB_HOST", "localhost");
        String port = env("DB_PORT", "5432");
        String name = env("DB_NAME", "map_db");
        String user = env("DB_USER", "shota");
        String password = env("DB_PASSWORD", "password");

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:postgresql://" + host + ":" + port + "/" + name);
        config.setUsername(user);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setPoolName("map-app-pool");

        return new HikariDataSource(config);
    }

    private static String env(String key, String defaultValue) {
        String value = System.getenv(key);
        return (value == null || value.isBlank()) ? defaultValue : value;
    }
}
