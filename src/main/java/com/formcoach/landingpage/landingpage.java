package com.formcoach.landingpage;

import com.formcoach.chatbot.chatbot;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class landingpage extends StackPane {

    public landingpage() {
        this.getStylesheets().add(getClass().getResource("/styles/landingpage.css").toExternalForm());

        // Main Layout Container
        VBox mainLayout = new VBox();

        // --- Navigation Bar ---
        HBox navBar = new HBox(40);
        navBar.getStyleClass().add("nav-bar");

        // test comment
        Image logoImage = new Image(getClass().getResourceAsStream("/assets/FClogo.png"));
        ImageView logoIcon = new ImageView(logoImage);
        logoIcon.setFitHeight(36);
        logoIcon.setPreserveRatio(true);

        Text logoText = new Text("FormCoach");
        logoText.getStyleClass().add("logo-text");

        HBox logo = new HBox(10, logoIcon, logoText);
        logo.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnHome = new Button("Home");
        btnHome.getStyleClass().addAll("nav-link", "nav-link-active");

        Button btnExercises = new Button("Exercises");
        btnExercises.getStyleClass().add("nav-link");

        Button btnHistory = new Button("History");
        btnHistory.getStyleClass().add("nav-link");

        Button btnProfile = new Button("Profile");
        btnProfile.getStyleClass().add("nav-link");

        navBar.getChildren().addAll(logo, spacer, btnHome, btnExercises, btnHistory, btnProfile);

        // --- Hero Content ---
        VBox heroSection = new VBox(25);
        heroSection.getStyleClass().add("hero-container");
        heroSection.setMaxWidth(800);
        heroSection.setAlignment(Pos.CENTER_LEFT);

        Label badge = new Label("⚡ AI-Powered Form Coaching");
        badge.getStyleClass().add("badge");

        // Rich text for the headline
        Text t1 = new Text("Perfect your ");
        t1.getStyleClass().add("headline");
        Text t2 = new Text("exercise\nform");
        t2.getStyleClass().add("headline-accent");
        t2.setStyle("-fx-font-size: 64px; -fx-font-weight: 900;");
        Text t3 = new Text(" with AI");
        t3.getStyleClass().add("headline");

        TextFlow headline = new TextFlow(t1, t2, t3);

        Label description = new Label("Get instant feedback on your technique. Our AI coach\nanalyzes your movements through your camera and helps\nyou exercise safely and effectively.");
        description.getStyleClass().add("subtext");
        description.setWrapText(true);

        HBox actionButtons = new HBox(15);
        Button btnBrowse = new Button("Browse Exercises  →");
        btnBrowse.getStyleClass().add("btn-primary");

        Button btnProgress = new Button("View Progress");
        btnProgress.getStyleClass().add("btn-secondary");
        actionButtons.getChildren().addAll(btnBrowse, btnProgress);

        heroSection.getChildren().addAll(badge, headline, description, actionButtons);

        mainLayout.getChildren().addAll(navBar, heroSection);

        // chatbot button
        Button fab = new Button("💬");
        fab.getStyleClass().add("chat-fab");
        fab.setTextFill(Color.WHITE);
        fab.setOnAction(e -> chatbot.showChatbot(this.getScene().getWindow()));
        StackPane.setAlignment(fab, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(fab, new Insets(30));

        this.getChildren().addAll(mainLayout, fab);
    }
}