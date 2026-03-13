package com.template;



import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
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
            boolean isFound = false;
            while (sc.hasNext()) {
                String ln = sc.next();
                MainController.testCount = sc.nextInt();
                MainController.avgWpm = sc.nextInt();
                MainController.avgAccuracy = sc.nextDouble();
                if (ln.equals(username)) {
                    return true;
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return false;
    }



    @FXML
    public void getUserInfo(ActionEvent event) {
        Button current_button = (Button) event.getSource();
        if (current_button == enter) {
            String user = username.getText();
            System.out.println(user);
            if (!isExist(user) || user.trim().equals("")) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Login failed!");
                alert.setContentText("Invalid username. Try signing up instead!");
                alert.showAndWait();
            }
            else {
                System.out.println("Login success!");
                Main.AppState.isLoggedIn = true;
                Main.AppState.currentUser = user;
                Main.switchScene("Main.fxml", event);
            }

        }

        if (current_button == createButton) {
            String newUser = username.getText();

            if(isExist(newUser) || newUser.equals("")) {
                System.out.println("Username already exists!");
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Sign Up failed!");
                alert.setContentText("Username already exists! Try login instead.");
                alert.showAndWait();

            }
            else {
                try {
                    FileWriter writer = new FileWriter("src/resources/db.txt", true);
                    writer.write(newUser + " " +0 + " " + 0 +" " + 0+ "\n");
                    writer.close();
                    Main.AppState.currentUser = newUser;
                    Main.AppState.isLoggedIn = true;
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Main.switchScene("Main.fxml", event);
                System.out.println("Sign up success!");
            }
        }

        if (current_button == backButton) {
            try {
                Main.switchScene("Main.fxml", event);
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Could not find or load main.fxml");
            }
        }

    }

}

