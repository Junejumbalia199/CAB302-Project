package com.formcoach.posedetection;

import com.formcoach.selection.ExerciseSelectionPage;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

/**
 * Placeholder for my live pose-detection screen.
 *
 * The actual pose pipeline lives in main.py (MediaPipe) and I haven't
 * bridged it back into the JavaFX UI yet. For now this view just shows
 * which exercise was picked and lets the user bail back to the list. When
 * I wire up the Python side (or port to a Java MediaPipe binding) I'll
 * drop the real camera feed into the placeholder rectangle below.
 */
public class posedetection extends StackPane {

    public posedetection() {
        this("Exercise");
    }

    public posedetection(String exerciseName) {
        getStylesheets().add(getClass().getResource("/styles/styles.css").toExternalForm());

        VBox root = new VBox(18);
        root.setPadding(new Insets(36));
        root.setAlignment(Pos.TOP_LEFT);

        // Title + back button in a single row.
        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Pose Detection — " + exerciseName);
        title.setStyle("-fx-font-size: 26px; -fx-font-weight: 800;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button back = new Button("← Back to exercises");
        back.getStyleClass().add("btn-secondary");
        back.setOnAction(e -> swap(new ExerciseSelectionPage()));

        header.getChildren().addAll(title, spacer, back);

        // Stand-in for the camera feed. Dark rounded rectangle with a
        // caption in the middle — looks intentional and reserves the
        // space the real preview will live in once I wire MediaPipe up.
        StackPane feed = new StackPane();
        Rectangle canvas = new Rectangle(800, 450, Color.web("#111827"));
        canvas.setArcWidth(16);
        canvas.setArcHeight(16);

        Label overlay = new Label("Camera preview placeholder");
        overlay.setTextFill(Color.web("#9ca3af"));
        overlay.setStyle("-fx-font-size: 16px;");

        feed.getChildren().addAll(canvas, overlay);

        Label hint = new Label(
                "Hook up the MediaPipe feed (main.py) here to show live pose overlays.");
        hint.getStyleClass().add("subtext");

        root.getChildren().addAll(header, feed, hint);
        getChildren().add(root);
    }

    private void swap(javafx.scene.Parent next) {
        Scene scene = getScene();
        if (scene != null) scene.setRoot(next);
    }
}
