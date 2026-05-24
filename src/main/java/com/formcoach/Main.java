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

/**
 * JavaFX application entry point for FormCoach.
 * Starts the Python MediaPipe pose server as a child process, opens the landing page via
 * {@link Navigator}, and warms up the Kaggle exercise dataset on a background thread.
 */
public class Main extends Application {

    /** Default constructor required by the JavaFX launcher. */
    public Main() {}

    // One repo alive for the whole app lifetime. After the first load the
    // parsed rows sit in memory and any screen that wants them can call
    // Main.exercises().all() instead of re-parsing the CSV.
    private static final ExerciseRepository exerciseRepo = new ExerciseRepository();

    // the running Python server process, kept so we can kill it on exit
    private static Process poseServerProcess;

    /**
     * Returns the shared {@link ExerciseRepository} instance loaded at startup.
     * @return the application-wide exercise repository
     */
    public static ExerciseRepository exercises() {
        return exerciseRepo;
    }

    /**
     * JavaFX lifecycle method — called after the toolkit is initialised.
     * Wires navigation, starts the pose server, and kicks off dataset pre-loading.
     * @param primaryStage the primary window provided by the JavaFX runtime
     */
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

    // starts scripts/main.py as a child process - installs dependencies first if needed
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
                    System.err.println("[pose] no Python interpreter found - pose detection disabled");
                    return;
                }

                // auto-install any missing packages before starting
                installDependencies(python);

                // confirm mediapipe is available after the install step
                if (!checkImport(python, "mediapipe")) {
                    System.err.println("[pose] mediapipe unavailable after install - pose detection disabled");
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

    // runs pip install -r requirements.txt using the found Python interpreter
    // pip skips packages that are already installed so this is fast after the first run
    private static void installDependencies(String python) {
        try {
            File requirements = new File("scripts/requirements.txt");
            if (!requirements.exists()) {
                System.err.println("[pose] requirements.txt not found, skipping install");
                return;
            }
            System.out.println("[pose] checking Python dependencies (first run may take a moment)...");
            ProcessBuilder pb = new ProcessBuilder(
                    python, "-m", "pip", "install", "-q", "-r", requirements.getAbsolutePath());
            pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
            pb.redirectErrorStream(true);
            int exit = pb.start().waitFor();
            if (exit == 0) {
                System.out.println("[pose] dependencies ready");
            } else {
                System.err.println("[pose] pip install failed with exit code " + exit);
            }
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            System.err.println("[pose] pip install error: " + e.getMessage());
        }
    }

    private static void stopPoseServer() {
        if (poseServerProcess != null && poseServerProcess.isAlive()) {
            poseServerProcess.destroy();
            System.out.println("[pose] Python server stopped");
        }
    }

    // finds the first Python interpreter available on PATH, any version
    private static String findPython() {
        boolean isWindows = System.getProperty("os.name", "").toLowerCase().contains("win");

        // on Windows, "py" is the Python Launcher and reliably finds the right install
        String[] candidates = isWindows
                ? new String[]{"py", "python", "python3"}
                : new String[]{"python3", "python"};

        for (String candidate : candidates) {
            try {
                Process probe = new ProcessBuilder(candidate, "--version")
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

    // checks whether a given module can be imported with the chosen interpreter
    private static boolean checkImport(String python, String module) {
        try {
            Process probe = new ProcessBuilder(python, "-c", "import " + module)
                    .redirectErrorStream(true)
                    .start();
            return probe.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Application entry point — delegates to JavaFX {@link Application#launch}.
     * @param args command-line arguments (not used)
     */
    public static void main(String[] args) {
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