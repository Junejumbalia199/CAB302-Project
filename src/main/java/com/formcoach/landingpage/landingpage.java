package com.formcoach.landingpage;

import com.formcoach.auth.AuthPage;
import com.formcoach.chatbot.chatbot;
import com.formcoach.home.home;
import com.formcoach.selection.ExerciseSelectionPage;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

public class landingpage extends StackPane {

    public landingpage() {
        // Load the shared stylesheet. I use a leading slash so it resolves
        // from the classpath root rather than this class's package — I kept
        // chasing that bug before I figured out why it was null.
        this.getStylesheets().add(getClass().getResource("/styles/styles.css").toExternalForm());

        // Main Layout Container
        VBox mainLayout = new VBox();

        // --- Navigation Bar ---
        HBox navBar = new HBox(40);
        // navBar.getStyleClass().add("nav-bar");           // "nav-bar" isn't in styles.css — left here for history
        navBar.getStyleClass().add("navbar");               // real class: white band + bottom border

        Text logo = new Text("FormCoach");
        // logo.getStyleClass().add("logo-text");           // "logo-text" was never defined in styles.css
        logo.getStyleClass().add("navbar-title");           // bold dark label, the intended logo style

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnHome = new Button("Home");
        // btnHome.getStyleClass().addAll("nav-link", "nav-link-active");  // neither class exists in styles.css
        btnHome.getStyleClass().addAll("nav-button", "active");            // real classes: base + active highlight

        Button btnExercises = new Button("Exercises");
        // btnExercises.getStyleClass().add("nav-link");   // "nav-link" not defined
        btnExercises.getStyleClass().add("nav-button");    // styled by the .nav-button rule

        Button btnHistory = new Button("History");
        // btnHistory.getStyleClass().add("nav-link");     // same issue as above
        btnHistory.getStyleClass().add("nav-button");      // use the real class

        Button btnProfile = new Button("Profile");
        // btnProfile.getStyleClass().add("nav-link");     // same issue as above
        btnProfile.getStyleClass().add("nav-button");      // use the real class

        // Wire up the nav buttons. Each one just swaps the scene root.
        // I don't have a History screen yet so I'm pointing it at home for
        // now — I'll build a real one later.
        btnHome.setOnAction(e     -> swap(new home()));
        btnExercises.setOnAction(e -> swap(new ExerciseSelectionPage()));
        btnHistory.setOnAction(e  -> swap(new home()));
        btnProfile.setOnAction(e  -> swap(new AuthPage()));

        navBar.getChildren().addAll(logo, spacer, btnHome, btnExercises, btnHistory, btnProfile);

        // --- Hero Content ---
        VBox heroSection = new VBox(25);
        // heroSection.getStyleClass().add("hero-container");   // "hero-container" isn't defined anywhere; keeping the line commented in case we add the class later
        heroSection.setPadding(new Insets(80, 40, 40, 40));      // inline padding so the hero isn't flush against the nav bar
        heroSection.setMaxWidth(800);
        heroSection.setAlignment(Pos.CENTER_LEFT);

        Label badge = new Label("⚡ AI-Powered Form Coaching");
        badge.getStyleClass().add("badge");

        // Rich text for the headline
        Text t1 = new Text("Perfect your ");
        // t1.getStyleClass().add("headline");   // "headline" isn't defined — this is why "Perfect your" was rendering tiny
        t1.getStyleClass().add("heading-xl");    // real class: 36px bold dark text
        Text t2 = new Text("exercise\nform");
        // t2.getStyleClass().add("headline-accent");              // "headline-accent" isn't defined
        t2.getStyleClass().addAll("heading-xl", "heading-accent"); // needs heading-xl for size + heading-accent for the blue fill
        t2.setStyle("-fx-font-size: 64px; -fx-font-weight: 900;");
        Text t3 = new Text(" with AI");
        // t3.getStyleClass().add("headline");   // same fix as t1
        t3.getStyleClass().add("heading-xl");    // real class

        TextFlow headline = new TextFlow(t1, t2, t3);

        Label description = new Label("Get instant feedback on your technique. Our AI coach\nanalyzes your movements through your camera and helps\nyou exercise safely and effectively.");
        description.getStyleClass().add("subtext");
        description.setWrapText(true);

        HBox actionButtons = new HBox(15);
        Button btnBrowse = new Button("Browse Exercises  →");
        btnBrowse.getStyleClass().add("btn-primary");
        btnBrowse.setOnAction(e -> swap(new ExerciseSelectionPage()));

        Button btnProgress = new Button("View Progress");
        btnProgress.getStyleClass().add("btn-secondary");
        btnProgress.setOnAction(e -> swap(new home()));
        actionButtons.getChildren().addAll(btnBrowse, btnProgress);

        heroSection.getChildren().addAll(badge, headline, description, actionButtons);

        mainLayout.getChildren().addAll(navBar, heroSection);

        // chatbot button for me (nathan) to link to chatbot.java
        Button fab = new Button("💬");
        // fab.getStyleClass().add("chat-fab");   // "chat-fab" isn't defined in styles.css
        fab.getStyleClass().add("chat-button");   // real class: blue circle + hover state
        fab.setTextFill(Color.WHITE);
        fab.setOnAction(e -> swap(new chatbot()));
        StackPane.setAlignment(fab, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(fab, new Insets(30));

        this.getChildren().addAll(mainLayout, fab);
    }

    /**
     * My tiny nav helper. Every button in here calls this to swap the scene
     * root. I didn't want a whole router class for what amounts to one line.
     */
    private void swap(Parent next) {
        Scene scene = getScene();
        if (scene != null) scene.setRoot(next);
    }
}