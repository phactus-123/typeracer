package com.template;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Pane;
import javafx.scene.text.TextFlow;
import javafx.scene.text.Text;
import javafx.scene.text.Font;
import javafx.scene.image.ImageView;
import javafx.application.Platform;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import javafx.scene.paint.Color;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import java.awt.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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
        endTime = System.currentTimeMillis();
        duration = (endTime - startTime) / 1000.0;
        double wpm = (correctCharCount/5.0)/(duration/60.0);

        wpmMeter = (int) wpm;
        return wpmMeter;
    }

    private double accuracy() {
        accuracyMeter = ((double) correctCharCount / charCount) *100;
        accuracyMeter = Double.parseDouble(String.format("%.2f", accuracyMeter));

        return accuracyMeter;
    }

    private void showResultDialog() {

        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Typing Test Result");
        alert.setHeaderText("Test Completed!");

        String resultText =
                "WPM: " + wpmMeter + "\n" +
                        "Accuracy: " + accuracy() + "%\n" +

                        "Keystrokes: " + correctCharCount + "\n" +
                        "Correct Words: " + correctWordCount + "\n"+
                        "Incorrect Words: " + incorrectWordCount +"\n"+
                        "Time Elapsed: " + (int) duration +"s";

        accuracyMeter = 0;
        alert.setContentText(resultText);
        alert.getDialogPane().setStyle("-fx-font-size: 16px;");
        alert.showAndWait();
    }

    public String getParagraph() {

    String text = null;
    try {
        text = Files.readString(Path.of("src/resources/contents.txt"));
    } catch (IOException e) {
        throw new RuntimeException(e);
    }

    String[] paragraphs = text.split("\\n\\s*\\n");

    Random rand = new Random();
    int index = rand.nextInt(paragraphs.length);

    String randomParagraph = paragraphs[index];

    return randomParagraph;
    }

    public void startTimer() {

        timeline = new Timeline(
                new KeyFrame(Duration.seconds(2), event -> {
                    wpmLabel.setText("WPM: " + wpm());
                })
        );

        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    public void stopTimer() {
        wpmLabel.setText("WPM: " + 0);
        timeline.stop();
    }

    private void setColor() {
        setColor("FORESTGREEN");
    }

    private void setColor(String color) {
        Text wordText = (Text) typingBlock.getChildren().get(wordCount);
        wordText.setFill(Color.web(color));

    }

    private void setUnderline() {
            Text wordText = (Text) typingBlock.getChildren().get(wordCount);
            wordText.setUnderline(true);

        if(wordCount>0) {
            wordText = (Text) typingBlock.getChildren().get(wordCount-1);
            wordText.setUnderline(false);
        }
    }

    private void startTest() {
        Main.AppState.testOn = true;
        togglePane(usernamePane);
        System.out.println("test mode is on!");
        togglePane(typingPane);
        currentWord.setDisable(false);
        currentWord.setEditable(true);

        contents = getParagraph().split(" ");
        for (String word : contents) {
            Text t = new Text(word + " ");
            t.setFont(Font.font("Cascadia Code", 21));
            typingBlock.getChildren().add(t);
            totalWordCount++;
        }
        setUnderline();
    }

    private void updateDb() {
        testCount++;
        avgWpm = (testCount* avgWpm + wpmMeter) / (testCount + 1);
        avgAccuracy = (testCount* avgAccuracy + accuracyMeter) / (testCount + 1);
        try {
            List<String> lines = new ArrayList<>();
            BufferedReader br = new BufferedReader(new FileReader("src/resources/db.txt"));
            String line;

            while ((line = br.readLine()) != null) {

                String[] p = line.split(" ");

                if (p[0].equals(Main.AppState.currentUser)) {


                    avgWpm = (testCount * avgWpm + wpmMeter) / (testCount + 1);
                    avgAccuracy = (testCount * avgAccuracy + accuracyMeter) / (testCount + 1);
                    testCount++;

                    line = Main.AppState.currentUser + " " + testCount + " " + avgWpm + " " + String.format("%.2f", avgAccuracy);;
                }

                lines.add(line);
            }

            br.close();

            FileWriter writer = new FileWriter("src/resources/db.txt");

            for (String l : lines) {
                writer.write(l + "\n");
            }

            writer.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateLabel() {
        wpm.setText("WPM: " + avgWpm);
        accuracy.setText("Accuracy: " + String.format("%.2f", avgAccuracy) +"%");

    }

    private void finishTest() {
        testing = false;
        togglePane(typingPane);
        togglePane(usernamePane);
        stopTimer();
        wpm.setText("WPM: " + wpmMeter);
        accuracy.setText("Accuracy: " + accuracy() +"%");
        showResultDialog();
        updateDb();
        updateLabel();
        currentWord.setDisable(true);
        wpmMeter = 0;
        accuracyMeter = 0;
        wordCount = 0;
        totalWordCount = 0;
        typingBlock.getChildren().clear();
        currentWord.clear();
        correctCharCount = 0;
        incorrectWordCount = 0;
        charCount = 0;
    }


    @FXML
    public void initialize() {
        username.setText("USER: "+ Main.AppState.currentUser);
        togglePane(usernamePane);
        togglePane(typingPane);

        if (Main.AppState.isLoggedIn) {
            togglePane(startupPane);
        }
        if(Main.AppState.isLoggedIn && !Main.AppState.testOn) {
            updateLabel();
            togglePane(usernamePane);
        }

        currentWord.textProperty().addListener((obs, oldText, newText) -> {
            if (newText.endsWith(" ")) {

                if(!testing) {
                    startTime = System.currentTimeMillis();
                    testing = true;
                    startTimer();
                }

                String typedWord = newText.trim();
                System.out.println(typedWord);
                System.out.println(contents[wordCount]);
                charCount += typedWord.length() +1;

                if(typedWord.equals(contents[wordCount])) {
                    System.out.println("Match Found!");
                    setColor();

                    correctCharCount += typedWord.length() +1;
                    correctWordCount++;
                }
                else {
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


    @FXML
    public void handleUser(ActionEvent actionEvent) {
        Button current_Button = (Button)actionEvent.getSource();
        Scene current_scene = current_Button.getScene();
        if(current_Button == logInButton) {
            Main.switchScene("login.fxml", actionEvent);
        }

        if(current_Button == signUpButton)  {
            Main.switchScene("Sign.fxml", actionEvent);

        }

        }

    @FXML
    private void actionButton(ActionEvent event) {
        Button current_Button = (Button)event.getSource();

        if(current_Button == test) {
            startTest();
        }

        if(current_Button == stopButton && testing) {
            finishTest();
            System.out.println("WPM " + wpm());
            System.out.println("Accuracy " + accuracy());
        }
    }

    private void togglePane(Pane pane) {
        pane.setVisible(!pane.isVisible());
        pane.setDisable(!pane.isDisable());
    }

    }

