package com.airlines_sql.controllers;

import com.airlines_sql.utils.DatabaseUtil;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.sql.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CreateOneOffFlightController {

	@FXML private ComboBox<String>    cbFrom;
	@FXML private ComboBox<String>    cbTo;
	@FXML private DatePicker          dpDate;
	@FXML private TextField           tfTime;
	@FXML private ComboBox<Integer>   cbAircraft;
	@FXML private Button              btnCreate;

	private static final double  AVG_SPEED_KMH = 800.0;
	private static final Duration TURNAROUND    = Duration.ofHours(1);

	@FXML
	private void initialize() {
		loadAirports();
		loadAircrafts();
	}

	private void loadAirports() {
		String sql = "SELECT airport_id, airport_iata FROM airport";
		List<String> items = new ArrayList<>();
		try (Connection conn = DatabaseUtil.getConnection();
			 Statement st = conn.createStatement();
			 ResultSet rs = st.executeQuery(sql)) {

			while (rs.next()) {
				items.add(rs.getInt("airport_id") + " - " + rs.getString("airport_iata"));
			}
			cbFrom.setItems(FXCollections.observableArrayList(items));
			cbTo  .setItems(FXCollections.observableArrayList(items));

		} catch (SQLException ex) {
			showAlert(Alert.AlertType.ERROR, "Error", "Cannot load airports");
		}
	}

	private void loadAircrafts() {
		String sql = "SELECT aircraft_id FROM aircraft";
		List<Integer> items = new ArrayList<>();
		try (Connection conn = DatabaseUtil.getConnection();
			 Statement st = conn.createStatement();
			 ResultSet rs = st.executeQuery(sql)) {

			while (rs.next()) {
				items.add(rs.getInt("aircraft_id"));
			}
			cbAircraft.setItems(FXCollections.observableArrayList(items));

		} catch (SQLException ex) {
			showAlert(Alert.AlertType.ERROR, "Error", "Cannot load aircrafts");
		}
	}

	@FXML
	private void handleCreate() {
		LocalDate date = dpDate.getValue();
		if (date == null) {
			showAlert(Alert.AlertType.WARNING, "Validation", "Select departure date.");
			return;
		}
		LocalTime time;
		try {
			time = LocalTime.parse(tfTime.getText(), DateTimeFormatter.ofPattern("HH:mm"));
		} catch (Exception ex) {
			showAlert(Alert.AlertType.WARNING, "Validation", "Time must be in HH:mm.");
			return;
		}
		LocalDateTime dep = LocalDateTime.of(date, time);
		if (dep.isBefore(LocalDateTime.now())) {
			showAlert(Alert.AlertType.WARNING, "Validation", "Departure must be in the future.");
			return;
		}

		if (cbFrom.getValue() == null || cbTo.getValue() == null) {
			showAlert(Alert.AlertType.WARNING, "Validation", "Select both origin and destination.");
			return;
		}
		int fromAirport = Integer.parseInt(cbFrom.getValue().split(" - ")[0]);
		int toAirport   = Integer.parseInt(cbTo .getValue().split(" - ")[0]);
		if (fromAirport == toAirport) {
			showAlert(Alert.AlertType.WARNING, "Validation", "Origin and destination must differ.");
			return;
		}

		Integer routeId = findRouteId(fromAirport, toAirport);
		if (routeId == null) {
			showAlert(Alert.AlertType.ERROR, "Error", "No route between selected airports.");
			return;
		}

		double distance = fetchDistance(routeId);

		Duration flightTime = Duration.ofSeconds((long)(distance / AVG_SPEED_KMH * 3600));
		LocalDateTime arr   = dep.plus(flightTime).plus(TURNAROUND);

		Integer aircraftId = cbAircraft.getValue();
		if (aircraftId == null) {
			showAlert(Alert.AlertType.WARNING, "Validation", "Select an aircraft.");
			return;
		}

		int scheduleId;
		String dow = date.getDayOfWeek()
				.getDisplayName(TextStyle.SHORT, Locale.ENGLISH);  // "Mon","Tue"...
		String insSched =
				"INSERT INTO flight_schedule "
						+ "(route_id, scheduled_departure_time, scheduled_arrival_time, day_of_week, valid_from, valid_to) "
						+ "VALUES (?, ?, ?, CAST(? AS weekday), ?, ?) "
						+ "RETURNING schedule_id";

		try (Connection conn = DatabaseUtil.getConnection();
			 PreparedStatement ps = conn.prepareStatement(insSched)) {

			ps.setInt   (1, routeId);
			ps.setTime  (2, Time.valueOf(dep.toLocalTime()));
			ps.setTime  (3, Time.valueOf(arr.toLocalTime()));
			ps.setString(4, dow);
			ps.setDate  (5, Date.valueOf(date));
			ps.setDate  (6, Date.valueOf(date));

			try (ResultSet rs = ps.executeQuery()) {
				rs.next();
				scheduleId = rs.getInt(1);
			}
		} catch (SQLException ex) {
			showAlert(Alert.AlertType.ERROR, "DB Error", "Failed to insert schedule.\n" + ex.getMessage());
			return;
		}

		String insInst =
				"INSERT INTO flight_instance " +
						"(schedule_id, scheduled_departure, scheduled_arrival, aircraft_id) " +
						"VALUES (?, ?, ?, ?)";
		try (Connection conn = DatabaseUtil.getConnection();
			 PreparedStatement ps = conn.prepareStatement(insInst)) {

			ps.setInt      (1, scheduleId);
			ps.setTimestamp(2, Timestamp.valueOf(dep));
			ps.setTimestamp(3, Timestamp.valueOf(arr));
			ps.setInt      (4, aircraftId);
			ps.executeUpdate();

		} catch (SQLException ex) {
			showAlert(Alert.AlertType.ERROR, "DB Error", "Failed to insert flight instance.\n" + ex.getMessage());
			return;
		}

		showAlert(Alert.AlertType.INFORMATION, "Success", "One-off flight created.");
		((Stage)btnCreate.getScene().getWindow()).close();
	}

	private Integer findRouteId(int fromAirport, int toAirport) {
		String sql = "SELECT route_id FROM route WHERE airport_from=? AND airport_to=?";
		try (Connection conn = DatabaseUtil.getConnection();
			 PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setInt(1, fromAirport);
			ps.setInt(2, toAirport);
			try (ResultSet rs = ps.executeQuery()) {
				return rs.next() ? rs.getInt("route_id") : null;
			}
		} catch (SQLException e) {
			return null;
		}
	}

	private double fetchDistance(int routeId) {
		String sql = "SELECT distance_km FROM route WHERE route_id=?";
		try (Connection conn = DatabaseUtil.getConnection();
			 PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setInt(1, routeId);
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					return rs.getDouble("distance_km");
				}
			}
		} catch (SQLException ignored) { }
		return 0;
	}

	private void showAlert(Alert.AlertType type, String header, String content) {
		Alert a = new Alert(type);
		a.setTitle("Attention");
		a.setHeaderText(header);
		a.setContentText(content);
		a.showAndWait();
	}
}
