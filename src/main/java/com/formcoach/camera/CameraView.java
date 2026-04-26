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

/**
 * My  webcam preview widget. Hand it a width/height, drop it in a
 * scene, call {@link #start()} and frames just show up. {@link #stop()}
 * on the way out so the camera light goes off — I really don't want to
 * be the app that leaves people's webcams on.
 *
 * If there's no camera plugged in, or some other app is hogging it, I
 * fall back to the same dark rectangle the placeholder used and slap a
 *  status label on it. That way the layout doesn't jump around
 * and the user can tell something's off rather than just staring at a
 * frozen black box.
 */
public class CameraView extends StackPane {

    private final ImageView view = new ImageView();
    private final Label status = new Label();

    private Webcam webcam;
    private Thread grabber;
    private volatile boolean running;

    // I keep one of these and reuse it forever. If I let toFXImage allocate
    // a fresh WritableImage every frame I'm spinning up garbage 30 times
    // a second, which the GC was not happy about. Pass this in as the
    // second arg and toFXImage just writes pixels into it.
    private WritableImage frameBuffer;

    public CameraView(double width, double height) {
        // Same dark rounded look the old placeholder rectangle had. I
        // wanted swapping in a real camera feed to feel like a smooth
        // upgrade rather than a different page.
        setStyle("-fx-background-color: #111827; -fx-background-radius: 16;");
        setMinSize(width, height);
        setPrefSize(width, height);

        view.setPreserveRatio(true);
        view.setFitWidth(width);
        view.setFitHeight(height);

        status.setTextFill(Color.web("#9ca3af"));
        status.setStyle("-fx-font-size: 16px;");

        // ImageView and status label sit on top of each other. The status
        // text is empty by default, so when the camera works it stays
        // invisible behind the live frames.
        getChildren().addAll(view, status);
    }

    /** Grab the default webcam, open it, and kick off the grab loop. */
    public void start() {
        webcam = Webcam.getDefault();
        if (webcam == null) {
            // No physical camera, or the OS doesn't see one. Pretty common
            // on lab machines without a webcam — don't crash, just say so.
            showStatus("No camera detected");
            return;
        }
        try {
            // 640x480 is enough for a preview and keeps the per-frame work
            // cheap. EOS Webcam Utility caps below this anyway.
            webcam.setViewSize(new Dimension(640, 480));
            webcam.open();
        } catch (Exception ex) {
            // Usually means another app is holding the camera (Zoom, Discord,
            // the Camera app, the Python pose script). Tell the user instead
            // of leaving them with a black square.
            showStatus("Couldn't open camera: " + ex.getMessage());
            webcam = null;
            return;
        }

        // Got a frame source — clear the status so it doesn't peek through
        // the live feed.
        status.setText("");

        // Daemon so it doesn't keep the JVM alive if the user closes the
        // window without going through my onBack handler.
        running = true;
        grabber = new Thread(this::grabLoop, "camera-grabber");
        grabber.setDaemon(true);
        grabber.start();
    }

    /**
     * The grabber loop. Just sits on a background thread, blocking on the
     * next frame, converting it, and asking the FX thread to draw it. No
     * artificial frame cap — it runs as fast as the camera will give me
     * frames, which on EOS Webcam Utility tends to be ~30 FPS.
     */
    private void grabLoop() {
        while (running) {
            BufferedImage frame;
            try {
                // This is the call that blocks. Glad it's not on the FX thread.
                frame = webcam.getImage();
            } catch (Exception ex) {
                // If stop() closed the webcam while I was mid-grab, bail
                // quietly — the user already navigated away. Anything else
                // I want to see in the log so I notice it.
                if (running) System.err.println("[camera] grab failed: " + ex.getMessage());
                return;
            }
            if (frame == null) continue;   // occasional miss, just try again

            // Hand the BufferedImage to JavaFX. Passing my reusable buffer
            // as the second arg means it writes into that instance instead
            // of allocating a new one every frame.
            frameBuffer = SwingFXUtils.toFXImage(frame, frameBuffer);
            final WritableImage out = frameBuffer;
            // The only thing the FX thread has to do is swap the image
            // reference — basically free.
            Platform.runLater(() -> view.setImage(out));
        }
    }

    /**
     * Tear it all down. I call this from a couple of different spots
     * (Back button, window close), so it has to be safe to run more
     * than once.
     */
    public void stop() {
        running = false;          // signals the grab loop to drop out
        if (grabber != null) {
            grabber.interrupt();   // wake it up if it's parked in getImage()
            grabber = null;
        }
        if (webcam != null && webcam.isOpen()) {
            // If the close itself blows up there's not much I can do —
            // swallow it so the rest of the shutdown still runs.
            try { webcam.close(); } catch (Exception ignored) { }
        }
        webcam = null;
    }

    private void showStatus(String text) {
        status.setText(text);
    }
}
