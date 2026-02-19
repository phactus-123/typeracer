package com.template;



import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;

import java.io.IOException;


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
    private void getUserInfo(ActionEvent event) {
        Button current_button =  (Button) event.getSource();
        if (current_button == enter) {
            String user = username.getText();
            System.out.println(user);
        }
    }
}
