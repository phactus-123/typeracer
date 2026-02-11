package com.template;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class MainController
{
    @FXML
    public Label label;


    @FXML
    public void buttonPressed(ActionEvent actionEvent) {
        label.setText("Clicked");
        System.out.println("Button pressed");
    }
}