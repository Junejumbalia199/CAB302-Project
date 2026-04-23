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
        // Hand the Stage off to the Navigator and open on landing.
        // Every page is reachable from there now: landing -> selection,
        // landing -> auth -> selection, etc.
        new Navigator(primaryStage).showLanding();
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