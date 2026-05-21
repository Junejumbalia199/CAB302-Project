package com.formcoach.camera;

import com.formcoach.PoseDetector.PoseDetector;
import com.github.sarxos.webcam.Webcam;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Cross-platform webcam preview. Iterates available cameras (USB / built-in /
 * virtual UVC) and opens the first that succeeds. Negotiates a supported view
 * size per device. Shows OS-specific hint when nothing works.
 *
 * When PoseDetector.DEBUG_MODE is true, a skeleton wireframe is drawn on top
 * of the feed using landmark data from the Python pose server.
 */
public class CameraView extends StackPane {

    private static final Dimension PREFERRED_SIZE = new Dimension(640, 480);

    // skeleton connections - each pair is a start/end landmark index
    private static final int[][] POSE_CONNECTIONS = {
            {0,1},{1,2},{2,3},{3,7},
            {0,4},{4,5},{5,6},{6,8},
            {9,10},
            {11,12},{11,13},{13,15},{15,17},{15,19},{15,21},{17,19},
            {12,14},{14,16},{16,18},{16,20},{16,22},{18,20},
            {11,23},{12,24},{23,24},
            {23,25},{25,27},{27,29},{27,31},{29,31},
            {24,26},{26,28},{28,30},{28,32},{30,32}
    };

    // green for left side, red for right side (from user's perspective)
    private static final Set<Integer> LEFT_LANDMARKS  = Set.of(1,2,3,7,9,11,13,15,17,19,21,23,25,27,29,31);
    private static final Set<Integer> RIGHT_LANDMARKS = Set.of(4,5,6,8,10,12,14,16,18,20,22,24,26,28,30,32);

    // landmarks to highlight in blue - matches the Python prototype defaults
    private static final Set<Integer> HIGHLIGHT_POINTS = Set.of(3, 9);

    private final ImageView view = new ImageView();
    private final Label status = new Label();
    private final Canvas overlay;

    private Webcam webcam;
    private Thread grabber;
    private volatile boolean running;

    // Reused frame target; sized on first frame, written every frame after.
    private WritableImage frameBuffer;
    private PoseDetector poseDetector;

    // actual camera frame dimensions, set on first grab and used for landmark mapping
    private volatile int frameWidth;
    private volatile int frameHeight;

    // optional callback fired on the JavaFX thread each time landmarks arrive
    private Consumer<List<float[]>> landmarkCallback;

    public CameraView(double width, double height) {
        setStyle("-fx-background-color: #111827; -fx-background-radius: 16;");
        setMinSize(width, height);
        setPrefSize(width, height);

        view.setPreserveRatio(true);
        view.setFitWidth(width);
        view.setFitHeight(height);

        status.setTextFill(Color.web("#9ca3af"));
        status.setStyle("-fx-font-size: 16px;");

        // transparent canvas sits on top of the video feed for the skeleton overlay
        overlay = new Canvas(width, height);
        overlay.setMouseTransparent(true);

        getChildren().addAll(view, overlay, status);
    }

    /**
     * Registers a callback that fires on the JavaFX thread each time a new set
     * of landmarks arrives from the pose server. Call this before start() so the
     * pose detector gets launched. Passing null clears the callback.
     */
    public void setOnLandmarks(Consumer<List<float[]>> callback) {
        this.landmarkCallback = callback;
    }

    /** Start camera discovery and the grab loop on a background thread. */
    public void start() {
        showStatus("Connecting to camera…");
        running = true;

        // launch the pose detector if the skeleton overlay or scoring is needed
        if (PoseDetector.DEBUG_MODE || landmarkCallback != null) {
            poseDetector = new PoseDetector(landmarks -> {
                if (PoseDetector.DEBUG_MODE) drawSkeleton(landmarks);
                if (landmarkCallback != null) landmarkCallback.accept(landmarks);
            });
            poseDetector.start();
        }

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

        if (poseDetector != null) {
            poseDetector.stop();
            poseDetector = null;
        }

        // clear any leftover skeleton drawing
        Platform.runLater(() ->
                overlay.getGraphicsContext2D().clearRect(0, 0, overlay.getWidth(), overlay.getHeight())
        );
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

            // store the real frame size once so the skeleton transform stays accurate
            if (frameWidth == 0) {
                frameWidth  = frame.getWidth();
                frameHeight = frame.getHeight();
            }

            // hand the raw frame to the pose detector before converting it
            if (poseDetector != null) {
                poseDetector.submitFrame(frame);
            }

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

    // ── skeleton drawing ─────────────────────────────────────────────────────

    // called on the JavaFX thread via PoseDetector's LandmarkListener
    private void drawSkeleton(List<float[]> landmarks) {
        GraphicsContext gc = overlay.getGraphicsContext2D();
        gc.clearRect(0, 0, overlay.getWidth(), overlay.getHeight());

        if (landmarks == null || landmarks.isEmpty()) return;
        if (frameWidth <= 0 || frameHeight <= 0) return;

        double canvasW = overlay.getWidth();
        double canvasH = overlay.getHeight();

        // the ImageView preserves aspect ratio, so the actual image may not fill the
        // canvas - work out the real image bounds to keep the skeleton aligned
        double scale = Math.min(canvasW / frameWidth, canvasH / frameHeight);
        double imgW  = frameWidth  * scale;
        double imgH  = frameHeight * scale;
        double offX  = (canvasW - imgW) / 2.0;
        double offY  = (canvasH - imgH) / 2.0;

        // draw connecting lines first so dots sit on top
        gc.setStroke(Color.web("#c8c8c8"));
        gc.setLineWidth(2);
        for (int[] conn : POSE_CONNECTIONS) {
            int a = conn[0], b = conn[1];
            if (a < landmarks.size() && b < landmarks.size()) {
                float[] pa = landmarks.get(a);
                float[] pb = landmarks.get(b);
                gc.strokeLine(offX + pa[0] * imgW, offY + pa[1] * imgH,
                        offX + pb[0] * imgW, offY + pb[1] * imgH);
            }
        }

        // draw each landmark dot
        for (int i = 0; i < landmarks.size(); i++) {
            float[] lm = landmarks.get(i);
            double x = offX + lm[0] * imgW;
            double y = offY + lm[1] * imgH;

            if (HIGHLIGHT_POINTS.contains(i)) {
                gc.setFill(Color.BLUE);
            } else if (LEFT_LANDMARKS.contains(i)) {
                gc.setFill(Color.LIME);
            } else if (RIGHT_LANDMARKS.contains(i)) {
                gc.setFill(Color.RED);
            } else {
                gc.setFill(Color.WHITE);
            }
            gc.fillOval(x - 5, y - 5, 10, 10);

            // thin black outline so dots are visible on any background
            gc.setStroke(Color.BLACK);
            gc.setLineWidth(1);
            gc.strokeOval(x - 5, y - 5, 10, 10);
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