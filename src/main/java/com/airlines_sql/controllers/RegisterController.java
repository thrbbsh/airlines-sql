package com.airlines_sql.controllers;

import com.airlines_sql.utils.DatabaseUtil;
import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

import java.util.regex.Pattern;

public class RegisterController {

	@FXML private TextField firstNameField;
	@FXML private TextField lastNameField;
	@FXML private DatePicker birthDatePicker;
	@FXML private TextField emailField;
	@FXML private TextField phoneField;
	@FXML private TextField usernameField;
	@FXML private PasswordField passwordField;
	@FXML private PasswordField confirmPasswordField;
	@FXML private Label errorLabel;

	@FXML private Button registerButton;
	@FXML private Button backToLoginButton;

	private static final Pattern PASSWORD_PATTERN =
			Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$");

	@FXML
	private void handleRegister() {
		String firstName = firstNameField.getText().trim();
		String lastName  = lastNameField.getText().trim();
		LocalDate birthDate = birthDatePicker.getValue();
		String email     = emailField.getText().trim();
		String phone     = phoneField.getText().trim();
		String username  = usernameField.getText().trim();
		String password  = passwordField.getText();
		String confirm   = confirmPasswordField.getText();

		// Проверка обязательных полей на стороне приложения
		if (firstName.isEmpty() || lastName.isEmpty() ||
				birthDate == null || email.isEmpty() ||
				username.isEmpty() || password.isEmpty() || confirm.isEmpty()) {

			errorLabel.setTextFill(Color.RED);
			errorLabel.setText("Fill in all required fields");
			return;
		}
		if (!password.equals(confirm)) {
			errorLabel.setTextFill(Color.RED);
			errorLabel.setText("Passwords do not match");
			return;
		}

		if (!PASSWORD_PATTERN.matcher(password).matches()) {
			errorLabel.setTextFill(Color.RED);
			errorLabel.setText("Password must be at least 8 characters, include a letter, a digit, and a special symbol");
			return;
		}

		// Хешируем пароль
		String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

		Connection conn = null;
		PreparedStatement psCust = null;
		PreparedStatement psUser = null;
		ResultSet rs = null;

		try {
			conn = DatabaseUtil.getConnection();
			conn.setAutoCommit(false);

			// 1) Вставляем customer. Если триггер сработает – бросит SQLException
			String insertCustomerSQL =
					"INSERT INTO customer (first_name, last_name, birth_date, email, phone) " +
							"VALUES (?, ?, ?, ?, ?) RETURNING customer_id";
			psCust = conn.prepareStatement(insertCustomerSQL);
			psCust.setString(1, firstName);
			psCust.setString(2, lastName);
			psCust.setObject(3, birthDate);
			psCust.setString(4, email);
			psCust.setString(5, phone);

			rs = psCust.executeQuery();  // здесь, если first_name = '1234', триггер выдаст PSQLException
			int customerId;
			if (rs.next()) {
				customerId = rs.getInt("customer_id");
			} else {
				throw new SQLException("Cannot retrieve customer_id");
			}
			rs.close();
			psCust.close();

			// 2) Вставляем app_user – без валидации триггером (роль USER, хеш)
			String insertUserSQL =
					"INSERT INTO app_user (username, password_hash, role, customer_id) " +
							"VALUES (?, ?, 'USER', ?)";
			psUser = conn.prepareStatement(insertUserSQL);
			psUser.setString(1, username);
			psUser.setString(2, hashedPassword);
			psUser.setInt(3, customerId);

			psUser.executeUpdate();
			psUser.close();

			conn.commit();

			// Успешная регистрация
			errorLabel.setTextFill(Color.GREEN);
			errorLabel.setText("Registration successful!");

			// Блокируем форму после успеха
			registerButton.setDisable(true);
			firstNameField.setDisable(true);
			lastNameField.setDisable(true);
			birthDatePicker.setDisable(true);
			emailField.setDisable(true);
			phoneField.setDisable(true);
			usernameField.setDisable(true);
			passwordField.setDisable(true);
			confirmPasswordField.setDisable(true);

		} catch (SQLException e) {
			if (conn != null) {
				try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
			}
			// Сырой текст ошибки (может содержать несколько строк), разделяем по "\n" и берём только первую
			String fullMessage = e.getMessage();
			String firstLine = fullMessage.split("\\R")[0]; // \\R — любой разделитель строки
			errorLabel.setTextFill(Color.RED);
			errorLabel.setText(firstLine);
		} finally {
			// Закрываем ресурсы
			try { if (rs != null) rs.close(); } catch (SQLException ignored) {}
			try { if (psCust != null) psCust.close(); } catch (SQLException ignored) {}
			try { if (psUser != null) psUser.close(); } catch (SQLException ignored) {}
			try { if (conn != null) conn.close(); } catch (SQLException ignored) {}
		}
	}

	@FXML
	private void backToLogin() {
		try {
			FXMLLoader loader = new FXMLLoader(
					getClass().getResource("/fxml/login.fxml")
			);
			Parent root = loader.load();
			Stage stage = (Stage) firstNameField.getScene().getWindow();
			stage.setScene(new Scene(root, 450, 300));
		} catch (Exception e) {
			e.printStackTrace();
			errorLabel.setTextFill(Color.RED);
			errorLabel.setText("Cannot return to login screen");
		}
	}
}
