package com.formcoach.posedetection;

import com.formcoach.camera.CameraView;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Standalone pose-detection tool placeholder. Same shape as the
 * runexercise screen but framed as a general-purpose pose checker
 * rather than tied to a specific exercise. Reachable via
 * Navigator#showPosedetection() if any page wants to add a link to it.
 */
public class posedetection {

    private final Stage stage;
    private final Runnable onBack;

    /**
     * Constructs a new posedetection screen.
     * @param stage  the primary application stage
     * @param onBack callback invoked when the user navigates back
     */
    public posedetection(Stage stage, Runnable onBack) {
        this.stage  = stage;
        this.onBack = onBack;
    }

    /** Builds and displays the standalone pose-detection screen on the primary stage. */
    public void show() {
        VBox root = new VBox(18);
        root.setPadding(new Insets(36));
        root.setStyle("-fx-background-color: linear-gradient(to bottom right, #f5f7fa, #e9eef5);");

        Label title = new Label("Pose Detection");
        title.setStyle("-fx-font-size: 26px; -fx-font-weight: 800;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button back = new Button("← Back");
        back.setStyle("-fx-background-color: white; -fx-text-fill: #1f2937;"
                + "-fx-border-color: #e5e7eb; -fx-border-radius: 8;"
                + "-fx-padding: 10 20; -fx-background-radius: 8; -fx-cursor: hand;");

        // live camera feed - skeleton overlay shows automatically if DEBUG_MODE is true
        CameraView feed = new CameraView(900, 500);
        feed.start();

        back.setOnAction(e -> {
            feed.stop();
            if (onBack != null) onBack.run();
        });

        HBox header = new HBox(16, title, spacer, back);
        header.setAlignment(Pos.CENTER_LEFT);

        Label hint = new Label(
                "Standalone pose checker — not tied to a specific exercise.");
        hint.setStyle("-fx-font-size: 13px; -fx-text-fill: #6b7280;");

        root.getChildren().addAll(header, feed, hint);

        Scene scene = new Scene(root, 1280, 760);
        stage.setScene(scene);
        stage.setTitle("FormCoach - Pose Detection");
        stage.show();
    }
}