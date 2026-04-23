package com.formcoach;

import com.formcoach.selection.ExerciseSelectionPage;
import com.formcoach.auth.AuthPage;
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