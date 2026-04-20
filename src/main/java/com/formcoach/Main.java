package com.formcoach;

import com.formcoach.landingpage.landingpage;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

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
    }

    public static void main(String[] args) {
        // This launches the JavaFX application
        launch(args);
    }
}