module typeracer {
    requires javafx.controls;
    requires javafx.fxml;

    exports com.template;
    opens com.template to javafx.fxml;
}