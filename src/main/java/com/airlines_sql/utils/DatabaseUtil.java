package com.airlines_sql.utils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class DatabaseUtil {
    private static final String DB = "airlinedb";
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

    public static void resetDatabase() throws Exception {
        executeSqlScript("/Users/volochai/prog/airlines-sql/src/main/resources/db/clear.sql");
        executeSqlScript("/Users/volochai/prog/airlines-sql/src/main/resources/db/create.sql");
        executeSqlScript("/Users/volochai/prog/airlines-sql/src/main/resources/db/loadConfiguration.sql");
    }

    public static void executeSqlScript(String scriptPath) throws Exception {
        List<String> command = new ArrayList<>();

        ProcessBuilder pb = new ProcessBuilder();

        command.add("psql");
        command.add("-U");
        command.add(USER);
        command.add("-d");
        command.add(DB);
        command.add("-f");
        command.add(scriptPath);

        pb.command(command);

        if (PASS != null && !PASS.isEmpty()) {
            pb.environment().put("PGPASSWORD", PASS);
        }

        pb.redirectErrorStream(true);

        Process process = pb.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("psql exited with code " + exitCode);
        }
    }


}
