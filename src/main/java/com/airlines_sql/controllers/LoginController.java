package com.airlines_sql.controllers;

import com.airlines_sql.utils.DatabaseUtil;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class LoginController {

	@FXML private TextField usernameField;
	@FXML private PasswordField passwordField;
	@FXML private Label errorLabel;
	@FXML private Button loginButton;           // должен совпадать с fx:id в FXML
	@FXML private Hyperlink showRegisterLink;   // должен совпадать с fx:id в FXML

	@FXML
	private void initialize() {
		// Опционально очистить сообщение об ошибке при старте
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
					// TODO: перейти к следующему окну
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
			stage.setScene(new Scene(root, 400, 500)); // при необходимости подкорректируйте размеры
		} catch (Exception e) {
			e.printStackTrace();
			errorLabel.setTextFill(Color.RED);
			errorLabel.setText("Cannot open registration screen");
		}
	}
}
