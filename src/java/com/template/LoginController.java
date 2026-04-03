package com.template;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import java.io.File;
import java.io.FileWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Scanner;
import javafx.stage.Stage;
import javafx.scene.Parent;
import javafx.scene.Node;

public class LoginController {

    @FXML
    private AnchorPane pane;

    @FXML
    private BorderPane border_pane;

    @FXML
    private ImageView bg_img;

    @FXML
    private Button enter;

    @FXML
    private TextField username;

    @FXML
    private Button backButton;

    @FXML
    private Button createButton;

    private boolean isExist(String username) {
        try {
            File file = new File("src/resources/db.txt");
            Scanner sc = new Scanner(file);
            while (sc.hasNext()) {
                String ln = sc.next();
                int    testCount   = sc.nextInt();
                int    avgWpm      = sc.nextInt();
                double avgAccuracy = sc.nextDouble();
                if (ln.equals(username)) {

                    MainController.testCount   = testCount;
                    MainController.avgWpm      = avgWpm;
                    MainController.avgAccuracy = avgAccuracy;
                    sc.close();
                    return true;
                }
            }
            sc.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return false;
    }

    @FXML
    public void getUserInfo(ActionEvent event) {
        // If Enter was pressed inside the TextField, treat it as the primary button
        // for the current scene: "enter" (Login) or "createButton" (Sign Up).
        Object source = event.getSource();
        if (source == username) {
            source = (enter != null) ? enter : createButton;
        }

        if (source == enter) {
            String user = username.getText().trim();
            if (user.isEmpty() || !isExist(user)) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Login failed!");
                alert.setContentText("Invalid username. Try signing up instead!");
                alert.showAndWait();
            } else {
                System.out.println("Login success!");
                Main.AppState.isLoggedIn  = true;
                Main.AppState.currentUser = user;
                Main.switchScene("main.fxml", event);
            }
        }

        if (source == createButton) {
            String newUser = username.getText().trim();

            if (newUser.isEmpty() || isExist(newUser)) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Sign Up failed!");
                alert.setContentText("Username already exists! Try login instead.");
                alert.showAndWait();
            } else {
                try {
                    FileWriter writer = new FileWriter("src/resources/db.txt", true);
                    writer.write(newUser + " " + 0 + " " + 0 + " " + 0 + "\n");
                    writer.close();
                    Main.AppState.currentUser  = newUser;
                    Main.AppState.isLoggedIn   = true;

                    MainController.testCount   = 0;
                    MainController.avgWpm      = 0;
                    MainController.avgAccuracy = 0.0;
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Main.switchScene("main.fxml", event);
                System.out.println("Sign up success!");
            }
        }

        if (source == backButton) {
            Main.switchScene("main.fxml", event);
        }
    }
}
