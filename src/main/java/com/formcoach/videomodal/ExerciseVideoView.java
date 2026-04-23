package com.formcoach.videomodal;

import com.formcoach.posedetection.posedetection;
import com.formcoach.selection.ExerciseSelectionPage;
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
 * The exercise demo popup that sits inside the main scene.
 *
 * Looks pretty much the same as the standalone {@code ExerciseModal} over in
 * src/, but I made this one a StackPane so it plays nicely with the
 * scene-swapping I'm doing elsewhere in formcoach (selection → here →
 * posedetection). The standalone version extends Application, which doesn't
 * really work with setRoot().
 *
 * How it's supposed to go:
 *   • User picks an exercise → this screen shows up with the demo video going.
 *   • They watch it (or don't), then hit ✕ or "Start Live Session".
 *   • I stop the player and flip over to the live pose-detection screen.
 *
 * About loading the video: I check the classpath first (/pushup.mp4), and if
 * it's not there I just point at src/pushup.mp4 directly. Saves me from
 * having to copy a 32 MB mp4 into formcoach's resources folder.
 */
public class ExerciseVideoView extends StackPane {

    private final String exerciseName;

    // Pulled these up as fields because the control bar and the cleanup code
    // both need to get at them. Passing them around as arguments everywhere
    // just felt messier than keeping them here.
    private MediaPlayer player;
    private Timeline    progressTimeline;
    private Label       playPauseLabel;
    private Slider      progressSlider;
    private Label       timeLabel;

    public ExerciseVideoView(String exerciseName) {
        this.exerciseName = exerciseName == null ? "Exercise" : exerciseName;

        getStylesheets().add(getClass().getResource("/styles/styles.css").toExternalForm());
        setStyle("-fx-background-color: #9389B1;");

        VBox modal = buildModal();
        getChildren().add(modal);

        // Little fade-in so the modal doesn't just pop into existence. Stole
        // this from the standalone ExerciseModal because it looked nice.
        FadeTransition fade = new FadeTransition(Duration.millis(260), modal);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();

        // Shut the player down when the window closes. MediaPlayer fires up
        // some native threads behind the scenes and if I don't stop it they
        // just hang around after the window's gone. The scene isn't hooked
        // up yet when the constructor runs, so I have to wait for it to
        // show up before I can wire this in.
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
        btn.setOnMouseClicked(e -> goToLiveSession());
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
        // I used to just call setAutoPlay(true) right here, but if you open
        // and close the modal really quickly, the new player tries to start
        // up before the old one's finished shutting down, and it gets stuck
        // and never actually plays. Waiting until the status hits READY
        // before kicking things off sorts that out.
        player.statusProperty().addListener((obs, oldS, newS) -> {
            if (newS == MediaPlayer.Status.READY) {
                player.play();
            }
        });
        // Looping it by hand instead of using setCycleCount(INDEFINITE). I
        // spent ages chasing a freeze that only happened when you seeked
        // while paused — turns out INDEFINITE just breaks in that case.
        // Doing it manually is dead simple and just works.
        player.setOnEndOfMedia(() -> {
            player.seek(Duration.ZERO);
            player.play();
        });
        // If the video does blow up (usually when you're clicking through
        // the app really fast), I just print what went wrong and clean up
        // so nothing's left hanging. The rest of the UI keeps working — you
        // can still hit Back or Start Live Session.
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


     // Figure out which demo video to show for this exercise. Used to just
     // hardcode pushup.mp4, but now that the selection page actually sends
     // real exercise names through, I hand it off to VideoResolver so each
     // one can have its own clip — and it falls back to the pushup demo
     // when I haven't got a proper one yet.

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

        // Only jump the video when the user lets go of the slider, not on
        // every little drag. I tried the "seek while dragging" version and
        // it totally choked the player — the video just froze and wouldn't
        // come back. One seek at the end is all it needs.
        progressSlider.setOnMouseReleased(e -> {
            Duration total = player.getTotalDuration();
            if (total != null && total.toSeconds() > 0) {
                player.seek(Duration.seconds(progressSlider.getValue() * total.toSeconds()));
            }
        });

        // Letting the player's status be the one thing that sets the play
        // button's icon. I used to update it inside togglePlay() too, but
        // the status can change for other reasons (errors, the auto-loop
        // kicking in) and the icon would end up out of sync.
        player.statusProperty().addListener((obs, oldS, newS) ->
                playPauseLabel.setText(newS == MediaPlayer.Status.PLAYING ? "⏸" : "▶"));

        // Just checking in with the player every 200ms to update the
        // progress bar and the time. I tried listening to the current-time
        // property instead, but it ticks on its own timer regardless of
        // what the mouse is doing, so it kept fighting the user mid-drag.
        // This polling approach plus the isPressed() check is boring but
        // rock solid.
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
        back.setOnAction(e -> {
            shutdownPlayer();
            Scene s = getScene();
            if (s != null) s.setRoot(new ExerciseSelectionPage());
        });

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
        Scene s = getScene();
        if (s != null) s.setRoot(new posedetection(exerciseName));
    }

    // Called from a few different spots, so it has to be OK to run more than once.
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