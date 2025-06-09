package com.airlines_sql.controllers;

import com.airlines_sql.utils.DatabaseUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

public class PurchaseController {

	@FXML private ComboBox<String> fareCombo;
	@FXML private ComboBox<String> seatCombo;
	@FXML private Label availableSeatsLabel;
	@FXML private TextField totalPriceField;
	@FXML private Button buyButton;

	private int currentInstanceId;
	private int currentUserId;
	private Map<String, TicketInfo> ticketMap = new LinkedHashMap<>();

	private static class TicketInfo {
		final int ticketId;
		final double price;
		TicketInfo(int id, double price) { this.ticketId = id; this.price = price; }
	}

	@FXML
	private void initialize() {
		fareCombo.setOnAction(e -> loadSeatsByFare());
		seatCombo.setOnAction(e -> updateTotalPrice());
	}

	public void initData(int instanceId, int userId) {
		this.currentInstanceId = instanceId;
		this.currentUserId = userId;
		loadFareClasses();
	}

	private void loadFareClasses() {
		String sql = "SELECT fare_id, class FROM fare ORDER BY class DESC";
		ObservableList<String> list = FXCollections.observableArrayList();
		try (Connection conn = DatabaseUtil.getConnection();
			 Statement st = conn.createStatement();
			 ResultSet rs = st.executeQuery(sql)) {
			while (rs.next()) {
				list.add(rs.getInt("fare_id") + " - " + rs.getString("class"));
			}
		} catch (SQLException ex) {
			showAlert(Alert.AlertType.ERROR, "Error loading tariffs", ex.getMessage());
		}
		fareCombo.setItems(list);
		if (!list.isEmpty()) {
			fareCombo.getSelectionModel().selectFirst();
		}
	}

	private void loadSeatsByFare() {
		ticketMap.clear();
		seatCombo.getItems().clear();
		totalPriceField.clear();
		availableSeatsLabel.setText("0");

		String selFare = fareCombo.getValue();
		if (selFare == null) return;
		int fareId = Integer.parseInt(selFare.split(" - ")[0]);

		String sql =
				"SELECT t.ticket_id, t.seat_id, t.price " +
						"FROM ticket t " +
						"LEFT JOIN booking b ON t.ticket_id = b.ticket_id " +
						"WHERE t.instance_id = ? " +
						"  AND t.fare_id = ? " +
						"  AND b.ticket_id IS NULL";

		try (Connection conn = DatabaseUtil.getConnection();
			 PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setInt(1, currentInstanceId);
			ps.setInt(2, fareId);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					int id = rs.getInt("ticket_id");
					String seat = rs.getString("seat_id");
					double price = rs.getDouble("price");
					ticketMap.put(seat, new TicketInfo(id, price));
				}
			}
		} catch (SQLException ex) {
			showAlert(Alert.AlertType.ERROR, "Error loading seats", ex.getMessage());
		}

		List<String> seats = ticketMap.keySet().stream()
				.sorted(Comparator
						.comparingInt((String s) -> Integer.parseInt(s.replaceAll("\\D+", "")))
						.thenComparing((String s) -> s.replaceAll("\\d+", "")))
				.collect(Collectors.toList());

		seatCombo.setItems(FXCollections.observableArrayList(seats));
		availableSeatsLabel.setText(String.valueOf(seats.size()));
		if (!seats.isEmpty()) {
			seatCombo.getSelectionModel().selectFirst();
		}
	}

	private void updateTotalPrice() {
		String seat = seatCombo.getValue();
		if (seat == null) {
			totalPriceField.setText("0.00");
			return;
		}
		double price = ticketMap.get(seat).price;
		totalPriceField.setText(String.format("%.2f", price));
	}

	@FXML
	private void handleBuy() {
		String seat = seatCombo.getValue();
		if (seat == null) {
			showAlert(Alert.AlertType.WARNING, "No location chosen", "Please select a location.");
			return;
		}
		int ticketId = ticketMap.get(seat).ticketId;

		String sql = "INSERT INTO booking(customer_id, status, ticket_id) VALUES (?, 'Confirmed', ?)";
		try (Connection conn = DatabaseUtil.getConnection();
			 PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setInt(1, currentUserId);
			ps.setInt(2, ticketId);
			ps.executeUpdate();
			showAlert(Alert.AlertType.INFORMATION, "Success", "Ticket successfully purchased!");
			((Stage) buyButton.getScene().getWindow()).close();
		} catch (SQLException ex) {
			showAlert(Alert.AlertType.ERROR, "Buying error", ex.getMessage());
		}
	}

	private void showAlert(Alert.AlertType type, String header, String content) {
		Alert alert = new Alert(type);
		alert.setTitle("Attention");
		alert.setHeaderText(header);
		alert.setContentText(content);
		alert.showAndWait();
	}
}