package com.airlines_sql.controllers;

import com.airlines_sql.models.FlightForIncident;
import com.airlines_sql.utils.DatabaseUtil;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class ManageIncidentsController {

	@FXML private TextField flightIdTextField;
	@FXML private Button searchButton;
	@FXML private GridPane flightInfoPane;
	@FXML private Label flightRouteLabel;
	@FXML private Label flightStatusLabel;
	@FXML private GridPane incidentCreationPane;
	@FXML private ComboBox<String> incidentTypeComboBox;
	@FXML private TextArea descriptionTextArea;
	@FXML private Button btnCreateIncident;

	private FlightForIncident currentFlight;

	private final Map<String, List<String>> allowedIncidentsByStatus = Map.of(
			"Scheduled", List.of("Technical", "Weather", "Security", "Other", "DestroyedOnGround"),
			"Delayed",   List.of("Technical", "Weather", "Security", "Other", "DestroyedOnGround"),
			"Departed",  List.of("Technical", "Weather", "Security", "Other", "Catastrophe")
	);

	@FXML
	private void initialize() {
		flightIdTextField.textProperty().addListener((obs, oldVal, newVal) -> {
			searchButton.setDisable(newVal.trim().isEmpty() || !newVal.matches("\\d+"));
		});
		resetForm();
	}

	@FXML
	private void handleSearchFlight() {
		String flightIdStr = flightIdTextField.getText().trim();
		int flightId;
		try {
			flightId = Integer.parseInt(flightIdStr);
		} catch (NumberFormatException e) {
			showAlert(Alert.AlertType.WARNING, "Invalid Input", "Flight ID must be a number.");
			return;
		}

		String sql = "SELECT " +
				"    fi.instance_id, fi.aircraft_id, fi.scheduled_departure, " +
				"    get_flight_current_status(fi.instance_id) AS current_status, " +
				"    a_from.airport_iata || ' -> ' || a_to.airport_iata AS route " +
				"FROM flight_instance fi " +
				"JOIN flight_schedule fs ON fi.schedule_id = fs.schedule_id " +
				"JOIN route r ON fs.route_id = r.route_id " +
				"JOIN airport a_from ON r.airport_from = a_from.airport_id " +
				"JOIN airport a_to ON r.airport_to = a_to.airport_id " +
				"WHERE fi.instance_id = ?";

		try (Connection conn = DatabaseUtil.getConnection();
			 PreparedStatement pstmt = conn.prepareStatement(sql)) {

			pstmt.setInt(1, flightId);
			ResultSet rs = pstmt.executeQuery();

			if (rs.next()) {
				currentFlight = new FlightForIncident(
						rs.getInt("instance_id"),
						rs.getInt("aircraft_id"),
						rs.getString("current_status"),
						rs.getTimestamp("scheduled_departure").toLocalDateTime(),
						rs.getString("route")
				);
				updateUIWithFlightData();
			} else {
				showAlert(Alert.AlertType.INFORMATION, "Not Found", "Flight with ID " + flightId + " was not found.");
				resetForm();
			}

		} catch (SQLException e) {
			e.printStackTrace();
			showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to search for the flight.");
		}
	}

	private void updateUIWithFlightData() {
		if (currentFlight == null) return;

		flightRouteLabel.setText(currentFlight.getRoute());
		flightStatusLabel.setText(currentFlight.getFlightStatus());
		flightInfoPane.setVisible(true);

		List<String> allowedTypes = allowedIncidentsByStatus.get(currentFlight.getFlightStatus());
		if (allowedTypes != null && !allowedTypes.isEmpty()) {
			populateIncidentTypes(allowedTypes);
			incidentCreationPane.setDisable(false);
			btnCreateIncident.setDisable(false);
		} else {
			showAlert(Alert.AlertType.INFORMATION, "Flight Status",
					"Cannot log an incident for a flight with status '" + currentFlight.getFlightStatus() + "'.");
			incidentCreationPane.setDisable(true);
			btnCreateIncident.setDisable(true);
		}
	}

	private void populateIncidentTypes(List<String> allowedTypes) {
		incidentTypeComboBox.getItems().setAll(allowedTypes);
		incidentTypeComboBox.getSelectionModel().selectFirst();
	}

	@FXML
	private void handleCreateIncident() {
		if (currentFlight == null) {
			showAlert(Alert.AlertType.ERROR, "System Error", "No flight selected.");
			return;
		}

		String selectedIncidentType = incidentTypeComboBox.getSelectionModel().getSelectedItem();
		String description = descriptionTextArea.getText();

		if (selectedIncidentType == null || selectedIncidentType.isEmpty()) {
			showAlert(Alert.AlertType.WARNING, "Input Error", "Please select an incident type.");
			return;
		}

		String sql = "INSERT INTO incident (flight_instance_id, incident_type, description) VALUES (?, ?::incident_type_enum, ?)";

		try (Connection conn = DatabaseUtil.getConnection();
			 PreparedStatement pstmt = conn.prepareStatement(sql)) {

			pstmt.setInt(1, currentFlight.getInstanceId());
			pstmt.setString(2, selectedIncidentType);
			pstmt.setString(3, description);

			int affectedRows = pstmt.executeUpdate();
			if (affectedRows > 0) {
				showAlert(Alert.AlertType.INFORMATION, "Success", "Incident '" + selectedIncidentType + "' has been successfully logged for flight " + currentFlight.getInstanceId() + ".");
				resetForm();
				flightIdTextField.clear();
			}

		} catch (SQLException e) {
			e.printStackTrace();
			showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to create incident: " + e.getMessage());
		}
	}

	private void resetForm() {
		currentFlight = null;
		flightInfoPane.setVisible(false);
		incidentCreationPane.setDisable(true);
		btnCreateIncident.setDisable(true);
		incidentTypeComboBox.getItems().clear();
		descriptionTextArea.clear();
	}

	private void showAlert(Alert.AlertType type, String header, String content) {
		Alert alert = new Alert(type);
		alert.setTitle("System Message");
		alert.setHeaderText(header);
		alert.setContentText(content);
		alert.showAndWait();
	}
}