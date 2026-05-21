package com.formcoach;

import com.formcoach.selection.ExerciseSelectionPage;
import com.formcoach.auth.AuthPage;
import com.formcoach.landingpage.landingpage;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import kaggle.ExerciseDataset;
import kaggle.ExerciseRepository;

import java.io.File;
import java.util.List;

public class Main extends Application {

    // One repo alive for the whole app lifetime. After the first load the
    // parsed rows sit in memory and any screen that wants them can call
    // Main.exercises().all() instead of re-parsing the CSV.
    private static final ExerciseRepository exerciseRepo = new ExerciseRepository();

    // the running Python server process, kept so we can kill it on exit
    private static Process poseServerProcess;

    public static ExerciseRepository exercises() {
        return exerciseRepo;
    }

    @Override
    public void start(Stage primaryStage) {
        startPoseServer();

        // Hand the Stage off to the Navigator and open on landing.
        // Every page is reachable from there now: landing -> selection,
        // landing -> auth -> selection, etc.
        new Navigator(primaryStage).showLanding();

        // shut the Python server down cleanly when the window closes
        primaryStage.setOnCloseRequest(e -> stopPoseServer());

        // also covers kills via task manager / kill signal
        Runtime.getRuntime().addShutdownHook(new Thread(Main::stopPoseServer, "pose-server-shutdown"));

        // Kick off the Kaggle dataset load on a background daemon thread.
        // First run has to download the CSV; subsequent runs hit the local
        // cache and return in a few ms. If something fails (no kaggle.json,
        // no internet, whatever) I log it and carry on — the selection
        // page has a hardcoded fallback so the app stays usable.
        Thread warmup = new Thread(() -> {
            try {
                List<ExerciseDataset> rows = exerciseRepo.ensureLoaded();
                System.out.println("[Kaggle] loaded " + rows.size() + " exercise rows");
            } catch (Exception ex) {
                System.err.println("[Kaggle] warmup skipped: " + ex.getMessage());
            }
        }, "kaggle-warmup");
        warmup.setDaemon(true);
        warmup.start();
    }

    // starts scripts/main.py as a child process - PoseDetector will connect once it's ready
    private static void startPoseServer() {
        Thread t = new Thread(() -> {
            try {
                File script = new File("scripts/main.py");
                if (!script.exists()) {
                    System.err.println("[pose] scripts/main.py not found - pose detection disabled");
                    return;
                }

                String python = findPython();
                if (python == null) {
                    System.err.println("[pose] no Python with mediapipe found - run: pip install mediapipe");
                    return;
                }

                ProcessBuilder pb = new ProcessBuilder(python, script.getAbsolutePath());
                pb.directory(new File("scripts"));

                // forward Python stdout/stderr into the Java console
                pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
                pb.redirectError(ProcessBuilder.Redirect.INHERIT);

                poseServerProcess = pb.start();
                System.out.println("[pose] Python server started (pid " + poseServerProcess.pid() + ")");

                int exitCode = poseServerProcess.waitFor();
                System.out.println("[pose] Python server exited with code " + exitCode);

            } catch (InterruptedException ignored) {
                // normal during shutdown
            } catch (Exception e) {
                System.err.println("[pose] failed to start Python server: " + e.getMessage());
            }
        }, "pose-server-launcher");
        t.setDaemon(true);
        t.start();
    }

    private static void stopPoseServer() {
        if (poseServerProcess != null && poseServerProcess.isAlive()) {
            poseServerProcess.destroy();
            System.out.println("[pose] Python server stopped");
        }
    }

    // finds a Python that actually has mediapipe installed, not just any Python on PATH
    private static String findPython() {
        boolean isWindows = System.getProperty("os.name", "").toLowerCase().contains("win");

        // on Windows, "py" is the Python Launcher and reliably finds the right install
        String[] candidates = isWindows
                ? new String[]{"py", "python", "python3"}
                : new String[]{"python3", "python"};

        for (String candidate : candidates) {
            try {
                // verify mediapipe is actually importable, not just that Python exists
                Process probe = new ProcessBuilder(candidate, "-c", "import mediapipe")
                        .redirectErrorStream(true)
                        .start();
                if (probe.waitFor() == 0) {
                    System.out.println("[pose] using Python: " + candidate);
                    return candidate;
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    public static void main(String[] args) {
        // This launches the JavaFX application
        launch(args);
    }
}


//login page run code, once the db is implemented remove and switch "null"
/*public class Main extends Application {

    @Override
    public void start(Stage stage) {
        AuthPage authPage = new AuthPage(
                stage,
                () -> System.out.println("Back pressed"),
                () -> System.out.println("Auth successful"),
                null
        );

        authPage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
*/

//exercise selection page code, needs to be linked with the tutorial page
/*public class Main extends Application {

    @Override
    public void start(Stage stage) {
        ExerciseSelectionPage selectionPage = new ExerciseSelectionPage(
                stage,
                () -> System.out.println("Back pressed")
        );

        selectionPage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
*/