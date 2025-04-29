package com.airlines_sql.controllers;

import com.airlines_sql.utils.DatabaseUtil;
import javafx.fxml.FXML;
import javafx.scene.control.TableView;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class MainController {
    @FXML private TableView<?> flightsTable;

    @FXML
    private void onLoadFlights() {
        String sql = "SELECT first_name, last_name, email FROM customer LIMIT 10";

        try (Connection conn = DatabaseUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            System.out.println("=== First 10 customers ===");
            while (rs.next()) {
                String firstName = rs.getString("first_name");
                String lastName = rs.getString("last_name");
                String email = rs.getString("email");
                System.out.printf("%s %s — %s%n", firstName, lastName, email);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
