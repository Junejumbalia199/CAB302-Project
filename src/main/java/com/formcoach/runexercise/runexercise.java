package com.formcoach.runexercise;

import com.formcoach.camera.CameraView;
import com.formcoach.chatbot.chatbot;
import com.formcoach.poseanalysis.PoseScorer;
import com.formcoach.textoutputgen.PoseValidationException;
import com.formcoach.textoutputgen.textoutputgen;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.TextAlignment;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.net.URL;
import java.util.Set;

/**
 * Live exercise session placeholder. The real pose pipeline lives in
 * main.py (MediaPipe) and isn't embedded in the JavaFX app yet — this
 * screen just shows which exercise the user picked and lets them go
 * back. When the camera / MediaPipe bridge lands, it slots into the
 * dark rectangle below.
 */
public class runexercise {

    // how often the feedback text updates, in seconds - change this to adjust the rate
    private static final double FEEDBACK_INTERVAL_SECONDS = 5.0;
    // Track whether the camera is running
    final boolean[] camRunning = {false}; // start as true because cam.start() is called below

    private final Stage stage;
    private final String exerciseName;
    private final Runnable onBack;
    private TextArea outputArea;

    // tracks when feedback was last written so we can throttle it
    private long lastFeedbackTime = 0;

    public runexercise(Stage stage, String exerciseName, Runnable onBack) {
        this.stage        = stage;
        this.exerciseName = exerciseName == null ? "Exercise" : exerciseName;
        this.onBack       = onBack;
    }

    public void show() {
        VBox root = new VBox(18);
        root.setPadding(new Insets(36));
        root.setStyle("-fx-background-color: linear-gradient(to bottom right, #f5f7fa, #e9eef5);");

        // Header: title + back.
        Label title = new Label("Live Session — " + exerciseName);
        title.setStyle("-fx-font-size: 26px; -fx-font-weight: 800;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Swapped out the dark placeholder rectangle for an actual webcam
        // feed. CameraView handles the messy bits (opening the camera,
        // running the grab loop, falling back gracefully if there's no
        // camera) — I just need to remember to call stop() on it before
        // the user leaves this page, otherwise the camera light stays on.
        CameraView cam = new CameraView(900, 500);

        // set up form scoring for the three supported exercises
        // this has to happen before cam.start() so the pose detector gets launched
        Set<String> scoredExercises = Set.of("Push-ups", "Sit-ups", "Squats");
        if (scoredExercises.contains(exerciseName)) {
            PoseScorer scorer = new PoseScorer(exerciseName);
            textoutputgen gen = new textoutputgen();
            cam.setOnLandmarks(landmarks -> {
                if (!camRunning[0]) return; // skip processing if paused

                // existing scoring code
                PoseScorer.ScorerResult match = scorer.score(landmarks);
                if (!match.isValid()) return;

                long now = System.currentTimeMillis();
                if (now - lastFeedbackTime < (long)(FEEDBACK_INTERVAL_SECONDS * 1000)) return;
                lastFeedbackTime = now;

                Double[] userX = new Double[33];
                Double[] userY = new Double[33];
                Double[] userZ = new Double[33];
                for (int i = 0; i < 33; i++) {
                    float[] lm = landmarks.get(i);
                    userX[i] = (double) lm[0];
                    userY[i] = (double) lm[1];
                    userZ[i] = (double) lm[2];
                }

                try {
                    textoutputgen.PoseResult result = gen.output(
                            userX, userY, userZ,
                            match.idealX(), match.idealY(), match.idealZ(),
                            match.exerciseType()
                    );

                    StringBuilder sb = new StringBuilder();
                    sb.append(result.summaryText());
                    if (!result.movementFeedback().isEmpty()) {
                        sb.append("\n");
                        for (String feedback : result.movementFeedback()) {
                            sb.append("\n  ").append(feedback);
                        }
                    }
                    sb.append("\n\nScore: ").append(result.score()).append(" / 10");
                    outputArea.setText(sb.toString());

                } catch (PoseValidationException e) {
                    System.err.println("[form] validation error: " + e.getMessage());
                }
            });
        }

        // cam.start();

        Button back = new Button("← Back to exercises");
        /*back.setStyle("-fx-background-color: white; -fx-text-fill: #1f2937;"
                + "-fx-border-color: #e5e7eb; -fx-border-radius: 8;"
                + "-fx-padding: 10 20; -fx-background-radius: 8; -fx-cursor: hand;");
                */
        back.getStyleClass().add("btn-primary");
        back.setOnAction(e -> {
            cam.stop();
            if (onBack != null) onBack.run();
        });

        HBox header = new HBox(16, title, spacer, back);
        header.setAlignment(Pos.CENTER_LEFT);

        StackPane feed = new StackPane(cam);
        feed.setAlignment(Pos.CENTER);

        Rectangle overlay = new Rectangle(900, 500, Color.rgb(0, 0, 0, 0.4));
        overlay.setVisible(true); // show overlay initially
        feed.getChildren().add(overlay);

        feed.setOnMouseClicked(e -> {
            if (camRunning[0]) {
                cam.stop();
                camRunning[0] = false;
                overlay.setVisible(true);
            } else {
                cam.start();
                camRunning[0] = true;
                overlay.setVisible(false);
            }
        });

        // replace with hint on how to run program.
        // "Click on the area above to start or stop video. Text feedback will appear below."
        Label hint = new Label(
                "Hook the MediaPipe feed (main.py) into here to show live pose overlays.");
        hint.setStyle("-fx-font-size: 13px; -fx-text-fill: #6b7280;");
        VBox.setMargin(hint, new Insets(0, 100, 0, 100));

        outputArea = new TextArea();
        outputArea.setEditable(false); // Prevent user editing
        outputArea.setWrapText(true);  // Wrap long lines
        VBox.setMargin(outputArea, new Insets(0, 100, 0, 100));

        root.getChildren().addAll(header, feed, hint, outputArea);

        Button helpButton = createHelpButton();
        StackPane chatbotWidget = createChatbotWidget();

        StackPane mainLayout = new StackPane();
        mainLayout.getChildren().addAll(root, helpButton, chatbotWidget);

        StackPane.setAlignment(helpButton, Pos.BOTTOM_LEFT);
        StackPane.setMargin(helpButton, new Insets(0, 0, 24, 24));

        StackPane.setAlignment(chatbotWidget, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(chatbotWidget, new Insets(0, 24, 24, 0));

        Scene scene = new Scene(mainLayout, 1280, 760);
        applyCss(scene);
        stage.setScene(scene);
        stage.setTitle("FormCoach - " + exerciseName);
        // If the user just X-es out of the window without going Back, the
        // Back handler never fires and the camera would stay open. Caught
        // me out the first time — second launch couldn't open the camera
        // because the previous run still had it. Belt and braces.
        stage.setOnCloseRequest(e -> cam.stop());
        stage.show();
    }

    private void applyCss(Scene scene) {
        URL css = getClass().getResource("/styles/styles.css");
        URL css2 = getClass().getResource("/styles/selection.css");
        URL css3 = getClass().getResource("/styles/landingpage.css");
        if (css == null) {
            throw new IllegalStateException("Could not load /styles/styles.css");
        }
        if (css2 == null) {
            throw new IllegalStateException("Could not load /styles/selection.css");
        }
        scene.getStylesheets().add(css.toExternalForm());
        scene.getStylesheets().add(css2.toExternalForm());
        if (css3 != null) {
            scene.getStylesheets().add(css3.toExternalForm());
        }
    }

    // append output to text area
    public void appendOutput(String text) {
        Platform.runLater(() -> {
            outputArea.appendText(text + "\n");
        });
    }

    // I've just copied over these methods from exercise selection page for now, suggest we move
    // common UI elements such as these to a single class that can be called from all other pages to add this stuff
    private Button createHelpButton() {
        Button helpButton = new Button("?");
        helpButton.getStyleClass().add("floating-help");
        helpButton.setOnAction(e -> openHelpDialog());
        return helpButton;
    }

    private StackPane createChatbotWidget() {
        StackPane outer = new StackPane();
        outer.setPickOnBounds(false);
        outer.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        Button fab = new Button("💬");
        fab.getStyleClass().add("chat-fab");
        fab.setTextFill(javafx.scene.paint.Color.WHITE);
        fab.setOnAction(e -> chatbot.showChatbot(stage));

        outer.getChildren().add(fab);
        return outer;
    }

    private void openHelpDialog() {
        Stage helpStage = new Stage();
        helpStage.initOwner(stage);
        helpStage.initModality(Modality.WINDOW_MODAL);
        helpStage.setTitle("FormCoach Help");

        VBox card = new VBox(16);
        card.getStyleClass().add("dialog-card");
        card.setMaxWidth(640);

        Label title = new Label("How to use FormCoach");
        title.getStyleClass().add("dialog-title");

        Label body = new Label(
                "1. Browse exercises with the page arrows or search bar.\n\n" +
                        "2. Press Select to open the tutorial page.\n\n" +
                        "3. Follow the exercise tutorial then open up the form tracker.\n\n" +
                        "4. Click the AI chat assistant if you need further help or information."
        );
        body.getStyleClass().add("exercise-description");
        body.setWrapText(true);
        body.setTextAlignment(TextAlignment.LEFT);

        Button close = new Button("Close");
        close.getStyleClass().add("btn-primary");
        close.setOnAction(e -> helpStage.close());

        VBox wrapper = new VBox(card);
        wrapper.getStyleClass().add("root-pane");
        wrapper.setAlignment(Pos.CENTER);
        wrapper.setPadding(new Insets(30));

        card.getChildren().addAll(title, body, close);

        Scene scene = new Scene(wrapper, 720, 420);
        applyCss(scene);
        helpStage.setScene(scene);
        helpStage.setResizable(false);
        helpStage.showAndWait();
    }
}