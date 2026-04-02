package com.template;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.text.*;
import javafx.scene.paint.Color;
import javafx.animation.*;
import javafx.util.Duration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;


public class RaceController {

    // ── Lobby pane ──────────────────────────────────────────────────────────
    @FXML private VBox   lobbyPane;
    @FXML private Button hostButton;
    @FXML private Button joinButton;
    @FXML private Button backButton;
    @FXML private Label  statusLabel;

    // ── Race pane ────────────────────────────────────────────────────────────
    @FXML private VBox      racePane;
    @FXML private TextFlow  typingBlock;
    @FXML private TextArea  inputField;
    @FXML private Label     myProgressLabel;
    @FXML private Label     opponentProgressLabel;
    @FXML private ProgressBar myProgressBar;
    @FXML private ProgressBar opponentProgressBar;
    @FXML private Label     myWpmLabel;
    @FXML private Label     opponentWpmLabel;
    @FXML private Label     countdownLabel;
    @FXML private Label     resultLabel;

    // ── State ────────────────────────────────────────────────────────────────
    private String[]  words;
    private int       wordIndex      = 0;
    private int       totalWords     = 0;
    private int       correctChars   = 0;
    private int       totalChars     = 0;
    private double    startTime;
    private boolean   raceStarted    = false;
    private boolean   raceFinished   = false;
    private boolean   resultShown    = false;

    private Timeline  wpmTimeline;
    private String    opponentName   = "Opponent";

    private static final int PORT = 5000;

    // ────────────────────────────────────────────────────────────────────────
    //  Initialise
    // ────────────────────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        racePane.setVisible(false);
        racePane.setDisable(true);
        resultLabel.setVisible(false);
        countdownLabel.setVisible(false);
        inputField.setDisable(true);

        inputField.textProperty().addListener((obs, oldText, newText) -> {
            if (!raceStarted || raceFinished) return;
            if (newText.endsWith(" ")) {
                handleWordSubmit(newText.trim());
                Platform.runLater(() -> inputField.clear());
            }
        });
    }

    // ────────────────────────────────────────────────────────────────────────
    //  Lobby buttons
    // ────────────────────────────────────────────────────────────────────────
    @FXML
    public void handleHost(ActionEvent event) {
        statusLabel.setText("Waiting for opponent to join...");
        hostButton.setDisable(true);
        joinButton.setDisable(true);

        Main.AppState.isHost = true;

        Thread serverThread = new Thread(() -> {
            try {
                Main.AppState.server = new GameServer();
                Main.AppState.server.start(PORT);

                String paragraph = getParagraph();
                Main.AppState.server.broadcast("PARAGRAPH:" + paragraph);

            } catch (IOException e) {
                Platform.runLater(() -> statusLabel.setText("Error starting server: " + e.getMessage()));
            }
        });
        serverThread.setDaemon(true);
        serverThread.start();

        new Thread(() -> {
            try {
                Thread.sleep(400);
                connectAsClient("localhost");
            } catch (InterruptedException | IOException e) {
                Platform.runLater(() -> statusLabel.setText("Error connecting: " + e.getMessage()));
            }
        }).start();
    }

    @FXML
    public void handleJoin(ActionEvent event) {
        statusLabel.setText("Connecting to host...");
        hostButton.setDisable(true);
        joinButton.setDisable(true);

        new Thread(() -> {
            try {
                connectAsClient("localhost");
            } catch (IOException e) {
                Platform.runLater(() -> statusLabel.setText("Could not connect. Is the host running?"));
                Platform.runLater(() -> {
                    hostButton.setDisable(false);
                    joinButton.setDisable(false);
                });
            }
        }).start();
    }

    // ── FIX #2: Back button was navigating to "Main.fxml" (capital M) but the
    //    actual resource file is "main.fxml". Fixed to lowercase to match.
    @FXML
    public void handleBack(ActionEvent event) {
        cleanup();
        Main.switchScene("main.fxml", event);
    }

    // ────────────────────────────────────────────────────────────────────────
    //  Networking helpers
    // ────────────────────────────────────────────────────────────────────────
    private void connectAsClient(String ip) throws IOException {
        GameClient client = new GameClient();
        Main.AppState.client = client;

        client.setMessageListener(this::handleServerMessage);
        client.connect(ip, PORT);

        client.sendMessage("JOIN:" + Main.AppState.currentUser);

        Platform.runLater(() -> statusLabel.setText("Connected! Waiting for race to start..."));
    }

    private void handleServerMessage(String msg) {
        System.out.println("[CLIENT RECV] " + msg);

        if (msg.startsWith("PARAGRAPH:")) {
            String paragraph = msg.substring("PARAGRAPH:".length());
            Platform.runLater(() -> setupRace(paragraph));

        } else if (msg.startsWith("JOIN:")) {
            String who = msg.substring("JOIN:".length());
            if (!who.equals(Main.AppState.currentUser)) {
                opponentName = who;
                Platform.runLater(() -> {
                    opponentProgressLabel.setText(opponentName + ": 0%");
                    statusLabel.setText(opponentName + " joined! Starting soon...");
                });
            }

        } else if (msg.startsWith("PROGRESS:")) {
            // PROGRESS:<user>:<wordsTyped>:<totalWords>:<wpm>
            String[] parts = msg.split(":");
            if (parts.length >= 5 && !parts[1].equals(Main.AppState.currentUser)) {
                int typed     = Integer.parseInt(parts[2]);
                int total     = Integer.parseInt(parts[3]);
                int theirWpm  = Integer.parseInt(parts[4]);
                double pct    = total > 0 ? (double) typed / total : 0;
                Platform.runLater(() -> {
                    opponentProgressBar.setProgress(pct);
                    opponentProgressLabel.setText(opponentName + ": " + (int)(pct * 100) + "%");
                    // ── FIX #1: opponent WPM label is now updated every second
                    //    because we broadcast PROGRESS from the timer, not just on word submit.
                    opponentWpmLabel.setText("WPM: " + theirWpm);
                });
            }

        } else if (msg.startsWith("FINISH:")) {
            String[] parts = msg.split(":");
            String who = parts[1];
            int    wpm = Integer.parseInt(parts[2]);

            if (!who.equals(Main.AppState.currentUser) && !resultShown) {
                resultShown = true;
                Platform.runLater(() -> showResult(false, who, wpm));
            }
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    //  Race setup
    // ────────────────────────────────────────────────────────────────────────
    private void setupRace(String paragraph) {
        words      = paragraph.split(" ");
        totalWords = words.length;
        wordIndex  = 0;

        typingBlock.getChildren().clear();
        for (String w : words) {
            Text t = new Text(w + " ");
            t.setFont(Font.font("Monospace", 18));
            t.setFill(Color.WHITE);
            typingBlock.getChildren().add(t);
        }
        highlightCurrent();

        lobbyPane.setVisible(false);
        lobbyPane.setDisable(true);
        racePane.setVisible(true);
        racePane.setDisable(false);

        myProgressLabel.setText(Main.AppState.currentUser + ": 0%");
        opponentProgressLabel.setText(opponentName + ": 0%");

        startCountdown();
    }

    private void startCountdown() {
        countdownLabel.setVisible(true);
        inputField.setDisable(true);

        int[] count = {3};
        Timeline cd = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            if (count[0] > 0) {
                countdownLabel.setText(String.valueOf(count[0]));
                count[0]--;
            } else {
                countdownLabel.setText("GO!");
            }
        }));
        cd.setCycleCount(4);
        cd.setOnFinished(e -> {
            countdownLabel.setVisible(false);
            raceStarted = true;
            startTime   = System.currentTimeMillis();
            inputField.setDisable(false);
            inputField.requestFocus();
            startWpmTimer();
        });
        cd.play();
    }

    // ────────────────────────────────────────────────────────────────────────
    //  Typing logic
    // ────────────────────────────────────────────────────────────────────────
    private void handleWordSubmit(String typed) {
        if (wordIndex >= totalWords) return;

        totalChars += typed.length() + 1;
        boolean correct = typed.equals(words[wordIndex]);

        Text current = (Text) typingBlock.getChildren().get(wordIndex);
        current.setFill(correct ? Color.FORESTGREEN : Color.RED);
        current.setUnderline(false);

        if (correct) correctChars += typed.length() + 1;

        if (correct) wordIndex++;

        double progress = (double) wordIndex / totalWords;
        myProgressBar.setProgress(progress);
        myProgressLabel.setText(Main.AppState.currentUser + ": " + (int)(progress * 100) + "%");

        // Broadcast on each word submission as well (belt + suspenders)
        broadcastProgress();

        if (wordIndex >= totalWords) {
            finishRace();
            return;
        }

        highlightCurrent();
    }

    private void finishRace() {
        raceFinished = true;
        inputField.setDisable(true);
        stopWpmTimer();

        int finalWpm = calcWpm();
        if (Main.AppState.client != null) {
            Main.AppState.client.sendMessage("FINISH:" + Main.AppState.currentUser + ":" + finalWpm);
        }
        if (!resultShown) {
            showResult(true, Main.AppState.currentUser, finalWpm);
            resultShown = true;
        }
    }

    private void showResult(boolean iWon, String finisher, int wpm) {
        resultLabel.setVisible(true);
        if (iWon) {
            resultLabel.setText("🏆 You finished first! WPM: " + wpm);
            resultLabel.setStyle("-fx-text-fill: green; -fx-font-size: 20px; -fx-font-weight: bold;");
        } else {
            resultLabel.setText(finisher + " finished first with " + wpm + " WPM. Keep practicing!");
            resultLabel.setStyle("-fx-text-fill: orangered; -fx-font-size: 18px;");
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    //  Helpers
    // ────────────────────────────────────────────────────────────────────────
    private void highlightCurrent() {
        if (wordIndex > 0) {
            Text prev = (Text) typingBlock.getChildren().get(wordIndex - 1);
            prev.setUnderline(false);
        }
        if (wordIndex < typingBlock.getChildren().size()) {
            Text cur = (Text) typingBlock.getChildren().get(wordIndex);
            cur.setUnderline(true);
        }
    }

    private int calcWpm() {
        double elapsed = (System.currentTimeMillis() - startTime) / 1000.0;
        if (elapsed <= 0) return 0;
        return (int) ((correctChars / 5.0) / (elapsed / 60.0));
    }

    // ── FIX #1: Every second we (a) update our own WPM label AND (b) broadcast
    //    our current progress so the opponent sees a continuously updated WPM,
    //    not just one that refreshes only on word submission.
    private void startWpmTimer() {
        wpmTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            int currentWpm = calcWpm();
            myWpmLabel.setText("WPM: " + currentWpm);
            broadcastProgress();
        }));
        wpmTimeline.setCycleCount(Timeline.INDEFINITE);
        wpmTimeline.play();
    }

    /** Sends our current progress + live WPM to the server so both players stay in sync. */
    private void broadcastProgress() {
        if (Main.AppState.client != null && raceStarted && !raceFinished) {
            Main.AppState.client.sendMessage(
                "PROGRESS:" + Main.AppState.currentUser
                + ":" + wordIndex
                + ":" + totalWords
                + ":" + calcWpm()
            );
        }
    }

    private void stopWpmTimer() {
        if (wpmTimeline != null) wpmTimeline.stop();
    }

    private String getParagraph() {
        try {
            String text = Files.readString(Path.of("src/resources/contents.txt"));
            String[] paragraphs = text.split("\\n\\s*\\n");
            return paragraphs[new Random().nextInt(paragraphs.length)].replaceAll("\\s+", " ").trim();
        } catch (IOException e) {
            return "The quick brown fox jumps over the lazy dog and runs into the forest";
        }
    }

    private void cleanup() {
        stopWpmTimer();
        if (Main.AppState.client != null) {
            Main.AppState.client.stop();
            Main.AppState.client = null;
        }
        if (Main.AppState.server != null) {
            Main.AppState.server.stop();
            Main.AppState.server = null;
        }
        Main.AppState.isHost = false;
    }
}
