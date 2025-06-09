package com.airlines_sql.controllers;

import com.airlines_sql.models.AircraftForAssignment;
import com.airlines_sql.models.FlightForAssignment;
import com.airlines_sql.utils.DatabaseUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import javax.xml.crypto.Data;
import java.sql.*;
import java.time.LocalDateTime;

public class AssignAircraftController {

	@FXML private TableView<FlightForAssignment> flightsTable;
	@FXML private TableColumn<FlightForAssignment, Integer> colInstanceId;
	@FXML private TableColumn<FlightForAssignment, LocalDateTime> colDeparture;
	@FXML private TableColumn<FlightForAssignment, String> colFrom;
	@FXML private TableColumn<FlightForAssignment, String> colTo;
	@FXML private TableColumn<FlightForAssignment, Integer> colDistance;
	@FXML private ComboBox<AircraftForAssignment> aircraftComboBox;
	@FXML private Button btnAssign;
	@FXML private Button btnRefresh;

	private final ObservableList<FlightForAssignment> unassignedFlights = FXCollections.observableArrayList();

	@FXML
	private void initialize() {
		colInstanceId.setCellValueFactory(new PropertyValueFactory<>("instanceId"));
		colDeparture.setCellValueFactory(new PropertyValueFactory<>("scheduledDeparture"));
		colFrom.setCellValueFactory(new PropertyValueFactory<>("fromIata"));
		colTo.setCellValueFactory(new PropertyValueFactory<>("toIata"));
		colDistance.setCellValueFactory(new PropertyValueFactory<>("distance"));

		flightsTable.setItems(unassignedFlights);

		flightsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
			if (newSelection != null) {
				populateAircraftComboBox(newSelection);
				btnAssign.setDisable(false);
			} else {
				aircraftComboBox.getItems().clear();
				aircraftComboBox.setPromptText("Select a flight first");
				btnAssign.setDisable(true);
			}
		});

		loadUnassignedFlights();
	}

	@FXML
	private void handleRefresh() {
		loadUnassignedFlights();
	}

	private void loadUnassignedFlights() {
		unassignedFlights.clear();
		String sql = "SELECT " +
				"fi.instance_id, fi.scheduled_departure, " +
				"a_from.airport_iata AS from_iata, a_to.airport_iata AS to_iata, r.distance_km " +
				"FROM flight_instance fi " +
				"JOIN flight_schedule fs ON fi.schedule_id = fs.schedule_id " +
				"JOIN route r ON fs.route_id = r.route_id " +
				"JOIN airport a_from ON r.airport_from = a_from.airport_id " +
				"JOIN airport a_to ON r.airport_to = a_to.airport_id " +
				"WHERE fi.aircraft_id IS NULL " +
				"AND fi.scheduled_departure > NOW() " +
				"ORDER BY fi.scheduled_departure;";

		try (Connection conn = DatabaseUtil.getConnection();
			 PreparedStatement pstmt = conn.prepareStatement(sql);
			 ResultSet rs = pstmt.executeQuery()) {

			while (rs.next()) {
				unassignedFlights.add(new FlightForAssignment(
						rs.getInt("instance_id"),
						rs.getTimestamp("scheduled_departure").toLocalDateTime(),
						rs.getString("from_iata"),
						rs.getString("to_iata"),
						rs.getInt("distance_km")
				));
			}
		} catch (SQLException e) {
			e.printStackTrace();
			showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to load unassigned flights.");
		}
	}

	private void populateAircraftComboBox(FlightForAssignment selectedFlight) {
		aircraftComboBox.getItems().clear();
		String sql = "SELECT a.aircraft_id, am.name, am.range_km FROM aircraft a " +
				"JOIN aircraft_model am ON a.model_id = am.model_id " +
				"WHERE am.range_km >= ? AND " +
				"a.aircraft_id IN ( " +
				"    SELECT aircraft_id FROM aircraft_status_history " +
				"    WHERE (aircraft_id, status_time) IN ( " +
				"        SELECT aircraft_id, MAX(status_time) " +
				"        FROM aircraft_status_history " +
				"        GROUP BY aircraft_id " +
				"    ) AND status = 'InService' " +
				") ORDER BY am.name, a.aircraft_id;";

		try (Connection conn = DatabaseUtil.getConnection();
			 PreparedStatement pstmt = conn.prepareStatement(sql)) {

			pstmt.setInt(1, selectedFlight.getDistance());
			ResultSet rs = pstmt.executeQuery();

			while (rs.next()) {
				aircraftComboBox.getItems().add(new AircraftForAssignment(
						rs.getInt("aircraft_id"),
						rs.getString("name"),
						rs.getInt("range_km")
				));
			}
			if (!aircraftComboBox.getItems().isEmpty()) {
				aircraftComboBox.getSelectionModel().selectFirst();
			} else {
				aircraftComboBox.setPromptText("No suitable aircraft found");
			}

		} catch (SQLException e) {
			e.printStackTrace();
			showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to load suitable aircraft.");
		}
	}

	@FXML
	private void handleAssignAircraft() {
		FlightForAssignment selectedFlight = flightsTable.getSelectionModel().getSelectedItem();
		AircraftForAssignment selectedAircraft = aircraftComboBox.getSelectionModel().getSelectedItem();

		if (selectedFlight == null || selectedAircraft == null) {
			showAlert(Alert.AlertType.WARNING, "Selection Error", "Please select both a flight and an aircraft.");
			return;
		}

		String sql = "UPDATE flight_instance SET aircraft_id = ? WHERE instance_id = ?";

		try (Connection conn = DatabaseUtil.getConnection();
			 PreparedStatement pstmt = conn.prepareStatement(sql)) {

			pstmt.setInt(1, selectedAircraft.getAircraftId());
			pstmt.setInt(2, selectedFlight.getInstanceId());

			int affectedRows = pstmt.executeUpdate();
			if (affectedRows > 0) {
				showAlert(Alert.AlertType.INFORMATION, "Success",
						"Aircraft " + selectedAircraft.getAircraftId() + " successfully assigned to flight " + selectedFlight.getInstanceId());
				loadUnassignedFlights();
			}

		} catch (SQLException e) {
			showAlert(Alert.AlertType.ERROR, "Assignment Failed", "Database constraint violation: " + e.getMessage());
		}
	}

	private void showAlert(Alert.AlertType type, String header, String content) {
		Alert alert = new Alert(type);
		alert.setTitle("System Message");
		alert.setHeaderText(header);
		alert.setContentText(content);
		alert.showAndWait();
	}
}