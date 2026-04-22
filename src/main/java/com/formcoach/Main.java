package com.formcoach;

import com.formcoach.landingpage.landingpage;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import kaggle.ExerciseDataset;
import kaggle.ExerciseRepository;

import java.util.List;

public class Main extends Application {

    // I keep one repo alive for the whole app. After the first load the
    // parsed rows sit in memory and any screen can grab them with
    // Main.exercises().all() — I didn't want every page re-parsing the CSV.
    private static final ExerciseRepository exerciseRepo = new ExerciseRepository();

    public static ExerciseRepository exercises() {
        return exerciseRepo;
    }

    @Override
    public void start(Stage primaryStage) {
        // 1. Create an instance of your landing page class
        landingpage root = new landingpage();

        // 2. Create the Scene (Width x Height)
        Scene scene = new Scene(root, 1200, 800);

        // 3. Configure the Window (Stage)
        primaryStage.setTitle("FormCoach - AI Powered Form Coaching");
        primaryStage.setScene(scene);

        // 4. Show the window
        primaryStage.show();

        // 5. Kick off the Kaggle dataset load in the background. I use a
        //    daemon thread so the window paints instantly on first launch
        //    — no user should be staring at a white screen while I pull a
        //    CSV. If it fails (no kaggle.json, no internet, whatever) I
        //    just log it and carry on; the fallback list in the selection
        //    page keeps the app usable.
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