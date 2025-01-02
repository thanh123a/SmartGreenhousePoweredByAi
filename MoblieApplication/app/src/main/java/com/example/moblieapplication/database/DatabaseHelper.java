package com.example.moblieapplication.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseHelper {
    private static final String DB_URL = "jdbc:jtds:sqlserver://150.95.108.120:1433;databaseName=SmartGreenHouse";
    private static final String USER = "sa";
    private static final String PASSWORD = "10012002aA!";

    public static Connection getConnection() throws SQLException {
        Connection conn = null;
        try {
            Class.forName("net.sourceforge.jtds.jdbc.Driver");
            conn = DriverManager.getConnection(DB_URL, USER, PASSWORD);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return conn;
    }
}
