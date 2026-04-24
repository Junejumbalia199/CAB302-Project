package com.formcoach.runexercise;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
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

        Button back = new Button("← Back to exercises");
        back.setStyle("-fx-background-color: white; -fx-text-fill: #1f2937;"
                + "-fx-border-color: #e5e7eb; -fx-border-radius: 8;"
                + "-fx-padding: 10 20; -fx-background-radius: 8; -fx-cursor: hand;");
        back.setOnAction(e -> { if (onBack != null) onBack.run(); });

        HBox header = new HBox(16, title, spacer, back);
        header.setAlignment(Pos.CENTER_LEFT);

        // Fake camera feed — dark rounded rectangle with a caption.
        Rectangle canvas = new Rectangle(900, 500, Color.web("#111827"));
        canvas.setArcWidth(16);
        canvas.setArcHeight(16);

        Label overlay = new Label("Camera preview placeholder");
        overlay.setTextFill(Color.web("#9ca3af"));
        overlay.setStyle("-fx-font-size: 16px;");

        StackPane feed = new StackPane(canvas, overlay);
        feed.setAlignment(Pos.CENTER);

        Label hint = new Label(
                "Hook the MediaPipe feed (main.py) into here to show live pose overlays.");
        hint.setStyle("-fx-font-size: 13px; -fx-text-fill: #6b7280;");

        root.getChildren().addAll(header, feed, hint);

        Scene scene = new Scene(root, 1280, 760);
        applyCss(scene);
        stage.setScene(scene);
        stage.setTitle("FormCoach - " + exerciseName);
        stage.show();
    }

    private void applyCss(Scene scene) {
        URL css = getClass().getResource("/styles/profile.css");
        if (css == null) {
            throw new IllegalStateException("Could not load /styles/profile.css");
        }
        scene.getStylesheets().add(css.toExternalForm());
    }
}
