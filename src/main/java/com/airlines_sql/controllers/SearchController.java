package com.airlines_sql.controllers;

import com.airlines_sql.models.SessionManager;
import com.airlines_sql.utils.DatabaseUtil;
import com.airlines_sql.models.Flight;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class SearchController {

	@FXML private ComboBox<String> originCombo;
	@FXML private ComboBox<String> destinationCombo;
	@FXML private DatePicker datePicker;
	@FXML private Button searchButton;
	@FXML private TableView<Flight> resultsTable;

	@FXML private TableColumn<Flight, Integer> colFlightId;
	@FXML private TableColumn<Flight, String> colOrigin;
	@FXML private TableColumn<Flight, String> colDestination;
	@FXML private TableColumn<Flight, Timestamp> colDeparture;
	@FXML private TableColumn<Flight, Timestamp> colArrival;
	@FXML private TableColumn<Flight, Double> colPrice;
	@FXML private TableColumn<Flight,String> colStatus;
	@FXML private TableColumn<Flight, String> colOriginIata;
	@FXML private TableColumn<Flight, String> colDestinationIata;


	private ObservableList<String> cityList = FXCollections.observableArrayList();

	private boolean originAdjusting = false;
	private boolean destAdjusting   = false;

	@FXML
	private void initialize() {
		DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

		colFlightId      .setCellValueFactory(new PropertyValueFactory<>("flightInstanceId"));
		colOrigin        .setCellValueFactory(new PropertyValueFactory<>("originCity"));
		colOriginIata    .setCellValueFactory(new PropertyValueFactory<>("originIata"));
		colDestination   .setCellValueFactory(new PropertyValueFactory<>("destinationCity"));
		colDestinationIata.setCellValueFactory(new PropertyValueFactory<>("destinationIata"));
		colDeparture     .setCellValueFactory(new PropertyValueFactory<>("scheduledDeparture"));
		colArrival       .setCellValueFactory(new PropertyValueFactory<>("scheduledArrival"));
		colPrice         .setCellValueFactory(new PropertyValueFactory<>("price"));
		colStatus        .setCellValueFactory(new PropertyValueFactory<>("status"));
		resultsTable.getColumns().add(colStatus);

		colDeparture.setCellFactory(col -> new TableCell<Flight, Timestamp>() {
			@Override protected void updateItem(Timestamp item, boolean empty) {
				super.updateItem(item, empty);
				if (empty || item == null) setText(null);
				else setText(item.toLocalDateTime()
						.truncatedTo(ChronoUnit.MINUTES)
						.format(fmt));
			}
		});
		colArrival.setCellFactory(col -> new TableCell<Flight, Timestamp>() {
			@Override protected void updateItem(Timestamp item, boolean empty) {
				super.updateItem(item, empty);
				if (empty || item == null) setText(null);
				else setText(item.toLocalDateTime()
						.truncatedTo(ChronoUnit.MINUTES)
						.format(fmt));
			}
		});

		loadCitiesFromDatabase();
		originCombo.setEditable(true);
		destinationCombo.setEditable(true);
		setupAutoCompleteForCombo(originCombo, cityList, true);
		setupAutoCompleteForCombo(destinationCombo, cityList, false);

		datePicker.getEditor().textProperty().addListener((obs, oldText, newText) -> {
			if (newText == null || newText.trim().isEmpty()) datePicker.setValue(null);
		});

		resultsTable.setRowFactory(tv -> {
			TableRow<Flight> row = new TableRow<>();
			row.setOnMouseClicked(ev -> {
				if (ev.getClickCount() == 2 && !row.isEmpty()) {
					Flight f = row.getItem();
					if (f.isAvailable()) {
						openPurchaseWindow(f);
					} else {
						showAlert(Alert.AlertType.INFORMATION,
								"Not yet issued",
								"Tickets for this flight are not yet issued.");
					}
				}
			});
			return row;
		});

	}

	private void setupAutoCompleteForCombo(ComboBox<String> comboBox,
										   ObservableList<String> data,
										   boolean isOrigin) {
		comboBox.setItems(data);
		comboBox.setVisibleRowCount(5);

		boolean[] adjustingFlag = isOrigin
				? new boolean[]{ originAdjusting ?  true : false }
				: new boolean[]{ destAdjusting   ?  true : false };

		TextField editor = comboBox.getEditor();

		editor.textProperty().addListener((obs, oldValue, newValue) -> {
			if (isOrigin && originAdjusting) return;
			if (!isOrigin && destAdjusting)   return;

			if (newValue == null) return;
			String typed = newValue.trim().toLowerCase();

			if (typed.isEmpty()) {
				if (isOrigin)     originAdjusting = true;
				else              destAdjusting   = true;

				comboBox.setItems(data);
				comboBox.hide();
				comboBox.show();

				if (isOrigin)     originAdjusting = false;
				else              destAdjusting   = false;
			} else {
				ObservableList<String> filtered = FXCollections.observableArrayList();
				for (String item : data) {
					if (item.toLowerCase().contains(typed)) {
						filtered.add(item);
					}
				}

				if (isOrigin)     originAdjusting = true;
				else              destAdjusting   = true;

				comboBox.setItems(filtered);
				comboBox.hide();
				comboBox.show();

				if (isOrigin)     originAdjusting = false;
				else              destAdjusting   = false;
			}
		});

		comboBox.setOnAction(event -> {
			if (isOrigin && originAdjusting) return;
			if (!isOrigin && destAdjusting)   return;

			String selected = comboBox.getSelectionModel().getSelectedItem();
			if (selected != null) {
				if (isOrigin) {
					originAdjusting = true;
					editor.setText(selected);
					comboBox.setValue(selected);
					originAdjusting = false;
				} else {
					destAdjusting = true;
					editor.setText(selected);
					comboBox.setValue(selected);
					destAdjusting   = false;
				}
			}

			if (isOrigin) {
				originAdjusting = true;
				comboBox.setItems(data);
				originAdjusting = false;
			} else {
				destAdjusting = true;
				comboBox.setItems(data);
				destAdjusting   = false;
			}

			comboBox.hide();
		});

		comboBox.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
			if (!isNowFocused) {
				String text = editor.getText();
				boolean exists = (text != null && data.contains(text));

				if (!exists) {
					// Стереть “левый” ввод
					if (isOrigin) {
						originAdjusting = true;
						editor.clear();
						comboBox.setValue(null);
						originAdjusting = false;
					} else {
						destAdjusting = true;
						editor.clear();
						comboBox.setValue(null);
						destAdjusting   = false;
					}
				}

				// Обязательно вернуть полный список
				if (isOrigin) {
					originAdjusting = true;
					comboBox.setItems(data);
					originAdjusting = false;
				} else {
					destAdjusting = true;
					comboBox.setItems(data);
					destAdjusting   = false;
				}
			}
		});
	}

	private void loadCitiesFromDatabase() {
		String sql = "SELECT DISTINCT c.name " +
				"FROM city c " +
				"JOIN airport a ON c.city_id = a.city_id " +
				"ORDER BY c.name";
		try (Connection conn = DatabaseUtil.getConnection();
			 PreparedStatement stmt = conn.prepareStatement(sql);
			 ResultSet rs = stmt.executeQuery()) {

			cityList.clear();
			while (rs.next()) {
				cityList.add(rs.getString("name"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	@FXML
	private void handleSearch() {
		String origin      = originCombo.getValue();
		String destination = destinationCombo.getValue();
		java.time.LocalDate date = datePicker.getValue();

		if (origin == null || destination == null || origin.trim().isEmpty() || destination.trim().isEmpty()) {
			showAlert(Alert.AlertType.WARNING, "Both cities are unselected",
					"Please enter or select your departure city and destination city.");
			return;
		}

		List<Flight> flights = fetchFlights(origin, destination, date);
		resultsTable.setItems(FXCollections.observableArrayList(flights));
	}

	private List<Flight> fetchFlights(String originCity, String destinationCity, LocalDate unused) {
		List<Flight> result = new ArrayList<>();

		String sql =
				"WITH dates AS ( " +
						"    SELECT gs::date AS dep_date, to_char(gs::date, 'Dy') AS dow " +
						"      FROM generate_series(current_date, current_date + INTERVAL '20 days', '1 day') AS gs " +
						"), " +
						"sched AS ( " +
						"    SELECT fs.schedule_id, fs.scheduled_departure_time, fs.scheduled_arrival_time, " +
						"           r.route_id, af.airport_iata AS origin_iata, at.airport_iata AS dest_iata, " +
						"           fs.day_of_week, fs.valid_from, fs.valid_to " +
						"      FROM flight_schedule fs " +
						"      JOIN route r ON fs.route_id = r.route_id " +
						"      JOIN airport af ON r.airport_from = af.airport_id " +
						"      JOIN city cf   ON af.city_id = cf.city_id " +
						"      JOIN airport at ON r.airport_to   = at.airport_id " +
						"      JOIN city ct   ON at.city_id     = ct.city_id " +
						"     WHERE cf.name = ? AND ct.name = ? " +
						"), " +
						"inst AS ( " +
						"    SELECT fi.instance_id, fi.schedule_id, fi.scheduled_departure, fi.real_price " +
						"      FROM flight_instance fi " +
						"     WHERE DATE(fi.scheduled_departure) >= CURRENT_DATE " +
						") " +
						"SELECT " +
						"  (d.dep_date + s.scheduled_departure_time) AS dep_ts, " +
						"  (d.dep_date + s.scheduled_arrival_time)   AS arr_ts, " +
						"  i.instance_id, i.real_price, " +
						"  s.origin_iata, s.dest_iata " +
						"FROM sched s " +
						"  JOIN dates d " +
						"    ON s.day_of_week = CAST(d.dow AS weekday) " +
						"   AND d.dep_date BETWEEN s.valid_from AND s.valid_to " +
						"  LEFT JOIN inst i " +
						"    ON i.schedule_id = s.schedule_id " +
						"   AND DATE(i.scheduled_departure) = d.dep_date " +
						"ORDER BY dep_ts;";

		try (Connection conn = DatabaseUtil.getConnection();
			 PreparedStatement ps = conn.prepareStatement(sql)) {

			ps.setString(1, originCity);
			ps.setString(2, destinationCity);

			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					Timestamp dep    = rs.getTimestamp("dep_ts");
					Timestamp arr    = rs.getTimestamp("arr_ts");
					Integer instId   = rs.getObject("instance_id", Integer.class);
					BigDecimal bd    = rs.getBigDecimal("real_price");
					Double price     = (bd != null) ? bd.doubleValue() : null;
					String originIata= rs.getString("origin_iata");
					String destIata  = rs.getString("dest_iata");

					result.add(new Flight(
							instId,
							originCity, originIata,
							destinationCity, destIata,
							dep, arr,
							price
					));
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
			showAlert(Alert.AlertType.ERROR,
					"Error when searching flights",
					e.getMessage());
		}
		return result;
	}



	private void showAlert(Alert.AlertType type, String header, String content) {
		Alert alert = new Alert(type);
		alert.setTitle("Attention");
		alert.setHeaderText(header);
		alert.setContentText(content);
		alert.showAndWait();
	}

	private void openPurchaseWindow(Flight flight) {
		try {
			FXMLLoader loader = new FXMLLoader(
					getClass().getResource("/fxml/purchase.fxml")
			);
			Parent root = loader.load();
			PurchaseController controller = loader.getController();

			int instanceId = flight.getFlightInstanceId();

			String sql =
					"SELECT aircraft_id FROM flight_instance WHERE instance_id = ?";
			int aircraftId;
			try (Connection conn = DatabaseUtil.getConnection();
				 PreparedStatement stmt = conn.prepareStatement(sql)) {
				stmt.setInt(1, instanceId);
				try (ResultSet rs = stmt.executeQuery()) {
					if (rs.next()) {
						aircraftId = rs.getInt("aircraft_id");
					} else {
						showAlert(Alert.AlertType.ERROR, "Error", "Flight instance not found.");
						return;
					}
				}
			}

			int currentUserId = SessionManager.getInstance().getCurrentCustomerId();

			controller.initData(instanceId, currentUserId);

			Stage stage = new Stage();
			stage.setTitle("Ticket Purchase");
			stage.setScene(new Scene(root));
			stage.initModality(Modality.APPLICATION_MODAL);
			stage.showAndWait();

		} catch (IOException | SQLException ex) {
			ex.printStackTrace();
			showAlert(Alert.AlertType.ERROR,
					"Error", "Failed to open purchase window:\n" + ex.getMessage()
			);
		}
	}


}
