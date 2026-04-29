package com.formcoach.videomodal;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.io.File;

/**
 * StackPane-based exercise demo modal for in-scene navigation.
 * Plays a tutorial video and routes the user to the live pose-detection
 * session or back to the exercise selection menu.
 */
public class ExerciseVideoView extends StackPane {

    private final String exerciseName;
    private final Runnable onBack;
    private final Runnable onStartLive;

    // Component references needed for UI updates and resource cleanup.
    private MediaPlayer player;
    private Timeline    progressTimeline;
    private Label       playPauseLabel;
    private Slider      progressSlider;
    private Label       timeLabel;

    /**
     * @param exerciseName Drives header title and video file resolution.
     * @param onBack       Callback for closing the modal (returns to selection).
     * @param onStartLive  Callback for proceeding to the live session.
     */
    public ExerciseVideoView(String exerciseName, Runnable onBack, Runnable onStartLive) {
        this.exerciseName = exerciseName == null ? "Exercise" : exerciseName;
        this.onBack       = onBack;
        this.onStartLive  = onStartLive;

        getStylesheets().add(getClass().getResource("/styles/styles.css").toExternalForm());
        setStyle("-fx-background-color: linear-gradient(to bottom right, #f5f7fa, #e9eef5);");

        VBox modal = buildModal();
        getChildren().add(modal);

        // Apply entry animation.
        FadeTransition fade = new FadeTransition(Duration.millis(260), modal);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();

        // Ensure MediaPlayer threads are terminated when the window is closed.
        sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null && newScene.getWindow() != null) {
                newScene.getWindow().setOnCloseRequest(e -> shutdownPlayer());
            }
        });
    }

    // ── layout ────────────────────────────────────────────────────────────────

    private VBox buildModal() {
        VBox modal = new VBox(0);
        modal.setMaxWidth(760);
        modal.setPrefWidth(760);
        modal.setStyle("-fx-background-color: white; -fx-background-radius: 20;");
        modal.setEffect(new DropShadow(40, 0, 8, Color.color(0, 0, 0, 0.22)));
        modal.setOpacity(0);

        modal.getChildren().addAll(
                buildHeader(),
                thinDivider(),
                buildVideoSection(),
                buildContent()
        );
        return modal;
    }

    private VBox buildHeader() {
        HBox tags = new HBox(8,
                pill("Intermediate", "#FEF9C3", "#A16207"),
                pill("Upper Body",   "#F1F5F9", "#475569")
        );

        Label title = new Label(exerciseName);
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #111827;");

        BorderPane titleRow = new BorderPane();
        titleRow.setLeft(title);
        titleRow.setRight(closeButton());
        BorderPane.setAlignment(title, Pos.CENTER_LEFT);

        VBox header = new VBox(12, tags, titleRow);
        header.setPadding(new Insets(24, 28, 18, 28));
        return header;
    }

    private Label closeButton() {
        String normal = "-fx-font-size: 16px; -fx-cursor: hand; -fx-text-fill: #9ca3af;"
                + "-fx-padding: 4 8 4 8; -fx-background-radius: 6;";
        String hover  = "-fx-font-size: 16px; -fx-cursor: hand; -fx-text-fill: #374151;"
                + "-fx-padding: 4 8 4 8; -fx-background-radius: 6; -fx-background-color: #f3f4f6;";

        Label btn = new Label("✕");
        btn.setStyle(normal);
        btn.setOnMouseEntered(e -> btn.setStyle(hover));
        btn.setOnMouseExited(e  -> btn.setStyle(normal));
        btn.setOnMouseClicked(e -> doBack());
        return btn;
    }

    private Pane thinDivider() {
        Pane p = new Pane();
        p.setPrefHeight(1);
        p.setStyle("-fx-background-color: #f3f4f6;");
        return p;
    }

    private VBox buildVideoSection() {
        Media media = new Media(locateVideo());
        player = new MediaPlayer(media);

        // Wait for READY status to prevent lockups on rapid modal toggles.
        player.statusProperty().addListener((obs, oldS, newS) -> {
            if (newS == MediaPlayer.Status.READY) {
                player.play();
            }
        });

        // Manual looping avoids bugs associated with setCycleCount(INDEFINITE) during seeks.
        player.setOnEndOfMedia(() -> {
            player.seek(Duration.ZERO);
            player.play();
        });

        // Graceful error handling and cleanup on playback failure.
        player.setOnError(() -> {
            System.err.println("MediaPlayer error: " + player.getError()
                    + " (source: " + locateVideo() + ")");
            try { player.stop();    } catch (Exception ignored) { }
            try { player.dispose(); } catch (Exception ignored) { }
        });

        MediaView view = new MediaView(player);
        view.setFitWidth(720);
        view.setPreserveRatio(true);

        VBox wrap = new VBox(view, buildControlBar());
        wrap.setAlignment(Pos.CENTER);
        wrap.setPadding(new Insets(0, 20, 20, 20));
        return wrap;
    }

    /** Resolves the video path via VideoResolver, falling back to a default if missing. */
    private String locateVideo() {
        return VideoResolver.resolve(exerciseName);
    }

    private HBox buildControlBar() {
        playPauseLabel = new Label("⏸");
        playPauseLabel.setStyle("-fx-font-size: 16px; -fx-cursor: hand;");
        playPauseLabel.setOnMouseClicked(e -> togglePlay());

        progressSlider = new Slider(0, 1, 0);
        HBox.setHgrow(progressSlider, Priority.ALWAYS);

        timeLabel = new Label("0:00 / 0:00");
        timeLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #6b7280;");

        // Seek only on release to prevent the player from freezing on continuous drag events.
        progressSlider.setOnMouseReleased(e -> {
            Duration total = player.getTotalDuration();
            if (total != null && total.toSeconds() > 0) {
                player.seek(Duration.seconds(progressSlider.getValue() * total.toSeconds()));
            }
        });

        // Sync UI icon securely using the status property instead of click events.
        player.statusProperty().addListener((obs, oldS, newS) ->
                playPauseLabel.setText(newS == MediaPlayer.Status.PLAYING ? "⏸" : "▶"));

        // Poll video progress every 200ms. Paused while the user actively drags the slider.
        progressTimeline = new Timeline(
                new KeyFrame(Duration.millis(200), ae -> {
                    if (!progressSlider.isPressed()) {
                        Duration current = player.getCurrentTime();
                        Duration total   = player.getTotalDuration();
                        if (current != null && total != null && total.toSeconds() > 0) {
                            progressSlider.setValue(current.toSeconds() / total.toSeconds());
                            timeLabel.setText(fmt(current) + " / " + fmt(total));
                        }
                    }
                })
        );
        progressTimeline.setCycleCount(Animation.INDEFINITE);
        progressTimeline.play();

        HBox bar = new HBox(12, playPauseLabel, progressSlider, timeLabel);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(8, 4, 0, 4));
        return bar;
    }

    private VBox buildContent() {
        Label description = new Label(
                "Watch the demo, then start a live session to get real-time form feedback.");
        description.getStyleClass().add("subtext");
        description.setWrapText(true);

        Button startLive = new Button("Start Live Session  →");
        startLive.getStyleClass().add("btn-primary");
        startLive.setOnAction(e -> goToLiveSession());

        Button back = new Button("Back to Exercises");
        back.getStyleClass().add("btn-secondary");
        back.setOnAction(e -> doBack());

        HBox actions = new HBox(12, startLive, back);

        VBox content = new VBox(14, description, actions);
        content.setPadding(new Insets(4, 28, 28, 28));
        return content;
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private void togglePlay() {
        if (player.getStatus() == MediaPlayer.Status.PLAYING) player.pause();
        else player.play();
    }

    private void goToLiveSession() {
        shutdownPlayer();
        if (onStartLive != null) onStartLive.run();
    }

    private void doBack() {
        shutdownPlayer();
        if (onBack != null) onBack.run();
    }

    /** Safely terminates background timelines and media player threads. Idempotent. */
    private void shutdownPlayer() {
        if (progressTimeline != null) progressTimeline.stop();
        if (player != null) {
            try { player.stop();    } catch (Exception ignored) { }
            try { player.dispose(); } catch (Exception ignored) { }
        }
    }

    private static Label pill(String text, String bg, String fg) {
        Label l = new Label(text);
        l.setStyle(
                "-fx-background-color: " + bg + ";" +
                        "-fx-text-fill: "        + fg + ";" +
                        "-fx-background-radius: 999; -fx-padding: 4 10 4 10;" +
                        "-fx-font-size: 12px; -fx-font-weight: 600;"
        );
        return l;
    }

    private static String fmt(Duration d) {
        int total = (int) d.toSeconds();
        return (total / 60) + ":" + String.format("%02d", total % 60);
    }
}