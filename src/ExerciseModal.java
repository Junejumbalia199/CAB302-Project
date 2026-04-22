import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
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
import javafx.stage.Stage;
import javafx.util.Duration;
import kaggle.ExerciseDataset;
import kaggle.ExerciseRepository;

import java.util.List;

/**
 * ExerciseModal — the page you see for a single exercise.
 * It plays a little demo video, shows some tags for difficulty and muscle group,
 * has a short description, numbered step-by-step instructions,
 * and a "Select Exercise" button at the bottom.
 */
public class ExerciseModal extends Application {

    // These get used by a few different methods (the video section, the control bar,
    // and the play/pause toggle), so I just kept them as fields instead of passing
    // them around everywhere. Felt cleaner that way.
    private MediaPlayer player;
    private Timeline    progressTimeline;
    private Label       playPauseLabel;
    private Slider      progressSlider;
    private Label       timeLabel;

    // Lets me drop in a Kaggle row from outside before the window opens, so the
    // title and tags match whatever exercise got picked. Had to make it static
    // because JavaFX builds this class itself with the no-arg constructor — there's
    // no way to pass anything in, so a static setter is the workaround.
    private static ExerciseDataset datasetOverride;

    public static void setDataset(ExerciseDataset dataset) {
        datasetOverride = dataset;
    }

    // ── Entry point ───────────────────────────────────────────────────────────

    @Override
    public void start(Stage primaryStage) {
        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: #9389B1;");

        VBox modal = buildModal(primaryStage);
        root.getChildren().add(modal);

        Scene scene = new Scene(root, 900, 780);
        // Load the shared stylesheet so everything matches the rest of the app
        scene.getStylesheets().add(ExerciseModal.class.getResource("styles.css").toExternalForm());

        primaryStage.setTitle("Fitness App");
        primaryStage.setScene(scene);
        primaryStage.show();

        // Do the fade-in after show() so everything's already laid out properly —
        // if you start it earlier, stuff hasn't settled into place yet and it looks weird.
        FadeTransition fadeIn = new FadeTransition(Duration.millis(280), modal);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();

        // Tidy up when the window closes, otherwise the video player and the
        // progress timer just keep chugging away in the background.
        primaryStage.setOnCloseRequest(e -> {
            player.stop();
            progressTimeline.stop();
        });

        // Start loading the Kaggle data on a background thread. First time you run
        // it, it has to download the CSV, and I don't want the window to freeze
        // while that happens. If something goes wrong (no internet, missing
        // kaggle.json, whatever) I just ignore it — the modal has default values
        // baked in, so the user still sees a working screen.
        Thread warmup = new Thread(() -> {
            try {
                ExerciseRepository repo = new ExerciseRepository();
                List<ExerciseDataset> rows = repo.ensureLoaded();
                System.out.println("[Kaggle] loaded " + rows.size() + " exercise rows");
            } catch (Exception ex) {
                System.err.println("[Kaggle] warmup skipped: " + ex.getMessage());
            }
        }, "kaggle-warmup");
        warmup.setDaemon(true);
        warmup.start();
    }

    // ── Modal assembly ────────────────────────────────────────────────────────

    private VBox buildModal(Stage primaryStage) {
        VBox modal = new VBox(0);
        modal.setMaxWidth(760);
        modal.setPrefWidth(760);
        // White card with rounded corners and a soft shadow to make it pop off
        // the background. Shadow has to go in code rather than CSS so it doesn't
        // get cut off at the edge of the card.
        modal.setStyle("-fx-background-color: white; -fx-background-radius: 20;");
        modal.setEffect(new DropShadow(40, 0, 8, Color.color(0, 0, 0, 0.22)));
        modal.setOpacity(0); // invisible at first so the fade-in has something to work with

        modal.getChildren().addAll(
                buildHeader(primaryStage),
                buildDivider(),
                buildVideoSection(),
                buildContent()
        );
        return modal;
    }

    private VBox buildHeader(Stage primaryStage) {
        // If someone set a Kaggle row, use those values. Otherwise just fall back
        // to my old hardcoded Pushups stuff so the modal still works on its own —
        // handy when I'm just testing this screen by itself.
        String titleText = datasetOverride != null && datasetOverride.isValid()
                ? datasetOverride.title()
                : "Pushups";
        String levelText = datasetOverride != null && !datasetOverride.level().isBlank()
                ? datasetOverride.level()
                : "Intermediate";
        String bodyText  = datasetOverride != null && !datasetOverride.bodyPart().isBlank()
                ? datasetOverride.bodyPart()
                : "Upper Body";

        HBox tags = new HBox(8,
                createTag(levelText, "#FEF9C3", "#A16207"),
                createTag(bodyText,  "#F1F5F9", "#475569")
        );

        Label title = new Label(titleText);
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #111827;");

        // BorderPane has a built-in left/center/right layout, which saves me
        // from messing around with invisible spacer elements.
        BorderPane titleRow = new BorderPane();
        titleRow.setLeft(title);
        titleRow.setRight(buildCloseButton(primaryStage));
        BorderPane.setAlignment(title,                   Pos.CENTER_LEFT);
        BorderPane.setAlignment(titleRow.getRight(),     Pos.CENTER);

        VBox header = new VBox(12, tags, titleRow);
        header.setPadding(new Insets(24, 28, 18, 28));
        return header;
    }

    private Label buildCloseButton(Stage primaryStage) {
        // Two style strings — one normal, one for hover. Just swap between them
        // on mouse enter/exit. Way simpler than animating anything.
        String normal = "-fx-font-size: 16px; -fx-cursor: hand; -fx-text-fill: #9ca3af;" +
                "-fx-padding: 4 8 4 8; -fx-background-radius: 6;";
        String hover  = "-fx-font-size: 16px; -fx-cursor: hand; -fx-text-fill: #374151;" +
                "-fx-padding: 4 8 4 8; -fx-background-radius: 6; -fx-background-color: #f3f4f6;";

        Label btn = new Label("✕");
        btn.setStyle(normal);
        btn.setOnMouseEntered(e -> btn.setStyle(hover));
        btn.setOnMouseExited(e  -> btn.setStyle(normal));
        btn.setOnMouseClicked(e -> primaryStage.close());
        return btn;
    }

    private Pane buildDivider() {
        // Tiny 1-pixel line between the header and the video. Barely noticeable
        // but it ties things together visually.
        Pane divider = new Pane();
        divider.setPrefHeight(1);
        divider.setStyle("-fx-background-color: #f3f4f6;");
        return divider;
    }

    private VBox buildVideoSection() {
        // The video sits right next to the compiled class file. If it's not there,
        // throw straight away — much easier to spot than a silent null later on.
        java.net.URL videoRes = ExerciseModal.class.getResource("pushup.mp4");
        if (videoRes == null) {
            throw new RuntimeException(
                    "pushup.mp4 not found in classpath. " +
                            "Make sure it's been copied to the out/ directory alongside ExerciseModal.class."
            );
        }

        player = new MediaPlayer(new Media(videoRes.toExternalForm()));
        player.setOnError(() -> System.err.println("MediaPlayer error: " + player.getError()));

        // Looping it by hand instead of using the built-in INDEFINITE cycle count.
        // Turns out INDEFINITE mode breaks things if you try to seek while paused —
        // the player just freezes up and play() stops working. This way is rock solid.
        player.setOnEndOfMedia(() -> {
            player.seek(Duration.ZERO);
            player.play();
        });

        MediaView mediaView = new MediaView(player);
        mediaView.setFitWidth(760);
        mediaView.setFitHeight(380);
        mediaView.setPreserveRatio(true); // don't stretch the video out of shape

        StackPane videoArea = new StackPane(mediaView);
        videoArea.setPrefSize(760, 380);
        videoArea.setMaxSize(760, 380);
        videoArea.setStyle("-fx-background-color: #0f0f0f;"); // black bars around the video, proper cinema feel

        // buildControlBar() also sets up playPauseLabel, progressSlider, and timeLabel
        // on the side, so I need to call it here before anything tries to use them.
        return new VBox(0, videoArea, buildControlBar());
    }

    private VBox buildContent() {
        Label description = new Label(
                "A classic upper body exercise that builds chest, tricep, and shoulder strength. " +
                        "Keep your core tight and glutes squeezed to maintain a straight body line " +
                        "throughout the movement."
        );
        description.setWrapText(true);
        description.setStyle("-fx-font-size: 14px; -fx-text-fill: #6b7280; -fx-line-spacing: 2;");

        Label howToHeader = new Label("How to do it");
        howToHeader.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #111827;");

        String[] steps = {
                "Start in a high plank — hands shoulder-width apart, body in a straight line.",
                "Lower your chest toward the floor, keeping elbows at ~45°. No flaring.",
                "Press back up to full arm extension.",
                "Repeat for the desired number of reps."
        };

        Button selectBtn = new Button("Select Exercise");
        selectBtn.setPrefWidth(200);
        selectBtn.setPrefHeight(42);
        selectBtn.getStyleClass().addAll("button", "btn-primary");
        selectBtn.setOnAction(e -> System.out.println("Exercise selected: Pushups"));

        // Stuck it over on the right — that's where you'd expect to find an
        // action button on a popup like this.
        HBox btnRow = new HBox(selectBtn);
        btnRow.setAlignment(Pos.CENTER_RIGHT);
        btnRow.setPadding(new Insets(8, 0, 0, 0));

        VBox content = new VBox(14, description, howToHeader, buildSteps(steps), btnRow);
        content.setPadding(new Insets(20, 28, 26, 28));
        return content;
    }

    private VBox buildSteps(String[] steps) {
        VBox box = new VBox(8);
        for (int i = 0; i < steps.length; i++) {
            // The numbers are blue to help your eye follow the list down the page,
            // without being loud enough to actually distract you.
            Label num = new Label(String.valueOf(i + 1));
            num.setMinWidth(20);
            num.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #4a90e2;");

            Label txt = new Label(steps[i]);
            txt.setWrapText(true);
            txt.setStyle("-fx-font-size: 13px; -fx-text-fill: #4b5563;");
            HBox.setHgrow(txt, Priority.ALWAYS);

            HBox row = new HBox(10, num, txt);
            row.setAlignment(Pos.TOP_LEFT);
            box.getChildren().add(row);
        }
        return box;
    }

    // ── Media controls ────────────────────────────────────────────────────────

    private HBox buildControlBar() {
        HBox bar = new HBox(12);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(9, 16, 9, 16));
        bar.setStyle("-fx-background-color: #1a1a1a;");

        // The ▶ / ⏸ icon. I never change its text directly — the listener just
        // below handles that based on whether the video is playing or not.
        playPauseLabel = new Label("▶");
        playPauseLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-cursor: hand; -fx-min-width: 18;");
        playPauseLabel.setOnMouseClicked(e -> togglePlay());

        // Keeps the icon matching what the video's actually doing. Useful because
        // the video can pause itself for reasons that have nothing to do with the
        // user clicking — like when it hits the end and loops.
        player.statusProperty().addListener((obs, oldS, newS) ->
                playPauseLabel.setText(newS == MediaPlayer.Status.PLAYING ? "⏸" : "▶")
        );

        // The scrub bar. 0 means the very start of the video, 1 means the end.
        progressSlider = new Slider(0, 1, 0);
        progressSlider.setStyle("-fx-accent: #4a90e2;");
        HBox.setHgrow(progressSlider, Priority.ALWAYS);

        // Only jump the video when the user finishes dragging, not during the drag.
        // If you seek on every little mouse movement the player gets swamped and
        // can freeze up — makes it look like play() is broken when it isn't.
        progressSlider.setOnMouseReleased(e -> {
            Duration total = player.getTotalDuration();
            if (total != null && total.toSeconds() > 0) {
                player.seek(Duration.seconds(progressSlider.getValue() * total.toSeconds()));
            }
        });

        timeLabel = new Label("0:00 / 0:00");
        timeLabel.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 12px;");

        Label volIcon = new Label("🔊");
        volIcon.setStyle("-fx-font-size: 13px;");

        Slider volumeSlider = new Slider(0, 1, 1);
        volumeSlider.setPrefWidth(72);
        volumeSlider.setStyle("-fx-accent: #4a90e2;");
        // Hook the volume straight to the slider — no extra code needed.
        player.volumeProperty().bind(volumeSlider.valueProperty());

        // Using a timer that ticks every 200ms to update the progress bar.
        // I tried listening to the player's current-time property instead but the
        // slider and the player end up fighting each other in a feedback loop
        // that's a pain to untangle. A simple poll every fifth of a second looks
        // smooth enough and keeps things nice and separate.
        progressTimeline = new Timeline(
                new KeyFrame(Duration.millis(200), ae -> {
                    // Leave the slider alone while the user is actively dragging it,
                    // otherwise the bar jumps around under their finger. isPressed()
                    // is the easiest way to tell.
                    if (!progressSlider.isPressed()) {
                        Duration current = player.getCurrentTime();
                        Duration total   = player.getTotalDuration();
                        if (current != null && total != null && total.toSeconds() > 0) {
                            progressSlider.setValue(current.toSeconds() / total.toSeconds());
                            timeLabel.setText(formatTime(current) + " / " + formatTime(total));
                        }
                    }
                })
        );
        progressTimeline.setCycleCount(Animation.INDEFINITE);
        progressTimeline.play();

        bar.getChildren().addAll(playPauseLabel, progressSlider, timeLabel, volIcon, volumeSlider);
        return bar;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void togglePlay() {
        if (player.getStatus() == MediaPlayer.Status.PLAYING) {
            player.pause();
        } else {
            player.play();
        }
    }

    private String formatTime(Duration d) {
        if (d == null || d.isUnknown()) return "0:00";
        int totalSecs = (int) d.toSeconds();
        return String.format("%d:%02d", totalSecs / 60, totalSecs % 60);
    }

    private Label createTag(String text, String bgColor, String textColor) {
        Label tag = new Label(text);
        tag.setPadding(new Insets(4, 12, 4, 12));
        tag.setStyle(
                "-fx-background-color: " + bgColor + ";" +
                        "-fx-text-fill: "        + textColor + ";" +
                        "-fx-background-radius: 12;" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-size: 11px;"
        );
        return tag;
    }

    public static void main(String[] args) {
        launch(args);
    }
}