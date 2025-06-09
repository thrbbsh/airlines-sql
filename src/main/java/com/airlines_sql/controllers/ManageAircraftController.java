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
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class ManageAircraftController {

	@FXML private Label lblCount;
	@FXML private TableView<AircraftInfo> tblAircraft;
	@FXML private TableColumn<AircraftInfo, Integer> colAircraftId;
	@FXML private TableColumn<AircraftInfo, String>  colModelName;
	@FXML private TableColumn<AircraftInfo, String>  colStatus;
	@FXML private TableColumn<AircraftInfo, LocalDate> colManufacture;

	@FXML private ComboBox<String> cmbModel;
	@FXML private DatePicker dpManufacture;
	@FXML private Button btnRegister;

	private final Map<String, Integer> modelNameToId = new HashMap<>();

	@FXML
	private void initialize() {
		setupTableColumns();
		loadModelsIntoCombo();
		reloadAircraftList();
	}

	private void setupTableColumns() {
		colAircraftId.setCellValueFactory(new PropertyValueFactory<>("aircraftId"));
		colModelName   .setCellValueFactory(new PropertyValueFactory<>("modelName"));
		colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
		colManufacture .setCellValueFactory(new PropertyValueFactory<>("manufactureDate"));

		colManufacture.setCellFactory(column -> new TableCell<>() {
			@Override
			protected void updateItem(LocalDate item, boolean empty) {
				super.updateItem(item, empty);
				setText(empty || item == null ? "" : item.toString());
			}
		});

		tblAircraft.setOnMouseClicked(event -> {
			if (event.getClickCount() == 2 && !tblAircraft.getSelectionModel().isEmpty()) {
				AircraftInfo selectedAircraft = tblAircraft.getSelectionModel().getSelectedItem();
				openHistoryWindow(selectedAircraft);
			}
		});
	}

	private void loadModelsIntoCombo() {
		String sql = "SELECT model_id, name FROM aircraft_model ORDER BY name";
		try (Connection conn = DatabaseUtil.getConnection();
			 PreparedStatement stmt = conn.prepareStatement(sql);
			 ResultSet rs = stmt.executeQuery()) {

			ObservableList<String> modelNames = FXCollections.observableArrayList();
			while (rs.next()) {
				int id        = rs.getInt("model_id");
				String name   = rs.getString("name");
				modelNames.add(name);
				modelNameToId.put(name, id);
			}
			cmbModel.setItems(modelNames);
		} catch (SQLException e) {
			e.printStackTrace();
			new Alert(Alert.AlertType.ERROR, "Failed to load the model list.").showAndWait();
		}
	}

	private void reloadAircraftList() {
		String sql =
				"SELECT a.aircraft_id, am.name AS model_name, a.manufacture_date, " +
						"get_aircraft_current_status(a.aircraft_id) AS current_status " +
						"FROM aircraft a " +
						"JOIN aircraft_model am ON a.model_id = am.model_id " +
						"ORDER BY a.aircraft_id";

		ObservableList<AircraftInfo> data = FXCollections.observableArrayList();
		int count = 0;

		try (Connection conn = DatabaseUtil.getConnection();
			 PreparedStatement stmt = conn.prepareStatement(sql);
			 ResultSet rs = stmt.executeQuery()) {

			while (rs.next()) {
				int id = rs.getInt("aircraft_id");
				String name = rs.getString("model_name");
				String status = rs.getString("current_status"); // << ПОЛУЧАЕМ СТАТУС
				Date dt = rs.getDate("manufacture_date");
				LocalDate localDate = dt != null ? dt.toLocalDate() : null;

				data.add(new AircraftInfo(id, name, status, localDate));
				count++;
			}
		} catch (SQLException e) {
			e.printStackTrace();
			new Alert(Alert.AlertType.ERROR, "Failed to load aircraft list.").showAndWait();
			return;
		}

		tblAircraft.setItems(data);
		lblCount.setText(String.valueOf(count));
	}

	private void openHistoryWindow(AircraftInfo aircraft) {
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/aircraft_history.fxml"));
			Parent root = loader.load();

			AircraftHistoryController controller = loader.getController();
			controller.initData(aircraft);

			Stage stage = new Stage();
			stage.setTitle("History for Aircraft ID: " + aircraft.getAircraftId());
			stage.setScene(new Scene(root));
			stage.show();
		} catch (IOException e) {
			e.printStackTrace();
			new Alert(Alert.AlertType.ERROR, "Failed to open the history window.").showAndWait();
		}
	}

	@FXML
	private void handleRegister() {
		String selectedModelName = cmbModel.getValue();
		LocalDate manufactureDate = dpManufacture.getValue();

		if (selectedModelName == null || manufactureDate == null) {
			new Alert(Alert.AlertType.WARNING, "Enter model and date of manufacture.").showAndWait();
			return;
		}

		Integer modelId = modelNameToId.get(selectedModelName);
		if (modelId == null) {
			new Alert(Alert.AlertType.ERROR, "Model ID could not be determined.").showAndWait();
			return;
		}

		String sqlInsert =
				"INSERT INTO aircraft (model_id, manufacture_date) VALUES (?, ?)";

		try (Connection conn = DatabaseUtil.getConnection();
			 PreparedStatement stmt = conn.prepareStatement(sqlInsert)) {

			stmt.setInt(1, modelId);
			stmt.setDate(2, java.sql.Date.valueOf(manufactureDate));
			int affected = stmt.executeUpdate();

			if (affected == 0) {
				new Alert(Alert.AlertType.ERROR, "Failed to register an aircraft.").showAndWait();
				return;
			}
		} catch (SQLException e) {
			e.printStackTrace();
			new Alert(Alert.AlertType.ERROR, "Aircraft check-in error:\n" + e.getMessage()).showAndWait();
			return;
		}

		cmbModel.getSelectionModel().clearSelection();
		dpManufacture.setValue(null);

		reloadAircraftList();
		new Alert(Alert.AlertType.INFORMATION, "Aircraft successfully registered.").showAndWait();
	}

	public static class AircraftInfo {
		private final Integer   aircraftId;
		private final String    modelName;
		private final String    status;
		private final LocalDate manufactureDate;

		public AircraftInfo(Integer aircraftId, String modelName, String status, LocalDate manufactureDate) {
			this.aircraftId      = aircraftId;
			this.modelName       = modelName;
			this.status          = status;
			this.manufactureDate = manufactureDate;
		}

		public Integer   getAircraftId()      { return aircraftId; }
		public String    getModelName()       { return modelName; }
		public String    getStatus()          { return status; }
		public LocalDate getManufactureDate() { return manufactureDate; }
	}
}
