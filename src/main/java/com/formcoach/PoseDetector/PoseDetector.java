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

public class PoseDetector {

    // flip this to true to enable the skeleton overlay and console landmark output
    public static boolean DEBUG_MODE = true;

    private static final String HOST = "127.0.0.1";
    private static final int PORT = 5001;

    // called on the JavaFX thread each time new landmarks arrive
    public interface LandmarkListener {
        // landmarks index matches MediaPipe's 0-32 landmark numbering
        // each float[] is {x, y, z, visibility} in normalised 0.0-1.0 coords
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

    public PoseDetector(LandmarkListener listener) {
        this.listener = listener;
    }

    public void start() {
        running = true;
        workerThread = new Thread(this::workerLoop, "pose-detector");
        workerThread.setDaemon(true);
        workerThread.start();
    }

    // hand a camera frame to the detector - safe to call from any thread
    public void submitFrame(BufferedImage frame) {
        pendingFrame.set(frame);
    }

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