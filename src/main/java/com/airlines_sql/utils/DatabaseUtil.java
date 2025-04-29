package com.airlines_sql.utils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;  // импорт для Statement

public class DatabaseUtil {
    private static final String URL  = "jdbc:postgresql://localhost:5432/airlinedb";
    private static final String USER = "volochai";
    private static final String PASS = "254849";

    static {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }

    /** Сбрасывает базу в "чистое" состояние */
    public static void resetDatabase() {
        executeSqlScript("/db/clear.sql");
        executeSqlScript("/db/create.sql");
    }

    private static void executeSqlScript(String resourcePath) {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             InputStream is = DatabaseUtil.class.getResourceAsStream(resourcePath);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {

            if (is == null) {
                System.err.println("Could not find file: " + resourcePath);
                return;
            }

            StringBuilder sql = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sql.append(line).append("\n");
                if (line.trim().endsWith(";")) {
                    String command = sql.toString();
//                    System.out.println("Executing SQL:\n" + command); // DEBUG

                    try {
                        stmt.execute(command);
                    } catch (Exception e) {
                        System.err.println("SQL ERROR:\n" + command);
                        e.printStackTrace();
                    }

                    sql.setLength(0);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
