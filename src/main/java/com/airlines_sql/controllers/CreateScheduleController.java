package com.airlines_sql.controllers;

import com.airlines_sql.utils.DatabaseUtil;
import javafx.beans.binding.BooleanBinding;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

import java.sql.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CreateScheduleController {

	public record Airport(int id, String name, String iata) {
		@Override
		public String toString() {
			return name + " (" + iata + ")";
		}
	}

	private static final double AVERAGE_AIRCRAFT_SPEED_KMH = 900.0;
	private static final long GROUND_TIME_BUFFER_MINUTES = 45;

	private static final Map<String, DayOfWeek> DAY_OF_WEEK_MAP = Map.of(
			"Mon", DayOfWeek.MONDAY, "Tue", DayOfWeek.TUESDAY, "Wed", DayOfWeek.WEDNESDAY,
			"Thu", DayOfWeek.THURSDAY, "Fri", DayOfWeek.FRIDAY, "Sat", DayOfWeek.SATURDAY,
			"Sun", DayOfWeek.SUNDAY
	);

	@FXML private ComboBox<Airport> departureAirportComboBox;
	@FXML private ComboBox<Airport> arrivalAirportComboBox;
	@FXML private TextField departureTimeField;
	@FXML private TextField arrivalTimeField;
	@FXML private HBox daysOfWeekBox;
	@FXML private Button createScheduleButton;
	private final DateTimeFormatter flexibleTimeParser = DateTimeFormatter.ofPattern("H:m");
	private final DateTimeFormatter outputTimeFormatter = DateTimeFormatter.ofPattern("HH:mm");

	@FXML
	private void initialize() {
		loadAirports();
		setupValidation();

		arrivalTimeField.setEditable(false);
		arrivalTimeField.setStyle("-fx-control-inner-background: #f0f0f0;");

		departureAirportComboBox.valueProperty().addListener((obs, oldVal, newVal) -> recalculateArrivalTime());
		arrivalAirportComboBox.valueProperty().addListener((obs, oldVal, newVal) -> recalculateArrivalTime());
		departureTimeField.textProperty().addListener((obs, oldVal, newVal) -> recalculateArrivalTime());
	}

	private void recalculateArrivalTime() {
		Airport departureAirport = departureAirportComboBox.getValue();
		Airport arrivalAirport = arrivalAirportComboBox.getValue();
		String departureTimeText = departureTimeField.getText();

		if (departureAirport == null || arrivalAirport == null || departureTimeText.isEmpty()) {
			arrivalTimeField.clear();
			return;
		}

		if (departureAirport.id() == arrivalAirport.id()) {
			arrivalTimeField.setText("Error!");
			return;
		}

		LocalTime departureTime;
		try {
			departureTime = LocalTime.parse(departureTimeText, flexibleTimeParser);
		} catch (DateTimeParseException e) {
			arrivalTimeField.clear();
			return;
		}

		int distance = getRouteDistance(departureAirport.id(), arrivalAirport.id());
		if (distance < 0) {
			arrivalTimeField.setText("No route");
			return;
		}

		double flightDurationHours = (double) distance / AVERAGE_AIRCRAFT_SPEED_KMH;
		long totalTravelMinutes = (long) (flightDurationHours * 60) + GROUND_TIME_BUFFER_MINUTES;

		LocalTime arrivalTime = departureTime.plusMinutes(totalTravelMinutes);
		arrivalTimeField.setText(arrivalTime.format(outputTimeFormatter));
	}

	private int getRouteDistance(int fromId, int toId) {
		String sql = "SELECT distance_km FROM route WHERE airport_from = ? AND airport_to = ?";
		try (Connection conn = DatabaseUtil.getConnection();
			 PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setInt(1, fromId);
			pstmt.setInt(2, toId);
			ResultSet rs = pstmt.executeQuery();
			return rs.next() ? rs.getInt("distance_km") : -1;
		} catch (SQLException e) {
			e.printStackTrace();
			return -1;
		}
	}

	private void loadAirports() {
		String sql = "SELECT airport_id, name, airport_iata FROM airport ORDER BY name";
		try (Connection conn = DatabaseUtil.getConnection();
			 PreparedStatement pstmt = conn.prepareStatement(sql);
			 ResultSet rs = pstmt.executeQuery()) {
			List<Airport> airports = new ArrayList<>();
			while (rs.next()) {
				airports.add(new Airport(rs.getInt("airport_id"), rs.getString("name"), rs.getString("airport_iata")));
			}
			departureAirportComboBox.getItems().setAll(airports);
			arrivalAirportComboBox.getItems().setAll(airports);
		} catch (SQLException e) {
			e.printStackTrace();
			showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to load airports.");
		}
	}

	private void setupValidation() {
		BooleanBinding formInvalid = departureAirportComboBox.valueProperty().isNull()
				.or(arrivalAirportComboBox.valueProperty().isNull())
				.or(departureTimeField.textProperty().isEmpty())
				.or(arrivalTimeField.textProperty().isEmpty())
				.or(new BooleanBinding() {
					{
						daysOfWeekBox.getChildren().forEach(node -> {
							if (node instanceof CheckBox) super.bind(((CheckBox) node).selectedProperty());
						});
					}
					@Override
					protected boolean computeValue() {
						return daysOfWeekBox.getChildren().stream()
								.filter(CheckBox.class::isInstance).map(CheckBox.class::cast)
								.noneMatch(CheckBox::isSelected);
					}
				});
		createScheduleButton.disableProperty().bind(formInvalid);
	}

	@FXML
	private void handleCreateSchedule() {
		Airport departureAirport = departureAirportComboBox.getValue();
		Airport arrivalAirport = arrivalAirportComboBox.getValue();
		if (departureAirport.id() == arrivalAirport.id()) {
			showAlert(Alert.AlertType.WARNING, "Invalid Route", "Departure and arrival airports cannot be the same.");
			return;
		}

		LocalTime departureTime, arrivalTime;
		try {
			departureTime = LocalTime.parse(departureTimeField.getText(), flexibleTimeParser);
			arrivalTime = LocalTime.parse(arrivalTimeField.getText(), flexibleTimeParser);
		} catch (DateTimeParseException e) {
			showAlert(Alert.AlertType.WARNING, "Invalid Time Format", "Please ensure time fields are correct. Use HH:MM or H:M format.");
			return;
		}

		List<CheckBox> selectedDays = daysOfWeekBox.getChildren().stream()
				.filter(CheckBox.class::isInstance).map(CheckBox.class::cast)
				.filter(CheckBox::isSelected).collect(Collectors.toList());

		Connection conn = null;
		try {
			conn = DatabaseUtil.getConnection();
			conn.setAutoCommit(false);
			int routeId = getRouteId(conn, departureAirport.id(), arrivalAirport.id());
			if (routeId == -1) {
				showAlert(Alert.AlertType.ERROR, "Route Not Found", "A route between " + departureAirport.iata() + " and " + arrivalAirport.iata() + " does not exist in the database.");
				conn.rollback();
				return;
			}
			for (CheckBox dayCheckbox : selectedDays) {
				String dayOfWeek = dayCheckbox.getText();
				int scheduleId = createFlightSchedule(conn, routeId, departureTime, arrivalTime, dayOfWeek);
				createFlightInstances(conn, scheduleId, departureTime, arrivalTime, dayOfWeek);
			}
			conn.commit();
			showAlert(Alert.AlertType.INFORMATION, "Success", "Schedules and corresponding flights created!");
			createScheduleButton.getScene().getWindow().hide();
		} catch (SQLException e) {
			e.printStackTrace();
			showAlert(Alert.AlertType.ERROR, "Transaction Failed", "An error occurred: " + e.getMessage());
			if (conn != null) try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
		} finally {
			if (conn != null) try { conn.setAutoCommit(true); } catch (SQLException e) { e.printStackTrace(); }
		}
	}

	private int getRouteId(Connection conn, int fromId, int toId) throws SQLException {
		String sql = "SELECT route_id FROM route WHERE airport_from = ? AND airport_to = ?";
		try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setInt(1, fromId);
			pstmt.setInt(2, toId);
			ResultSet rs = pstmt.executeQuery();
			return rs.next() ? rs.getInt("route_id") : -1;
		}
	}

	private int createFlightSchedule(Connection conn, int routeId, LocalTime depTime, LocalTime arrTime, String dayOfWeek) throws SQLException {
		String sql = "INSERT INTO flight_schedule (route_id, scheduled_departure_time, scheduled_arrival_time, day_of_week) " +
				"VALUES (?, ?, ?, ?::weekday) RETURNING schedule_id";
		try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setInt(1, routeId);
			pstmt.setTime(2, Time.valueOf(depTime));
			pstmt.setTime(3, Time.valueOf(arrTime));
			pstmt.setString(4, dayOfWeek);
			ResultSet rs = pstmt.executeQuery();
			if (rs.next()) return rs.getInt("schedule_id");
			else throw new SQLException("Failed to create schedule, no ID obtained.");
		}
	}

	private void createFlightInstances(Connection conn, int scheduleId, LocalTime depTime, LocalTime arrTime, String dayOfWeekStr) throws SQLException {
		// ... остальной код без изменений
		String sql = "INSERT INTO flight_instance (schedule_id, scheduled_departure, scheduled_arrival, aircraft_id) VALUES (?, ?, ?, NULL)";
		DayOfWeek targetDayOfWeek = DAY_OF_WEEK_MAP.get(dayOfWeekStr);
		if (targetDayOfWeek == null) throw new IllegalStateException("Invalid day of week string: " + dayOfWeekStr);
		try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
			LocalDate today = LocalDate.now();
			for (int i = 0; i <= 15; i++) {
				LocalDate futureDate = today.plusDays(i);
				if (futureDate.getDayOfWeek() == targetDayOfWeek) {
					LocalDateTime scheduledDeparture = LocalDateTime.of(futureDate, depTime);
					LocalDateTime scheduledArrival = LocalDateTime.of(futureDate, arrTime);
					if (scheduledArrival.isBefore(scheduledDeparture)) {
						scheduledArrival = scheduledArrival.plusDays(1);
					}
					pstmt.setInt(1, scheduleId);
					pstmt.setTimestamp(2, Timestamp.valueOf(scheduledDeparture));
					pstmt.setTimestamp(3, Timestamp.valueOf(scheduledArrival));
					pstmt.addBatch();
				}
			}
			pstmt.executeBatch();
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