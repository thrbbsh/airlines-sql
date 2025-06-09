package com.airlines_sql.controllers;

import com.airlines_sql.models.FlightStatusHistoryEntry;
import com.airlines_sql.utils.DatabaseUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

public class FlightHistoryController {

	@FXML private Label lblHeader;
	@FXML private TableView<FlightStatusHistoryEntry> historyTable;
	@FXML private TableColumn<FlightStatusHistoryEntry, String> colStatus;
	@FXML private TableColumn<FlightStatusHistoryEntry, LocalDateTime> colTime;

	private int flightInstanceId;

	public void initData(int instanceId) {
		this.flightInstanceId = instanceId;
		lblHeader.setText("Status History for Flight ID: " + flightInstanceId);
		setupTable();
		loadHistory();
	}

	private void setupTable() {
		colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
		colTime.setCellValueFactory(new PropertyValueFactory<>("statusTime"));
	}

	private void loadHistory() {
		ObservableList<FlightStatusHistoryEntry> historyData = FXCollections.observableArrayList();
		String sql = "SELECT status, status_time " +
				"FROM flight_status_history " +
				"WHERE instance_id = ? " +
				"ORDER BY status_time DESC";

		try (Connection conn = DatabaseUtil.getConnection();
			 PreparedStatement pstmt = conn.prepareStatement(sql)) {

			pstmt.setInt(1, flightInstanceId);
			ResultSet rs = pstmt.executeQuery();

			while (rs.next()) {
				historyData.add(new FlightStatusHistoryEntry(
						rs.getString("status"),
						rs.getTimestamp("status_time").toLocalDateTime()
				));
			}
			historyTable.setItems(historyData);

		} catch (SQLException e) {
			e.printStackTrace();
			new Alert(Alert.AlertType.ERROR, "Failed to load flight history.").showAndWait();
		}
	}
}