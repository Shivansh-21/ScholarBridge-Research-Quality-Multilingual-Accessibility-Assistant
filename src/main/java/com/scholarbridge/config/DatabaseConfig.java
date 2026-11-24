package com.scholarbridge.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DatabaseConfig {
    private DatabaseConfig() {}

    public static Connection getConnection() throws SQLException {
        String url = env("DB_URL", "jdbc:mysql://localhost:3306/scholarbridge"
                + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
        String user = env("DB_USER", "root");
        String password = env("DB_PASSWORD", "");
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException exception) {
            throw new SQLException("MySQL JDBC driver not found", exception);
        }
        return DriverManager.getConnection(url, user, password);
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}
