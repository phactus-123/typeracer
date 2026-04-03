package com.template;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.text.TextFlow;
import javafx.scene.text.Text;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.image.ImageView;
import javafx.application.Platform;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import javafx.scene.paint.Color;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.io.OutputStreamWriter;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class MainController
{

    @FXML public Label label;

    @FXML private Button logInButton;
    @FXML private Button signUpButton;
    @FXML private ImageView WelcomeText;

    @FXML private Pane startupPane;
    @FXML private Pane usernamePane;
    @FXML private Pane typingPane;

    @FXML private Label username;
    @FXML private Label wpmLabel;
    @FXML private Label accuracy;
    @FXML private Label wpm;

    @FXML private Button test;
    @FXML private Button race;
    @FXML private Button stopButton;

    @FXML private TextFlow typingBlock;
    @FXML private TextArea currentWord;

    @FXML private Button dashboardButton;
    @FXML private Button leaderboardButton;
    @FXML private Button myStatsButton;
    @FXML private Button aboutButton;
    @FXML private Button logoutButton;

    @FXML private Pane leaderboardPane;

    @FXML private TableView<LeaderboardEntry>       leaderboardTable;
    @FXML private TableColumn<LeaderboardEntry, Integer> rankCol;
    @FXML private TableColumn<LeaderboardEntry, String>  nameCol;
    @FXML private TableColumn<LeaderboardEntry, Integer> wpmCol;
    @FXML private TableColumn<LeaderboardEntry, String>  accCol;
    @FXML private TableColumn<LeaderboardEntry, Integer> testsCol;

    @FXML private Pane  myStatsPane;

    @FXML private Pane  aboutPane;
    @FXML private Label statUsernameLabel;
    @FXML private Label statTestCount;
    @FXML private Label statAvgWpm;
    @FXML private Label statAvgAccuracy;
    @FXML private Label statLastWpm;
    @FXML private Label statLastAccuracy;
    @FXML private Label statLastChars;
    @FXML private Label statHint;

    private static String[] contents;

    private int charCount        = 0;
    private int correctCharCount = 0;

    public static int    avgWpm;
    public static double avgAccuracy;
    public static int    testCount;

    private int    lastWpm      = 0;
    private double lastAccuracy = 0.0;
    private int    lastChars    = 0;

    private int wordCount        = 0;
    private int correctWordCount = 0;
    private int incorrectWordCount = 0;
    private int totalWordCount   = 0;

    private double startTime;
    private double endTime;
    private double duration;

    private int    wpmMeter      = 0;
    private double accuracyMeter = 0.0;

    private Timeline timeline;
    private boolean  testing = false;

    @FXML
    public void initialize() {
        applyDashboardFonts();

        rankCol .setCellValueFactory(new PropertyValueFactory<>("rank"));
        nameCol .setCellValueFactory(new PropertyValueFactory<>("name"));
        wpmCol  .setCellValueFactory(new PropertyValueFactory<>("avgWpm"));
        accCol  .setCellValueFactory(new PropertyValueFactory<>("avgAccuracyStr"));
        testsCol.setCellValueFactory(new PropertyValueFactory<>("tests"));

        username.setText("USER: " + Main.AppState.currentUser);
        togglePane(usernamePane);
        togglePane(typingPane);

        if (Main.AppState.isLoggedIn) {
            togglePane(startupPane);
        }
        if (Main.AppState.isLoggedIn && !Main.AppState.testOn) {
            updateLabel();
            togglePane(usernamePane);
        }

        currentWord.textProperty().addListener((obs, oldText, newText) -> {
            if (newText.endsWith(" ") && !oldText.endsWith(" ")) {

                if (!testing) {
                    startTime = System.currentTimeMillis();
                    testing   = true;
                    startTimer();
                }

                String typedWord = newText.trim();
                if (typedWord.isEmpty()) {
                    Platform.runLater(currentWord::clear);
                    return;
                }

                charCount += typedWord.length() + 1;

                if (typedWord.equals(contents[wordCount])) {
                    setColor();
                    correctCharCount += typedWord.length() + 1;
                    correctWordCount++;
                    wordCount++;
                    if (wordCount >= totalWordCount) {
                        finishTest();
                        return;
                    }

                    setUnderline();
                    Platform.runLater(currentWord::clear);
                } else {
                    setColor("RED");
                    incorrectWordCount++;
                }
            }
        });
    }

    private void applyDashboardFonts() {
        username.setFont(Main.monoFont(FontWeight.BOLD, 22));
        wpm.setFont(Main.monoFont(16));
        accuracy.setFont(Main.monoFont(16));
        wpmLabel.setFont(Main.monoFont(FontWeight.BOLD, 24));
    }

    @FXML
    public void handleNavigation(ActionEvent event) {
        if (!Main.AppState.isLoggedIn) return;

        Button src = (Button) event.getSource();

        if (src == dashboardButton) {
            navigateTo(usernamePane);

        } else if (src == leaderboardButton) {
            loadLeaderboardData();
            navigateTo(leaderboardPane);

        } else if (src == myStatsButton) {
            loadMyStats();
            navigateTo(myStatsPane);

        } else if (src == aboutButton) {
            navigateTo(aboutPane);
        }

    }

    private void navigateTo(Pane target) {
        for (Pane p : new Pane[]{ startupPane, usernamePane, leaderboardPane, myStatsPane, aboutPane }) {
            p.setVisible(false);
            p.setDisable(true);
        }
        target.setVisible(true);
        target.setDisable(false);
    }

    private void loadLeaderboardData() {
        List<LeaderboardEntry> entries = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream("src/resources/db.txt"), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] parts = line.split("\\s+");
                if (parts.length < 4) continue;
                entries.add(new LeaderboardEntry(
                        parts[0],
                        Integer.parseInt(parts[1]),
                        Integer.parseInt(parts[2]),
                        Double.parseDouble(parts[3])
                ));
            }
        } catch (IOException e) {
            leaderboardTable.setPlaceholder(
                    new Label("Could not load leaderboard – check db.txt."));
            return;
        }

        entries.sort((a, b) -> {
            if (b.getAvgWpm() != a.getAvgWpm())
                return Integer.compare(b.getAvgWpm(), a.getAvgWpm());
            return Double.compare(b.getAvgAccuracy(), a.getAvgAccuracy());
        });

        for (int i = 0; i < entries.size(); i++) {
            entries.get(i).setRank(i + 1);
        }

        leaderboardTable.getItems().setAll(entries);

        String currentUser = Main.AppState.currentUser;
        leaderboardTable.setRowFactory(tv -> new TableRow<LeaderboardEntry>() {
            @Override
            protected void updateItem(LeaderboardEntry item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("current-user-row");
                if (!empty && item != null && item.getName().equals(currentUser)) {
                    getStyleClass().add("current-user-row");
                }
            }
        });

        if (entries.isEmpty()) {
            leaderboardTable.setPlaceholder(
                    new Label("No players yet – complete a test to appear here!"));
        }
    }

    private void loadMyStats() {
        statUsernameLabel.setText("👤  " + Main.AppState.currentUser);

        boolean found = false;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream("src/resources/db.txt"), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] parts = line.split("\\s+");
                if (parts.length >= 4 && parts[0].equals(Main.AppState.currentUser)) {
                    int    tests = Integer.parseInt(parts[1]);
                    int    wpmV  = Integer.parseInt(parts[2]);
                    double acc   = Double.parseDouble(parts[3]);

                    statTestCount  .setText(String.valueOf(tests));
                    statAvgWpm     .setText(String.valueOf(wpmV));
                    statAvgAccuracy.setText(String.format("%.1f%%", acc));
                    statHint.setVisible(tests == 0);
                    found = true;
                    break;
                }
            }
        } catch (IOException ignored) { }

        if (!found) {
            statTestCount  .setText("0");
            statAvgWpm     .setText("0");
            statAvgAccuracy.setText("0%");
            statHint.setVisible(true);
        }

        if (lastWpm > 0) {
            statLastWpm     .setText(String.valueOf(lastWpm));
            statLastAccuracy.setText(String.format("%.1f%%", lastAccuracy));
            statLastChars   .setText(String.valueOf(lastChars));
        } else {
            statLastWpm     .setText("—");
            statLastAccuracy.setText("—");
            statLastChars   .setText("—");
        }
    }

    private int wpm() {
        endTime  = System.currentTimeMillis();
        duration = (endTime - startTime) / 1000.0;
        double wpmVal = (correctCharCount / 5.0) / (duration / 60.0);
        wpmMeter = (int) wpmVal;
        return wpmMeter;
    }

    private double accuracy() {
        if (charCount == 0) return 0.0;
        accuracyMeter = ((double) correctCharCount / charCount) * 100;
        accuracyMeter = Double.parseDouble(String.format("%.2f", accuracyMeter));
        return accuracyMeter;
    }

    private void showResultDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Typing Test Result");
        alert.setHeaderText("Test Completed!");

        String resultText =
                "WPM: "             + wpmMeter           + "\n" +
                "Accuracy: "        + accuracy()          + "%\n" +
                "Keystrokes: "      + correctCharCount    + "\n" +
                "Correct Words: "   + correctWordCount    + "\n" +
                "Incorrect Words: " + incorrectWordCount  + "\n" +
                "Time Elapsed: "    + (int) duration      + "s";

        alert.setContentText(resultText);
        alert.getDialogPane().setStyle("-fx-font-size: 16px;");
        alert.showAndWait();
    }

    public String getParagraph() {
        String text;
        try {
            text = Files.readString(Path.of("src/resources/contents.txt"), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String[] paragraphs = text.split("\\n\\s*\\n");
        return paragraphs[new Random().nextInt(paragraphs.length)];
    }

    public void startTimer() {
        timeline = new Timeline(
                new KeyFrame(Duration.seconds(2),
                        event -> wpmLabel.setText("WPM: " + wpm()))
        );
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    public void stopTimer() {
        wpmLabel.setText("WPM: 0");
        if (timeline != null) timeline.stop();
    }

    private void setColor()              { setColor("FORESTGREEN"); }
    private void setColor(String color)  {
        Text wordText = (Text) typingBlock.getChildren().get(wordCount);
        wordText.setFill(Color.web(color));
    }

    private void setUnderline() {
        Text wordText = (Text) typingBlock.getChildren().get(wordCount);
        wordText.setUnderline(true);
        if (wordCount > 0) {
            ((Text) typingBlock.getChildren().get(wordCount - 1)).setUnderline(false);
        }
    }

    private void startTest() {
        Main.AppState.testOn = true;
        usernamePane.setVisible(false); usernamePane.setDisable(true);
        typingPane  .setVisible(true);  typingPane  .setDisable(false);
        currentWord.setDisable(false);
        currentWord.setEditable(true);

        contents = getParagraph().split(" ");
        for (String word : contents) {
            Text t = new Text(word + " ");
            t.setFont(Font.font("Monospace", 18));
            t.setFill(Color.WHITE);
            typingBlock.getChildren().add(t);
            totalWordCount++;
        }
        setUnderline();
    }

    private void updateDb() {
        try {
            List<String> lines = new ArrayList<>();
            BufferedReader br   = new BufferedReader(new InputStreamReader(new FileInputStream("src/resources/db.txt"), StandardCharsets.UTF_8));
            String line;

            while ((line = br.readLine()) != null) {
                String[] p = line.split(" ");
                if (p[0].equals(Main.AppState.currentUser)) {
                    int    storedCount    = Integer.parseInt(p[1]);
                    int    storedWpm      = Integer.parseInt(p[2]);
                    double storedAccuracy = Double.parseDouble(p[3]);

                    int    newCount    = storedCount + 1;
                    int    newWpm      = (storedCount * storedWpm + wpmMeter) / newCount;
                    double newAccuracy = (storedCount * storedAccuracy + accuracyMeter) / newCount;

                    testCount   = newCount;
                    avgWpm      = newWpm;
                    avgAccuracy = newAccuracy;

                    line = Main.AppState.currentUser + " " + newCount + " " + newWpm
                            + " " + String.format("%.2f", newAccuracy);
                }
                lines.add(line);
            }
            br.close();

            OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream("src/resources/db.txt"), StandardCharsets.UTF_8);
            for (String l : lines) writer.write(l + "\n");
            writer.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateLabel() {
        wpm     .setText("WPM: "      + avgWpm);
        accuracy.setText("Accuracy: " + String.format("%.2f", avgAccuracy) + "%");
    }

    private void finishTest() {
        testing = false;
        Main.AppState.testOn = false;

        typingPane   .setVisible(false); typingPane   .setDisable(true);
        usernamePane .setVisible(true);  usernamePane .setDisable(false);
        stopTimer();
        currentWord.setDisable(true);

        if (charCount > 0) {
            lastWpm      = wpm();
            lastAccuracy = accuracy();
            lastChars    = correctCharCount;

            wpm     .setText("WPM: "      + wpmMeter);
            accuracy.setText("Accuracy: " + accuracy() + "%");
            showResultDialog();
            updateDb();
            updateLabel();
        }

        resetCounters();
    }

    private void resetCounters() {
        wpmMeter           = 0;
        accuracyMeter      = 0;
        wordCount          = 0;
        totalWordCount     = 0;
        correctCharCount   = 0;
        incorrectWordCount = 0;
        charCount          = 0;
        correctWordCount   = 0;
        typingBlock.getChildren().clear();
        currentWord.clear();
    }

    private void logout() {

        if (Main.AppState.testOn) {
            testing              = false;
            Main.AppState.testOn = false;
            if (timeline != null) timeline.stop();
            typingPane   .setVisible(false); typingPane   .setDisable(true);
            currentWord.setDisable(true);
            resetCounters();
        }

        Main.AppState.isLoggedIn  = false;
        Main.AppState.currentUser = "";
        Main.AppState.testOn      = false;
        avgWpm      = 0;
        avgAccuracy = 0.0;
        testCount   = 0;
        lastWpm     = 0;
        lastAccuracy = 0.0;
        lastChars    = 0;

        for (Pane p : new Pane[]{ usernamePane, leaderboardPane, myStatsPane, aboutPane, typingPane }) {
            p.setVisible(false);
            p.setDisable(true);
        }
        startupPane.setVisible(true);
        startupPane.setDisable(false);
        wpmLabel.setText("WPM: 0");
    }

    @FXML
    public void handleUser(ActionEvent actionEvent) {
        Button btn = (Button) actionEvent.getSource();
        if (btn == logInButton)  Main.switchScene("login.fxml",  actionEvent);
        if (btn == signUpButton) Main.switchScene("Sign.fxml",   actionEvent);
    }

    @FXML
    private void actionButton(ActionEvent event) {
        Button btn = (Button) event.getSource();

        if (btn == test)                          startTest();
        if (btn == stopButton && Main.AppState.testOn) finishTest();
        if (btn == race)                          Main.switchScene("Race.fxml", event);
        if (btn == logoutButton)                  logout();
    }

    private void togglePane(Pane pane) {
        pane.setVisible(!pane.isVisible());
        pane.setDisable(!pane.isDisable());
    }

    public static class LeaderboardEntry {
        private int    rank;
        private final String name;
        private final int    tests;
        private final int    avgWpm;
        private final double avgAccuracy;

        public LeaderboardEntry(String name, int tests, int avgWpm, double avgAccuracy) {
            this.name        = name;
            this.tests       = tests;
            this.avgWpm      = avgWpm;
            this.avgAccuracy = avgAccuracy;
        }

        public int    getRank()           { return rank; }
        public void   setRank(int r)      { this.rank = r; }
        public String getName()           { return name; }
        public int    getTests()          { return tests; }
        public int    getAvgWpm()         { return avgWpm; }
        public double getAvgAccuracy()    { return avgAccuracy; }
        public String getAvgAccuracyStr() { return String.format("%.2f%%", avgAccuracy); }
    }
}
