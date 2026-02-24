package com.template;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

public class MainController
{
    @FXML
    public Label label;

    @FXML
    private Button logInButton;

    @FXML
    private Button signUpButton;

    @FXML
    private Label WelcomeText;

    @FXML
    private ImageView lgInBtn;

    @FXML
    private ImageView sgnUpImg;

    @FXML
    public void initialize() {
        if (Main.AppState.isLoggedIn) {
            logInButton.setDisable(true);
            signUpButton.setDisable(true);
        }
    }

    @FXML
    public void handleUser(ActionEvent actionEvent) {
        Button current_Button = (Button)actionEvent.getSource();
        Scene current_scene = current_Button.getScene();
        if(current_Button == logInButton) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("Login.fxml"));


                Parent loginRoot = loader.load();


                Scene loginScene = new Scene(loginRoot);


                Stage stage = (Stage) current_Button.getScene().getWindow();


                stage.setScene(loginScene);
                stage.show();
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Could not find or load Login.fxml");
            }
        }

        if(current_Button == signUpButton)  {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("Sign.fxml"));


                Parent signRoot = loader.load();


                Scene signScene = new Scene(signRoot);


                Stage stage = (Stage) current_Button.getScene().getWindow();



                stage.setScene(signScene);
                stage.show();
            }
            catch (Exception e){
                e.printStackTrace();
                System.out.println("Could not find or load Sign.fxml");
            }

        }

        }
    }

