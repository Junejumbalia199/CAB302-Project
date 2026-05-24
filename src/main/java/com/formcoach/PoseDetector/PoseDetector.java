package com.formcoach.PoseDetector;

import javafx.application.Platform;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Bridges the JavaFX camera feed to the Python MediaPipe pose-detection server.
 * Frames submitted via {@link #submitFrame} are sent over a length-prefixed TCP socket
 * to {@code scripts/main.py} running on port 5001; landmark JSON is returned and
 * delivered to the registered {@link LandmarkListener} on the JavaFX thread.
 */
public class PoseDetector {

    /**
     * When {@code true}, the skeleton wireframe overlay is drawn on the camera feed
     * and raw landmark data is printed to the console. Set to {@code false} for
     * production builds.
     */
    public static boolean DEBUG_MODE = true;

    private static final String HOST = "127.0.0.1";
    private static final int PORT = 5001;

    /**
     * Callback interface notified on the JavaFX thread each time the Python server
     * returns a new set of pose landmarks.
     */
    public interface LandmarkListener {
        /**
         * Called with the latest pose landmark data.
         * Landmark indices match MediaPipe's 0-32 numbering; each {@code float[]} is
         * {@code {x, y, z, visibility}} in normalised 0.0–1.0 coordinates.
         * @param landmarks list of 33 landmark arrays
         */
        void onLandmarks(List<float[]> landmarks);
    }

    private final LandmarkListener listener;
    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;
    private Thread workerThread;
    private volatile boolean running;

    // only the latest frame is kept - older ones are dropped if the server is slow
    private final AtomicReference<BufferedImage> pendingFrame = new AtomicReference<>();

    /**
     * Constructs a new PoseDetector that will notify the given listener each time landmarks arrive.
     * @param listener the callback to invoke with fresh landmark data on the JavaFX thread
     */
    public PoseDetector(LandmarkListener listener) {
        this.listener = listener;
    }

    /**
     * Starts the background worker thread that connects to the Python server and processes frames.
     */
    public void start() {
        running = true;
        workerThread = new Thread(this::workerLoop, "pose-detector");
        workerThread.setDaemon(true);
        workerThread.start();
    }

    /**
     * Submits a camera frame for pose detection. Safe to call from any thread.
     * Only the most recent frame is retained; older frames are dropped if the server is busy.
     * @param frame the JPEG-encodable image frame to analyse
     */
    public void submitFrame(BufferedImage frame) {
        pendingFrame.set(frame);
    }

    /**
     * Stops the worker thread and closes the socket connection to the Python server.
     */
    public void stop() {
        running = false;
        if (workerThread != null) {
            workerThread.interrupt();
            workerThread = null;
        }
        closeSocket();
    }

    private void workerLoop() {
        // keep retrying until the python server is up since its not really an optional feature
        while (running && socket == null) {
            try {
                socket = new Socket(HOST, PORT);
                out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
                in  = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
                System.out.println("[pose] connected to Python server on port " + PORT);
            } catch (IOException e) {
                System.err.println("[pose] Python server not found, retrying in 2s...");
                socket = null;
                try { Thread.sleep(2000); } catch (InterruptedException ie) { return; }
            }
        }

        while (running) {
            BufferedImage frame = pendingFrame.getAndSet(null);
            if (frame == null) {
                // no new frame yet, just wait a bit
                try { Thread.sleep(16); } catch (InterruptedException e) { break; }
                continue;
            }

            try {
                // compress to jepg - much smaller than raw pixels over the socket
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(frame, "jpg", baos);
                byte[] jpegBytes = baos.toByteArray();

                // send length prefix then the data (matching pythons struct.pack)
                out.writeInt(jpegBytes.length);
                out.write(jpegBytes);
                out.flush();

                // read the response the same way
                int responseLen = in.readInt();
                byte[] responseBytes = new byte[responseLen];
                in.readFully(responseBytes);
                String json = new String(responseBytes, StandardCharsets.UTF_8);

                List<float[]> landmarks = parseLandmarks(json);

                // deliver to the listener on the JavaFX thread
                if (listener != null) {
                    Platform.runLater(() -> listener.onLandmarks(landmarks));
                }

            } catch (IOException e) {
                if (running) System.err.println("[pose] lost connection: " + e.getMessage());
                break;
            }
        }
    }

    // pulls x/y/z/visibility out of the JSON that i'm getting from main.py
    // using a regex since i control the exact format from the Python side
    private static List<float[]> parseLandmarks(String json) {
        List<float[]> result = new ArrayList<>();
        if (json == null || json.trim().equals("null")) return result;

        Pattern p = Pattern.compile(
                "\"x\":([-\\d.Ee]+),\"y\":([-\\d.Ee]+),\"z\":([-\\d.Ee]+),\"visibility\":([-\\d.Ee]+)"
        );
        Matcher m = p.matcher(json);
        while (m.find()) {
            result.add(new float[]{
                    Float.parseFloat(m.group(1)),
                    Float.parseFloat(m.group(2)),
                    Float.parseFloat(m.group(3)),
                    Float.parseFloat(m.group(4))
            });
        }
        return result;
    }

    private void closeSocket() {
        try { if (socket != null) socket.close(); } catch (Exception ignored) {}
        socket = null;
    }
}