package com.template;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.Parent;

public class Main extends Application
{

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception
    {
        primaryStage = stage;
        FXMLLoader loader = new FXMLLoader(Main.class.getResource("main.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root);

        stage.setTitle("TypeRacer");
        stage.setScene(scene);
        stage.show();
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public class AppState {
        public static boolean isLoggedIn = false;
        public static String currentUser = null;
    }

    public static void main(String[] args)
    {
        launch();
    }
}