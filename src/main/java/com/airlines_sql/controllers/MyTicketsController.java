package com.airlines_sql.controllers;

import com.airlines_sql.utils.DatabaseUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class MyTicketsController {

	@FXML private TableView<TicketInfo> ticketsTable;
	@FXML private TableColumn<TicketInfo, Integer> colTicketId;
	@FXML private TableColumn<TicketInfo, Integer> colInstanceId;
	@FXML private TableColumn<TicketInfo, String>  colOrigin;
	@FXML private TableColumn<TicketInfo, String>  colDestination;
	@FXML private TableColumn<TicketInfo, Timestamp> colDeparture;
	@FXML private TableColumn<TicketInfo, Timestamp> colArrival;
	@FXML private TableColumn<TicketInfo, String>  colSeat;
	@FXML private TableColumn<TicketInfo, String>  colFareClass;
	@FXML private TableColumn<TicketInfo, Double>  colPrice;
	@FXML private TableColumn<TicketInfo, Timestamp> colBookingDate;
	@FXML private TableColumn<TicketInfo, String>   colStatus;

	private Integer customerId;

	public void initData(Integer customerId) {
		this.customerId = customerId;
		setupTableColumns();
		loadTickets();

		ticketsTable.setOnMouseClicked(event -> {
			if (event.getClickCount() == 2 && !ticketsTable.getSelectionModel().isEmpty()) {
				TicketInfo selectedTicket = ticketsTable.getSelectionModel().getSelectedItem();
				openFlightHistoryWindow(selectedTicket.getInstanceId());
			}
		});
	}

	private void openFlightHistoryWindow(int instanceId) {
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/flight_history.fxml"));
			Parent root = loader.load();

			FlightHistoryController controller = loader.getController();
			controller.initData(instanceId);

			Stage stage = new Stage();
			stage.setTitle("History for Flight ID: " + instanceId);
			stage.setScene(new Scene(root));
			stage.show();
		} catch (IOException e) {
			e.printStackTrace();
			new Alert(Alert.AlertType.ERROR, "Failed to open the flight history window.").showAndWait();
		}
	}

	private void setupTableColumns() {
		colTicketId    .setCellValueFactory(new PropertyValueFactory<>("ticketId"));
		colInstanceId  .setCellValueFactory(new PropertyValueFactory<>("instanceId"));
		colOrigin      .setCellValueFactory(new PropertyValueFactory<>("origin"));
		colDestination .setCellValueFactory(new PropertyValueFactory<>("destination"));
		colDeparture   .setCellValueFactory(new PropertyValueFactory<>("departure"));
		colArrival     .setCellValueFactory(new PropertyValueFactory<>("arrival"));
		colSeat        .setCellValueFactory(new PropertyValueFactory<>("seatId"));
		colFareClass   .setCellValueFactory(new PropertyValueFactory<>("fareClass"));
		colPrice       .setCellValueFactory(new PropertyValueFactory<>("price"));
		colBookingDate .setCellValueFactory(new PropertyValueFactory<>("bookingDate"));
		colStatus      .setCellValueFactory(new PropertyValueFactory<>("status"));

		DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
		setupTimestampColumn(colDeparture, fmt);
		setupTimestampColumn(colArrival,   fmt);
		setupTimestampColumn(colBookingDate, fmt);
	}

	private void setupTimestampColumn(TableColumn<TicketInfo, Timestamp> column, DateTimeFormatter fmt) {
		column.setCellFactory(col -> new TableCell<>() {
			@Override
			protected void updateItem(Timestamp item, boolean empty) {
				super.updateItem(item, empty);
				if (empty || item == null) {
					setText(null);
				} else {
					setText(item.toLocalDateTime()
							.truncatedTo(ChronoUnit.MINUTES)
							.format(fmt));
				}
			}
		});
	}

	private void loadTickets() {
		String sql =
				"SELECT " +
						"  t.ticket_id, " +
						"  fi.instance_id, " +
						"  cf.name AS origin, " +
						"  ct.name AS destination, " +
						"  fi.scheduled_departure, " +
						"  fi.scheduled_arrival, " +
						"  t.seat_id, " +
						"  f.class AS fare_class, " +
						"  t.price AS ticket_price, " +
						"  b.booking_date, " +
						"  b.status AS booking_status " +
						"FROM booking b " +
						"JOIN ticket t ON b.ticket_id = t.ticket_id " +
						"JOIN flight_instance fi ON t.instance_id = fi.instance_id " +
						"JOIN flight_schedule fs ON fi.schedule_id = fs.schedule_id " +
						"JOIN route r ON fs.route_id = r.route_id " +
						"JOIN airport af ON r.airport_from = af.airport_id " +
						"JOIN city cf ON af.city_id = cf.city_id " +
						"JOIN airport at ON r.airport_to = at.airport_id " +
						"JOIN city ct ON at.city_id = ct.city_id " +
						"JOIN fare f ON t.fare_id = f.fare_id " +
						"WHERE b.customer_id = ? " +
						"ORDER BY fi.scheduled_departure ASC";

		try (Connection conn = DatabaseUtil.getConnection();
			 PreparedStatement stmt = conn.prepareStatement(sql)) {

			stmt.setInt(1, customerId);
			try (ResultSet rs = stmt.executeQuery()) {
				ObservableList<TicketInfo> data = FXCollections.observableArrayList();
				while (rs.next()) {
					data.add(new TicketInfo(
							rs.getInt("ticket_id"),
							rs.getInt("instance_id"),
							rs.getString("origin"),
							rs.getString("destination"),
							rs.getTimestamp("scheduled_departure"),
							rs.getTimestamp("scheduled_arrival"),
							rs.getString("seat_id"),
							rs.getString("fare_class"),
							rs.getDouble("ticket_price"),
							rs.getTimestamp("booking_date"),
							rs.getString("booking_status")
					));
				}
				ticketsTable.setItems(data);
			}
		} catch (SQLException e) {
			e.printStackTrace();
			new Alert(Alert.AlertType.ERROR, "Failed to download purchased tickets.").showAndWait();
		}
	}

	public static class TicketInfo {
		private final Integer    ticketId;
		private final Integer    instanceId;
		private final String     origin;
		private final String     destination;
		private final Timestamp  departure;
		private final Timestamp  arrival;
		private final String     seatId;
		private final String     fareClass;
		private final Double     price;
		private final Timestamp  bookingDate;
		private final String     status;

		public TicketInfo(Integer ticketId, Integer instanceId, String origin,
						  String destination, Timestamp departure, Timestamp arrival,
						  String seatId, String fareClass, Double price,
						  Timestamp bookingDate, String status) {
			this.ticketId    = ticketId;
			this.instanceId  = instanceId;
			this.origin      = origin;
			this.destination = destination;
			this.departure   = departure;
			this.arrival     = arrival;
			this.seatId      = seatId;
			this.fareClass   = fareClass;
			this.price       = price;
			this.bookingDate = bookingDate;
			this.status      = status;
		}

		public Integer getTicketId()    { return ticketId; }
		public Integer getInstanceId()  { return instanceId; }
		public String  getOrigin()      { return origin; }
		public String  getDestination() { return destination; }
		public Timestamp getDeparture() { return departure; }
		public Timestamp getArrival()   { return arrival; }
		public String  getSeatId()      { return seatId; }
		public String  getFareClass()   { return fareClass; }
		public Double  getPrice()       { return price; }
		public Timestamp getBookingDate(){ return bookingDate; }
		public String  getStatus()      { return status; }
	}
}
