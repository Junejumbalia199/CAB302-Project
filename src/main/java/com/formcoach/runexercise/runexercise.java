package com.formcoach.runexercise;

import com.formcoach.camera.CameraView;
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

/**
 * Live exercise session placeholder. The real pose pipeline lives in
 * main.py (MediaPipe) and isn't embedded in the JavaFX app yet — this
 * screen just shows which exercise the user picked and lets them go
 * back. When the camera / MediaPipe bridge lands, it slots into the
 * dark rectangle below.
 */
public class runexercise {

    private final Stage stage;
    private final String exerciseName;
    private final Runnable onBack;
    private TextArea outputArea;

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
        cam.start();

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
        appendOutput("This is where the text output generator will send things to");

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
        if (css == null) {
            throw new IllegalStateException("Could not load /styles/styles.css");
        }
        if (css2 == null) {
            throw new IllegalStateException("Could not load /styles/selection.css");
        }
        scene.getStylesheets().add(css.toExternalForm());
        scene.getStylesheets().add(css2.toExternalForm());
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

        HBox widget = new HBox(12);
        widget.getStyleClass().add("chat-widget");
        widget.setAlignment(Pos.CENTER_LEFT);
        widget.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        StackPane iconCircle = new StackPane();
        iconCircle.getStyleClass().add("chat-icon-circle");

        SVGPath chatIcon = new SVGPath();
        chatIcon.setContent("M6 6 H18 Q20 6 20 8 V14 Q20 16 18 16 H11 L8 19 V16 H6 Q4 16 4 14 V8 Q4 6 6 6 Z");
        chatIcon.setStyle("-fx-fill: white;");
        iconCircle.getChildren().add(chatIcon);

        widget.getChildren().addAll(iconCircle);

        widget.setOnMouseClicked(e -> System.out.println("AI chatbot clicked"));

        outer.getChildren().add(widget);
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
