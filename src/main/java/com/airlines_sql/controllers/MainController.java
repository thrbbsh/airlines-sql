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
        try (Connection conn = DatabaseUtil.getConnection();
             Statement stmt = conn.createStatement()) {
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}