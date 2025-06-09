package com.airlines_sql.controllers;

import com.airlines_sql.models.LocationHistoryEntry;
import com.airlines_sql.models.StatusHistoryEntry;
import com.airlines_sql.utils.DatabaseUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

public class AircraftHistoryController {

	@FXML private Label lblHeader;
	@FXML private RadioButton rbStatusHistory;
	@FXML private RadioButton rbLocationHistory;
	@FXML private ToggleGroup historyToggleGroup;
	@FXML private TableView<Object> historyTable;

	private ManageAircraftController.AircraftInfo currentAircraft;

	public void initData(ManageAircraftController.AircraftInfo aircraft) {
		this.currentAircraft = aircraft;
		lblHeader.setText("History for Aircraft ID: " + aircraft.getAircraftId() + " (" + aircraft.getModelName() + ")");

		historyToggleGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
			if (newToggle == rbStatusHistory) {
				loadStatusHistory();
			} else if (newToggle == rbLocationHistory) {
				loadLocationHistory();
			}
		});

		loadStatusHistory();
	}

	private void loadStatusHistory() {
		setupStatusTable();
		ObservableList<Object> data = FXCollections.observableArrayList();
		String sql = "SELECT status, status_time FROM aircraft_status_history " +
				"WHERE aircraft_id = ? ORDER BY status_time DESC";

		try (Connection conn = DatabaseUtil.getConnection();
			 PreparedStatement pstmt = conn.prepareStatement(sql)) {

			pstmt.setInt(1, currentAircraft.getAircraftId());
			ResultSet rs = pstmt.executeQuery();

			while (rs.next()) {
				data.add(new StatusHistoryEntry(
						rs.getString("status"),
						rs.getTimestamp("status_time").toLocalDateTime()
				));
			}
			historyTable.setItems(data);

		} catch (SQLException e) {
			e.printStackTrace();
			showAlert("Database Error", "Failed to load status history.");
		}
	}

	private void loadLocationHistory() {
		setupLocationTable();
		ObservableList<Object> data = FXCollections.observableArrayList();
		String sql = "SELECT ap.name, ap.airport_iata, alh.recorded_at " +
				"FROM aircraft_location_history alh " +
				"JOIN airport ap ON alh.airport_id = ap.airport_id " +
				"WHERE alh.aircraft_id = ? ORDER BY alh.recorded_at DESC";

		try (Connection conn = DatabaseUtil.getConnection();
			 PreparedStatement pstmt = conn.prepareStatement(sql)) {

			pstmt.setInt(1, currentAircraft.getAircraftId());
			ResultSet rs = pstmt.executeQuery();

			while (rs.next()) {
				data.add(new LocationHistoryEntry(
						rs.getString("name"),
						rs.getString("airport_iata"),
						rs.getTimestamp("recorded_at").toLocalDateTime()
				));
			}
			historyTable.setItems(data);

		} catch (SQLException e) {
			e.printStackTrace();
			showAlert("Database Error", "Failed to load location history.");
		}
	}

	private void setupStatusTable() {
		historyTable.getColumns().clear();
		TableColumn<Object, String> colStatus = new TableColumn<>("Status");
		colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

		TableColumn<Object, LocalDateTime> colTime = new TableColumn<>("Time");
		colTime.setCellValueFactory(new PropertyValueFactory<>("statusTime"));

		historyTable.getColumns().addAll(colStatus, colTime);
	}

	private void setupLocationTable() {
		historyTable.getColumns().clear();
		TableColumn<Object, String> colName = new TableColumn<>("Airport Name");
		colName.setCellValueFactory(new PropertyValueFactory<>("airportName"));

		TableColumn<Object, String> colIata = new TableColumn<>("IATA");
		colIata.setCellValueFactory(new PropertyValueFactory<>("iataCode"));

		TableColumn<Object, LocalDateTime> colTime = new TableColumn<>("Recorded At");
		colTime.setCellValueFactory(new PropertyValueFactory<>("recordedAt"));

		historyTable.getColumns().addAll(colName, colIata, colTime);
	}

	private void showAlert(String header, String content) {
		Alert alert = new Alert(Alert.AlertType.ERROR);
		alert.setHeaderText(header);
		alert.setContentText(content);
		alert.showAndWait();
	}
}