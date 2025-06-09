package com.airlines_sql.controllers;

import com.airlines_sql.models.SessionManager;
import com.airlines_sql.utils.DatabaseUtil;
import com.sun.javafx.css.StyleManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.mindrot.jbcrypt.BCrypt;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class LoginController {

	@FXML private TextField usernameField;
	@FXML private PasswordField passwordField;
	@FXML private Label errorLabel;
	@FXML private Button loginButton;
	@FXML private Hyperlink showRegisterLink;

	@FXML
	private void initialize() {
		errorLabel.setText("");
	}

	@FXML
	private void handleLogin() {
		String username = usernameField.getText().trim();
		String password = passwordField.getText();

		if (username.isEmpty() || password.isEmpty()) {
			errorLabel.setTextFill(Color.RED);
			errorLabel.setText("Fill in both fields");
			return;
		}

		String sql = "SELECT user_id, password_hash, role, customer_id " +
				"FROM app_user WHERE username = ?";

		try (Connection conn = DatabaseUtil.getConnection();
			 PreparedStatement stmt = conn.prepareStatement(sql)) {

			stmt.setString(1, username);
			ResultSet rs = stmt.executeQuery();

			if (rs.next()) {
				String storedHash = rs.getString("password_hash");
				String role       = rs.getString("role");
				int customerId    = rs.getInt("customer_id");

				if (BCrypt.checkpw(password, storedHash)) {
					errorLabel.setText("");
					System.out.printf(
							"Login successful: username=%s, role=%s, customerId=%d%n",
							username, role, customerId
					);
					SessionManager.getInstance().setCurrentCustomerId(customerId);
					SessionManager.getInstance().setCurrentUserRole(role);
					openMainMenu();
				} else {
					errorLabel.setTextFill(Color.RED);
					errorLabel.setText("Incorrect password");
				}
			} else {
				errorLabel.setTextFill(Color.RED);
				errorLabel.setText("User not found");
			}

			rs.close();

		} catch (Exception e) {
			e.printStackTrace();
			errorLabel.setTextFill(Color.RED);
			errorLabel.setText("Login error: " + e.getMessage());
		}
	}

	@FXML
	private void showRegister() {
		try {
			FXMLLoader loader = new FXMLLoader(
					getClass().getResource("/fxml/register.fxml")
			);
			Parent root = loader.load();
			Stage stage = (Stage) usernameField.getScene().getWindow();
			stage.setScene(new Scene(root, 400, 500));
		} catch (Exception e) {
			e.printStackTrace();
			errorLabel.setTextFill(Color.RED);
			errorLabel.setText("Cannot open registration screen");
		}
	}

	private void openMainMenu() {
		try {
			Stage currentStage = (Stage) loginButton.getScene().getWindow();
			currentStage.close();

			FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main_menu.fxml"));
			Parent root = loader.load();

			Stage menuStage = new Stage();
			menuStage.setTitle("Main menu");
			menuStage.setScene(new Scene(root));
			menuStage.show();
		} catch (IOException ex) {
			ex.printStackTrace();
			showAlert(Alert.AlertType.ERROR, "Error", "Failed to open the main menu.");
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
