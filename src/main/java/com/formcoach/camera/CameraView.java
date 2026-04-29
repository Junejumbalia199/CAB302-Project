package com.formcoach.camera;

import com.github.sarxos.webcam.Webcam;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Locale;

/**
 * Cross-platform webcam preview. Iterates available cameras (USB / built-in /
 * virtual UVC) and opens the first that succeeds. Negotiates a supported view
 * size per device. Shows OS-specific hint when nothing works.
 */
public class CameraView extends StackPane {

    private static final Dimension PREFERRED_SIZE = new Dimension(640, 480);

    private final ImageView view = new ImageView();
    private final Label status = new Label();

    private Webcam webcam;
    private Thread grabber;
    private volatile boolean running;

    // Reused frame target; sized on first frame, written every frame after.
    private WritableImage frameBuffer;

    public CameraView(double width, double height) {
        setStyle("-fx-background-color: #111827; -fx-background-radius: 16;");
        setMinSize(width, height);
        setPrefSize(width, height);

        view.setPreserveRatio(true);
        view.setFitWidth(width);
        view.setFitHeight(height);

        status.setTextFill(Color.web("#9ca3af"));
        status.setStyle("-fx-font-size: 16px;");

        getChildren().addAll(view, status);
    }

    /** Start camera discovery and the grab loop on a background thread. */
    public void start() {
        showStatus("Connecting to camera…");
        running = true;
        grabber = new Thread(this::grabLoop, "camera-grabber");
        grabber.setDaemon(true);
        grabber.start();
    }

    /** Stop the grab loop and release the camera. Safe to call twice. */
    public void stop() {
        running = false;
        if (grabber != null) {
            grabber.interrupt();
            grabber = null;
        }
        if (webcam != null && webcam.isOpen()) {
            try { webcam.close(); } catch (Exception ignored) { }
        }
        webcam = null;
    }

    // ── background thread ────────────────────────────────────────────────────

    /** Discovers a working camera then streams frames until {@link #stop()}. */
    private void grabLoop() {
        webcam = openFirstWorkingCam();
        if (webcam == null) {
            Platform.runLater(() -> showStatus("No camera available. " + osHint()));
            return;
        }

        boolean firstFrame = true;
        while (running) {
            BufferedImage frame;
            try {
                frame = webcam.getImage();
            } catch (Exception ex) {
                if (running) System.err.println("[camera] grab failed: " + ex.getMessage());
                return;
            }
            if (frame == null) continue;

            frameBuffer = SwingFXUtils.toFXImage(frame, frameBuffer);
            final WritableImage out = frameBuffer;
            if (firstFrame) {
                firstFrame = false;
                Platform.runLater(() -> { view.setImage(out); status.setText(""); });
            } else {
                Platform.runLater(() -> view.setImage(out));
            }
        }
    }

    // ── camera selection ─────────────────────────────────────────────────────

    /** Tries each detected camera; returns first one that opens. */
    private static Webcam openFirstWorkingCam() {
        List<Webcam> cams = Webcam.getWebcams();
        if (cams == null || cams.isEmpty()) return null;

        for (Webcam cam : cams) {
            try {
                cam.setViewSize(pickViewSize(cam));
                cam.open();
                System.out.println("[camera] using: " + cam.getName());
                return cam;
            } catch (Exception ex) {
                System.err.println("[camera] " + cam.getName() + " failed: " + ex.getMessage());
                // try next device
            }
        }
        return null;
    }

    /** Returns a view size the device actually supports. */
    private static Dimension pickViewSize(Webcam cam) {
        Dimension[] sizes = cam.getViewSizes();
        if (sizes != null) {
            for (Dimension d : sizes) {
                if (d.equals(PREFERRED_SIZE)) return d;
            }
            if (sizes.length > 0) return sizes[0];
        }
        return PREFERRED_SIZE;
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    /** OS-specific guidance shown when no camera could be opened. */
    static String osHint() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("win")) {
            return "Check Settings → Privacy → Camera → allow apps to access camera.";
        }
        if (os.contains("mac")) {
            return "Grant camera access in System Settings → Privacy & Security → Camera.";
        }
        if (os.contains("nux") || os.contains("nix")) {
            return "Verify /dev/video0 exists and your user is in the 'video' group.";
        }
        return "Check OS camera permissions for this app.";
    }

    private void showStatus(String text) {
        status.setText(text);
    }
}
