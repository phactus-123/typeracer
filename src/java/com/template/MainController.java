package com.template;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.text.TextFlow;
import javafx.scene.text.Text;
import javafx.scene.text.Font;
import javafx.scene.image.ImageView;
import javafx.application.Platform;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;


public class MainController
{
    @FXML
    public Label label;

    @FXML
    private Button logInButton;

    @FXML
    private Button signUpButton;

    @FXML
    private ImageView WelcomeText;

    @FXML
    private Pane startupPane;

    @FXML
    private Pane usernamePane;

    @FXML
    private Pane typingPane;

    @FXML
    private Label username;

    @FXML
    private Label wpmLabel;

    @FXML
    private Label accuracy;

    @FXML
    private Button test;

    @FXML
    private Button race;

    @FXML
    private Button stopButton;

    @FXML
    private TextFlow typingBlock;

    @FXML
    private TextArea currentWord;

    @FXML
    private Label wpm;

    // ── FIX #3: Leaderboard button wired from FXML (fx:id="leaderboardButton")
    @FXML
    private Button leaderboardButton;


    private static String contents[];

    private int charCount = 0;
    private int correctCharCount = 0;

    public static int avgWpm;
    public static double avgAccuracy;
    public static int testCount;

    private int wordCount = 0;
    private int correctWordCount = 0;
    private int incorrectWordCount = 0;
    private int totalWordCount = 0;

    private double startTime;
    private double endTime;
    private double duration;

    private int wpmMeter = 0;
    private double accuracyMeter;

    private Timeline timeline;
    private Boolean testing = false;

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
                "WPM: "            + wpmMeter          + "\n" +
                "Accuracy: "       + accuracy()        + "%\n" +
                "Keystrokes: "     + correctCharCount  + "\n" +
                "Correct Words: "  + correctWordCount  + "\n" +
                "Incorrect Words: "+ incorrectWordCount + "\n" +
                "Time Elapsed: "   + (int) duration    + "s";

        alert.setContentText(resultText);
        alert.getDialogPane().setStyle("-fx-font-size: 16px;");
        alert.showAndWait();
    }

    public String getParagraph() {
        String text;
        try {
            text = Files.readString(Path.of("src/resources/contents.txt"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String[] paragraphs = text.split("\\n\\s*\\n");
        return paragraphs[new Random().nextInt(paragraphs.length)];
    }

    public void startTimer() {
        timeline = new Timeline(
                new KeyFrame(Duration.seconds(2), event -> wpmLabel.setText("WPM: " + wpm()))
        );
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    public void stopTimer() {
        wpmLabel.setText("WPM: " + 0);
        if (timeline != null) timeline.stop();
    }

    private void setColor() { setColor("FORESTGREEN"); }

    private void setColor(String color) {
        Text wordText = (Text) typingBlock.getChildren().get(wordCount);
        wordText.setFill(Color.web(color));
    }

    private void setUnderline() {
        Text wordText = (Text) typingBlock.getChildren().get(wordCount);
        wordText.setUnderline(true);
        if (wordCount > 0) {
            wordText = (Text) typingBlock.getChildren().get(wordCount - 1);
            wordText.setUnderline(false);
        }
    }

    private void startTest() {
        Main.AppState.testOn = true;
        togglePane(usernamePane);
        togglePane(typingPane);
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
            BufferedReader br = new BufferedReader(new FileReader("src/resources/db.txt"));
            String line;

            while ((line = br.readLine()) != null) {
                String[] p = line.split(" ");
                if (p[0].equals(Main.AppState.currentUser)) {
                    int    storedCount    = Integer.parseInt(p[1]);
                    int    storedWpm      = Integer.parseInt(p[2]);
                    double storedAccuracy = Double.parseDouble(p[3]);

                    // Rolling average
                    int    newCount    = storedCount + 1;
                    int    newWpm      = (storedCount * storedWpm + wpmMeter) / newCount;
                    double newAccuracy = (storedCount * storedAccuracy + accuracyMeter) / newCount;

                    // Update static fields so labels reflect correctly
                    testCount   = newCount;
                    avgWpm      = newWpm;
                    avgAccuracy = newAccuracy;

                    line = Main.AppState.currentUser + " " + newCount + " " + newWpm
                            + " " + String.format("%.2f", newAccuracy);
                }
                lines.add(line);
            }
            br.close();

            FileWriter writer = new FileWriter("src/resources/db.txt");
            for (String l : lines) writer.write(l + "\n");
            writer.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateLabel() {
        wpm.setText("WPM: " + avgWpm);
        accuracy.setText("Accuracy: " + String.format("%.2f", avgAccuracy) + "%");
    }

    private void finishTest() {
        testing = false;
        // ── FIX #2 (part 2): testOn was never reset to false, which caused
        //    initialize() to skip showing usernamePane on the next visit.
        Main.AppState.testOn = false;

        togglePane(typingPane);
        togglePane(usernamePane);
        stopTimer();
        wpm.setText("WPM: " + wpmMeter);
        accuracy.setText("Accuracy: " + accuracy() + "%");
        showResultDialog();
        updateDb();
        updateLabel();
        currentWord.setDisable(true);
        wpmMeter        = 0;
        accuracyMeter   = 0;
        wordCount       = 0;
        totalWordCount  = 0;
        typingBlock.getChildren().clear();
        currentWord.clear();
        correctCharCount    = 0;
        incorrectWordCount  = 0;
        charCount           = 0;
        correctWordCount    = 0;
    }


    // ─────────────────────────────────────────────────────────────────────────
    //  initialize
    // ─────────────────────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
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
            if (newText.endsWith(" ")) {

                if (!testing) {
                    startTime = System.currentTimeMillis();
                    testing   = true;
                    startTimer();
                }

                String typedWord = newText.trim();
                charCount += typedWord.length() + 1;

                if (typedWord.equals(contents[wordCount])) {
                    setColor();
                    correctCharCount += typedWord.length() + 1;
                    correctWordCount++;
                } else {
                    setColor("RED");
                    incorrectWordCount++;
                }

                wordCount++;
                if (wordCount >= totalWordCount) {
                    finishTest();
                    return;
                }

                setUnderline();
                Platform.runLater(() -> currentWord.clear());
            }
        });
    }


    // ─────────────────────────────────────────────────────────────────────────
    //  Button handlers
    // ─────────────────────────────────────────────────────────────────────────
    @FXML
    public void handleUser(ActionEvent actionEvent) {
        Button btn = (Button) actionEvent.getSource();
        if (btn == logInButton)  Main.switchScene("login.fxml",  actionEvent);
        if (btn == signUpButton) Main.switchScene("Sign.fxml",   actionEvent);
    }

    @FXML
    private void actionButton(ActionEvent event) {
        Button btn = (Button) event.getSource();

        if (btn == test) {
            startTest();
        }
        if (btn == stopButton && testing) {
            finishTest();
        }
        if (btn == race) {
            Main.switchScene("Race.fxml", event);
        }
        // ── FIX #3: Leaderboard button (also hookable via fx:id="leaderboardButton"
        //    or onAction="#showLeaderboard" in FXML)
        if (btn == leaderboardButton) {
            showLeaderboard();
        }
    }

    // ── FIX #3: Public so it can also be wired directly via FXML onAction
    @FXML
    public void showLeaderboard() {
        // ── Read all players from db.txt ─────────────────────────────────────
        List<LeaderboardEntry> entries = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader("src/resources/db.txt"))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] parts = line.split("\\s+");
                if (parts.length < 4) continue;
                String name      = parts[0];
                int    tests     = Integer.parseInt(parts[1]);
                int    wpmVal    = Integer.parseInt(parts[2]);
                double acc       = Double.parseDouble(parts[3]);
                entries.add(new LeaderboardEntry(name, tests, wpmVal, acc));
            }
        } catch (IOException e) {
            Alert err = new Alert(Alert.AlertType.ERROR, "Could not load leaderboard data.");
            err.showAndWait();
            return;
        }

        // Sort by avg WPM descending, then accuracy descending
        entries.sort(Comparator.comparingInt(LeaderboardEntry::getAvgWpm).reversed()
                               .thenComparingDouble(LeaderboardEntry::getAvgAccuracy).reversed());

        // ── Build the leaderboard Stage ──────────────────────────────────────
        Stage stage = new Stage();
        stage.setTitle("🏆 Leaderboard");
        stage.initModality(Modality.APPLICATION_MODAL);

        // Title bar
        Label title = new Label("🏆  Leaderboard");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #f0c040;");

        // TableView
        TableView<LeaderboardEntry> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setStyle("-fx-font-size: 14px;");

        TableColumn<LeaderboardEntry, Integer> rankCol = new TableColumn<>("#");
        rankCol.setCellValueFactory(new PropertyValueFactory<>("rank"));
        rankCol.setMaxWidth(45);
        rankCol.setMinWidth(45);

        TableColumn<LeaderboardEntry, String> nameCol = new TableColumn<>("Player");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<LeaderboardEntry, Integer> wpmCol = new TableColumn<>("Avg WPM");
        wpmCol.setCellValueFactory(new PropertyValueFactory<>("avgWpm"));

        TableColumn<LeaderboardEntry, String> accCol = new TableColumn<>("Avg Accuracy");
        accCol.setCellValueFactory(new PropertyValueFactory<>("avgAccuracyStr"));

        TableColumn<LeaderboardEntry, Integer> testsCol = new TableColumn<>("Tests");
        testsCol.setCellValueFactory(new PropertyValueFactory<>("tests"));

        //noinspection unchecked
        table.getColumns().addAll(rankCol, nameCol, wpmCol, accCol, testsCol);

        // Assign rank numbers and populate table
        for (int i = 0; i < entries.size(); i++) {
            entries.get(i).setRank(i + 1);
        }
        table.getItems().addAll(entries);

        // Highlight current user's row
        table.setRowFactory(tv -> new TableRow<LeaderboardEntry>() {
            @Override
            protected void updateItem(LeaderboardEntry item, boolean empty) {
                super.updateItem(item, empty);
                if (!empty && item != null
                        && item.getName().equals(Main.AppState.currentUser)) {
                    setStyle("-fx-background-color: #2a4a2a;");
                } else {
                    setStyle("");
                }
            }
        });

        // Empty-state label
        if (entries.isEmpty()) {
            table.setPlaceholder(new Label("No players yet — complete a test to appear here!"));
        }

        Button closeBtn = new Button("Close");
        closeBtn.setStyle("-fx-font-size: 13px; -fx-padding: 6 20;");
        closeBtn.setOnAction(e -> stage.close());

        VBox root = new VBox(14, title, table, closeBtn);
        root.setAlignment(Pos.TOP_CENTER);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #1a1a2e;");

        Scene scene = new Scene(root, 500, 420);
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }


    // ─────────────────────────────────────────────────────────────────────────
    //  Leaderboard data model
    // ─────────────────────────────────────────────────────────────────────────
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
        public void   setRank(int rank)   { this.rank = rank; }
        public String getName()           { return name; }
        public int    getTests()          { return tests; }
        public int    getAvgWpm()         { return avgWpm; }
        public double getAvgAccuracy()    { return avgAccuracy; }
        public String getAvgAccuracyStr() { return String.format("%.2f%%", avgAccuracy); }
    }


    private void togglePane(Pane pane) {
        pane.setVisible(!pane.isVisible());
        pane.setDisable(!pane.isDisable());
    }
}
