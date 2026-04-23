package com.formcoach;

import com.formcoach.selection.ExerciseSelectionPage;
import com.formcoach.auth.AuthPage;
import com.formcoach.landingpage.landingpage;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import kaggle.ExerciseDataset;
import kaggle.ExerciseRepository;

import java.util.List;

public class Main extends Application {

    // One repo alive for the whole app lifetime. After the first load the
    // parsed rows sit in memory and any screen that wants them can call
    // Main.exercises().all() instead of re-parsing the CSV.
    private static final ExerciseRepository exerciseRepo = new ExerciseRepository();

    public static ExerciseRepository exercises() {
        return exerciseRepo;
    }

    @Override
    public void start(Stage primaryStage) {
        // Hand the Stage off to the Navigator and open on landing.
        // Every page is reachable from there now: landing -> selection,
        // landing -> auth -> selection, etc.
        new Navigator(primaryStage).showLanding();

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

