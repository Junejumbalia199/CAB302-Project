package com.formcoach.profile;

import com.formcoach.auth.AuthSession;
import com.formcoach.auth.User;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.net.URL;

public class ProfilePage {

    private final Stage stage;
    private final Runnable onHome;
    private final Runnable onExercises;
    private final Runnable onLogout;

    public ProfilePage(Stage stage, Runnable onHome, Runnable onExercises, Runnable onLogout) {
        this.stage = stage;
        this.onHome = onHome;
        this.onExercises = onExercises;
        this.onLogout = onLogout;
    }

    public void show() {
        User user = AuthSession.getCurrentUser();

        BorderPane root = new BorderPane();
        root.getStyleClass().add("root-pane");

        VBox topWrapper = new VBox(createNavigationBar());
        topWrapper.setPadding(new Insets(18, 24, 0, 24));
        root.setTop(topWrapper);

        VBox content = new VBox(24);
        content.setAlignment(Pos.TOP_CENTER);
        content.setPadding(new Insets(30, 24, 40, 24));

        VBox header = new VBox(8);
        header.setAlignment(Pos.CENTER);

        Label title = new Label("Your Profile");
        title.getStyleClass().add("page-title");

        Label subtitle = new Label("View your account details and manage your session");
        subtitle.getStyleClass().add("page-subtitle");

        header.getChildren().addAll(title, subtitle);

        VBox profileCard = new VBox(18);
        profileCard.getStyleClass().add("profile-card");
        profileCard.setMaxWidth(520);
        profileCard.setAlignment(Pos.CENTER_LEFT);

        Label accountHeading = new Label("Account Details");
        accountHeading.getStyleClass().add("profile-heading");

        VBox usernameBox = createInfoRow("Username", user != null ? user.getUsername() : "Not logged in");
        VBox emailBox = createInfoRow("Email", user != null ? user.getEmail() : "Not logged in");
        VBox statusBox = createInfoRow("Status", user != null ? "Logged in" : "Not logged in");

        HBox buttonRow = new HBox(14);
        buttonRow.setAlignment(Pos.CENTER_LEFT);

        Button exercisesButton = new Button("Back to Exercises");
        exercisesButton.getStyleClass().add("btn-secondary");
        exercisesButton.setOnAction(e -> {
            if (onExercises != null) {
                onExercises.run();
            }
        });


        Button authButton = new Button(user != null ? "Logout" : "Log In");
        authButton.getStyleClass().add("btn-primary");
        authButton.setOnAction(e -> {
            if (user != null) {
                AuthSession.clear();
            }
            if (onLogout != null) {
                onLogout.run();
            }
        });

        buttonRow.getChildren().addAll(exercisesButton, authButton);

        profileCard.getChildren().addAll(
                accountHeading,
                usernameBox,
                emailBox,
                statusBox,
                buttonRow
        );

        content.getChildren().addAll(header, profileCard);
        root.setCenter(content);

        Scene scene = new Scene(root, 1280, 760);
        applyCss(scene);

        stage.setScene(scene);
        stage.setTitle("FormCoach - Profile");
        stage.show();
    }

    private HBox createNavigationBar() {
        HBox navBar = new HBox(40);
        navBar.getStyleClass().add("nav-bar");
        navBar.setAlignment(Pos.CENTER_LEFT);

        Text logo = new Text("FormCoach");
        logo.getStyleClass().add("logo-text");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnHome = createNavButton("Home", false, onHome);
        Button btnExercises = createNavButton("Exercises", false, onExercises);
        Button btnHistory = createNavButton("History", false, () -> System.out.println("History clicked"));
        Button btnProfile = createNavButton("Profile", true, null);

        navBar.getChildren().addAll(logo, spacer, btnHome, btnExercises, btnHistory, btnProfile);
        return navBar;
    }

    private Button createNavButton(String text, boolean active, Runnable action) {
        Button button = new Button(text);
        button.getStyleClass().add("nav-link");
        if (active) {
            button.getStyleClass().add("nav-link-active");
        }
        button.setOnAction(e -> {
            if (action != null) {
                action.run();
            }
        });
        return button;
    }

    private VBox createInfoRow(String labelText, String valueText) {
        Label label = new Label(labelText);
        label.getStyleClass().add("info-label");

        Label value = new Label(valueText);
        value.getStyleClass().add("info-value");

        VBox box = new VBox(6, label, value);
        return box;
    }

    private void applyCss(Scene scene) {
        URL css = getClass().getResource("/styles/profile.css");
        if (css == null) {
            throw new IllegalStateException("Could not load /styles/profile.css");
        }
        scene.getStylesheets().add(css.toExternalForm());
    }
}