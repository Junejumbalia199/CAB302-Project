package com.formcoach.auth;

import java.sql.Connection;
import java.sql.DriverManager;

/**
 * Singleton that provides a shared SQLite {@link Connection} to the application's {@code users.db} database.
 * The connection is opened on first access and reused for the lifetime of the application.
 */
public class SqliteConnection {
    private static Connection instance = null;

    private SqliteConnection() {
        String url = "jdbc:sqlite:users.db";
        try {
            Class.forName("org.sqlite.JDBC");
            instance = DriverManager.getConnection(url);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns the shared database connection, opening it if this is the first call.
     * @return the SQLite {@link Connection} to {@code users.db}
     */
    public static Connection getInstance() {
        if (instance == null) {
            new SqliteConnection();
        }
        return instance;
    }
}
