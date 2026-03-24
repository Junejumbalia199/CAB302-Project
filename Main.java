import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.Scanner;

public class Main extends JPanel {
    private int fingerX = 0;
    private int fingerY = 0;

    public Main() {
        JFrame frame = new JFrame("MediaPipe Java Demo");
        frame.setSize(800, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(this);
        frame.setVisible(true);

        // Start the Python Bridge in a background thread
        new Thread(this::startPythonBridge).start();
    }

    private void startPythonBridge() {
        try {
            // Change "python" to "python3" if on Mac/Linux
            ProcessBuilder pb = new ProcessBuilder("python", "finger_tracker.py");
            pb.redirectErrorStream(true);
            Process process = pb.start();

            Scanner sc = new Scanner(process.getInputStream());
            while (sc.hasNextLine()) {
                String line = sc.nextLine();
                String[] coords = line.split(",");
                if (coords.length == 2) {
                    // MediaPipe gives 0.0 to 1.0, we scale to window size
                    fingerX = (int) (Double.parseDouble(coords[0]) * getWidth());
                    fingerY = (int) (Double.parseDouble(coords[1]) * getHeight());
                    repaint(); // Redraw the UI
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, getWidth(), getHeight());

        // Draw a target circle at the finger position
        g.setColor(Color.RED);
        g.fillOval(fingerX - 25, fingerY - 25, 50, 50);

        g.setColor(Color.BLACK);
        g.drawString("Finger Position: " + fingerX + ", " + fingerY, 20, 20);
    }

    public static void main(String[] args) {
        new Main();
    }
}