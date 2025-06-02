package com.airlines_sql;

import com.airlines_sql.utils.DatabaseUtil;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        // При старте показываем окно логина
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/login.fxml"));
        primaryStage.setTitle("Airline Simulator — Login");
        primaryStage.setScene(new Scene(root, 400, 300));
        primaryStage.show();
        System.out.println("Ok!");
    }

    public static void main(String[] args) {
        // Если нужно каждый раз сбрасывать БД:
        try {
            DatabaseUtil.resetDatabase();
        } catch (Exception e) {
            e.printStackTrace();
        }
        launch(args);
    }
}
