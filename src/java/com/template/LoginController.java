package com.template;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import javafx.stage.Stage;
import javafx.scene.Parent;
import javafx.scene.Node;

public class LoginController {

    @FXML private AnchorPane  pane;
    @FXML private BorderPane  border_pane;
    @FXML private ImageView   bg_img;
    @FXML private Button      enter;
    @FXML private TextField   username;
    @FXML private Button      backButton;
    @FXML private Button      createButton;

    private static final String DB_PATH      = "src/resources/db.txt";
    private static final String SESSION_PATH = "src/resources/sessions.txt";

    // ── session helpers ───────────────────────────────────────────────────────

    /** Returns true if the username is currently logged in on another instance. */
    private boolean isSessionActive(String user) {
        try {
            File f = new File(SESSION_PATH);
            if (!f.exists()) return false;
            Scanner sc = new Scanner(f);
            while (sc.hasNextLine()) {
                if (sc.nextLine().trim().equals(user)) { sc.close(); return true; }
            }
            sc.close();
        } catch (FileNotFoundException ignored) {}
        return false;
    }

    /** Appends the username to sessions.txt on successful login. */
    public static void registerSession(String user) {
        try {
            File f = new File(SESSION_PATH);
            f.getParentFile().mkdirs();
            FileWriter fw = new FileWriter(f, true);
            fw.write(user + "\n");
            fw.close();
        } catch (IOException e) { e.printStackTrace(); }
    }

    /** Removes the username from sessions.txt on logout / app close. */
    public static void removeSession(String user) {
        if (user == null) return;
        File f = new File(SESSION_PATH);
        if (!f.exists()) return;
        try {
            List<String> lines = new ArrayList<>(Files.readAllLines(f.toPath()));
            lines.removeIf(l -> l.trim().equals(user));
            FileWriter fw = new FileWriter(f, false);
            for (String l : lines) fw.write(l + "\n");
            fw.close();
        } catch (IOException e) { e.printStackTrace(); }
    }

    // ── db helper ─────────────────────────────────────────────────────────────

    private boolean isExist(String username) {
        try {
            File file = new File(DB_PATH);
            Scanner sc = new Scanner(file);
            while (sc.hasNext()) {
                String ln          = sc.next();
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
        } catch (FileNotFoundException e) { e.printStackTrace(); }
        return false;
    }

    // ── event handler ─────────────────────────────────────────────────────────

    @FXML
    public void getUserInfo(ActionEvent event) {
        Object source = event.getSource();
        if (source == username) {
            source = (enter != null) ? enter : createButton;
        }

        if (source == enter) {
            String user = username.getText().trim();
            if (user.isEmpty() || !isExist(user)) {
                showError("Login failed!", "Invalid username. Try signing up instead!");
                return;
            }
            if (isSessionActive(user)) {
                showError("Login failed!", "\"" + user + "\" is already logged in on another window.");
                return;
            }
            System.out.println("Login success!");
            Main.AppState.isLoggedIn  = true;
            Main.AppState.currentUser = user;
            registerSession(user);
            Main.switchScene("main.fxml", event);
        }

        else if (source == createButton) {
            String newUser = username.getText().trim();
            if (newUser.isEmpty() || isExist(newUser)) {
                showError("Sign Up failed!", "Username already exists! Try logging in instead.");
                return;
            }
            try {
                FileWriter writer = new FileWriter(DB_PATH, true);
                writer.write(newUser + " 0 0 0\n");
                writer.close();
                Main.AppState.currentUser  = newUser;
                Main.AppState.isLoggedIn   = true;
                MainController.testCount   = 0;
                MainController.avgWpm      = 0;
                MainController.avgAccuracy = 0.0;
                registerSession(newUser);
                Main.switchScene("main.fxml", event);
                System.out.println("Sign up success!");
            } catch (IOException e) { e.printStackTrace(); }
        }

        else if (source == backButton) {
            Main.switchScene("main.fxml", event);
        }
    }

    private void showError(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
