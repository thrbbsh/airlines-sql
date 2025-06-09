package com.airlines_sql.controllers;

import com.airlines_sql.models.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import javafx.scene.control.Alert;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;

import java.io.IOException;

public class MainMenuController {

	@FXML private Button btnMyTickets;
	@FXML private Button btnPurchase;
	@FXML private Button btnManageAircraft;
	@FXML private Button btnLogout;

	@FXML private Button btnCreateFlight;
	@FXML private Button btnAssignAircraft;
	@FXML private Button btnManageIncidents;
	@FXML private Button btnCreateSchedule;

	@FXML
	private void initialize() {
		String role = SessionManager.getInstance().getCurrentUserRole();
		boolean isAdmin = "ADMIN".equalsIgnoreCase(role);

		btnManageAircraft.setVisible(isAdmin);
		btnCreateFlight .setVisible(isAdmin);
		btnAssignAircraft.setVisible(isAdmin);
		btnManageIncidents.setVisible(isAdmin);
		btnCreateSchedule.setVisible(isAdmin);
	}

	@FXML
	private void handleCreateSchedule() {
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/create_schedule.fxml"));
			Parent root = loader.load();
			Stage stage = new Stage();
			stage.setTitle("Create New Schedule");
			stage.setScene(new Scene(root));
			stage.show();
		} catch (IOException e) {
			e.printStackTrace();
			showAlert(Alert.AlertType.ERROR, "Error", "Cannot open the schedule creation window.");
		}
	}

	@FXML
	private void handleManageIncidents() {
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/manage_incidents.fxml"));
			Parent root = loader.load();
			Stage stage = new Stage();
			stage.setTitle("Manage Incidents");
			stage.setScene(new Scene(root));
			stage.show();
		} catch (IOException e) {
			e.printStackTrace();
			showAlert(Alert.AlertType.ERROR, "Error", "Failed to open the Incident Management window.");
		}
	}

	@FXML
	private void handleAssignAircraft() {
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/assign_aircraft.fxml"));
			Parent root = loader.load();
			Stage stage = new Stage();
			stage.setTitle("Assign Aircraft");
			stage.setScene(new Scene(root));
			stage.show();
		} catch (IOException e) {
			e.printStackTrace();
			showAlert(Alert.AlertType.ERROR, "Error", "Failed to open the Assign Aircraft window.");
		}
	}

	@FXML
	private void handleCreateFlight() {
		try {
			FXMLLoader loader = new FXMLLoader(
					getClass().getResource("/fxml/create_oneoff_flight.fxml")
			);
			Parent root = loader.load();
			Stage stage = new Stage();
			stage.setTitle("Create One-Off Flight");
			stage.setScene(new Scene(root));
			stage.show();
		} catch (IOException e) {
			e.printStackTrace();
			showAlert(Alert.AlertType.ERROR, "Error", "Cannot open one-off flight window.");
		}
	}

	@FXML
	private void handleMyTickets() {
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/my_tickets.fxml"));
			Parent root = loader.load();

			MyTicketsController controller = loader.getController();
			Integer customerId = SessionManager.getInstance().getCurrentCustomerId();
			controller.initData(customerId);

			Stage stage = new Stage();
			stage.setTitle("My tickets");
			stage.setScene(new Scene(root));
			stage.show();
		} catch (IOException e) {
			e.printStackTrace();
			showAlert(Alert.AlertType.ERROR, "Error", "Failed to open the “My Tickets” window.");
		}
	}

	@FXML
	private void handlePurchase() {
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/search.fxml"));
			Parent root = loader.load();

			SearchController controller = loader.getController();
			Stage stage = new Stage();
			stage.setScene(new Scene(root));
			stage.show();
		} catch (IOException e) {
			e.printStackTrace();
			showAlert(Alert.AlertType.ERROR, "Error", "Failed to open the “Search/Purchase Ticket” window.");
		}
	}

	@FXML
	private void handleManageAircraft() {
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/manage_aircraft.fxml"));
			Parent root = loader.load();

			Stage stage = new Stage();
			stage.setTitle("Aircraft control");
			stage.setScene(new Scene(root));
			stage.show();
		} catch (IOException e) {
			e.printStackTrace();
			new Alert(Alert.AlertType.ERROR, "Failed to open the “Aircraft Management” window.").showAndWait();
		}
	}

	@FXML
	private void handleLogout() {
		SessionManager.getInstance().clearSession();
		Stage stage = (Stage) btnLogout.getScene().getWindow();
		stage.close();
	}

	private void showAlert(Alert.AlertType type, String header, String content) {
		Alert alert = new Alert(type);
		alert.setTitle("Attention");
		alert.setHeaderText(header);
		alert.setContentText(content);
		alert.showAndWait();
	}
}
