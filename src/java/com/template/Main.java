package com.template;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.InputStream;

public class Main extends Application
{
    private static final String MONO_FONT_PATH = "/fonts/CascadiaCode-Regular.ttf";
    private static final String MONO_FONT_FALLBACK = "Consolas";

    private static boolean monoFontLoaded = false;
    private static String monoFontFamily = MONO_FONT_FALLBACK;

    @Override
    public void start(Stage stage) throws Exception
    {
        stage.setTitle("TypeRacer");
        stage.setScene(loadScene("main.fxml"));
        stage.show();
    }

    public static void switchScene(String fxmlFile, ActionEvent event) {
        try {
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(loadScene(fxmlFile));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Font monoFont(double size) {
        ensureMonoFontLoaded();
        return Font.font(monoFontFamily, size);
    }

    public static Font monoFont(FontWeight weight, double size) {
        ensureMonoFontLoaded();
        return Font.font(monoFontFamily, weight, size);
    }

    private static Scene loadScene(String fxmlFile) throws IOException {
        ensureMonoFontLoaded();
        FXMLLoader loader = new FXMLLoader(Main.class.getResource(fxmlFile));
        Parent root = loader.load();
        return new Scene(root);
    }

    private static void ensureMonoFontLoaded() {
        if (monoFontLoaded) return;

        monoFontLoaded = true;

        try (InputStream stream = Main.class.getResourceAsStream(MONO_FONT_PATH)) {
            if (stream == null) return;

            Font font = Font.loadFont(stream, 16);
            if (font != null && font.getFamily() != null && !font.getFamily().isBlank()) {
                monoFontFamily = font.getFamily();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public class AppState {
        public static boolean isLoggedIn = false;
        public static boolean testOn = false;
        public static String currentUser = null;

        public static boolean isHost = false;
        public static GameServer server = null;
        public static GameClient client = null;
    }

    public static void main(String[] args)
    {
        launch();
    }
}
