package com.formcoach.historypage;

import com.formcoach.Navigator;
import com.formcoach.chatbot.chatbot;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.net.URL;

public class HistoryPage {

    private final Stage stage;

    public HistoryPage(Stage stage) {
        this.stage = stage;
    }

    public void show() {
        Scene scene = createScene();
        stage.setScene(scene);
        stage.setTitle("FormCoach - Progress History");
        stage.setMinWidth(1280);
        stage.setMinHeight(760);
        stage.show();
    }

    private Scene createScene() {
        StackPane root = new StackPane();

        BorderPane layout = new BorderPane();
        layout.getStyleClass().add("root-pane");

        VBox topWrapper = new VBox(createNavigationBar());
        topWrapper.setPadding(new Insets(18, 24, 0, 24));
        layout.setTop(topWrapper);

        VBox pageContent = new VBox(24);
        pageContent.setAlignment(Pos.TOP_CENTER);
        pageContent.setPadding(new Insets(26, 24, 120, 24));
        pageContent.getChildren().addAll(
                createHeader(),
                createHistoryCard()
        );

        layout.setCenter(pageContent);

        StackPane chatbotWidget = createChatbotWidget();
        StackPane.setAlignment(chatbotWidget, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(chatbotWidget, new Insets(0, 24, 24, 0));

        root.getChildren().addAll(layout, chatbotWidget);

        Scene scene = new Scene(root, 1280, 760);
        applyCss(scene);
        return scene;
    }

    private HBox createNavigationBar() {
        HBox navBar = new HBox(40);
        navBar.getStyleClass().add("nav-bar");
        navBar.setAlignment(Pos.CENTER_LEFT);

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

        Navigator navigator = new Navigator(stage);

        Button btnHome = createNavButton("Home", false);
        btnHome.setOnAction(e -> navigator.showLanding());

        Button btnExercises = createNavButton("Exercises", false);
        btnExercises.setOnAction(e -> navigator.showSelection());

        Button btnHistory = createNavButton("History", true);

        Button btnProfile = createNavButton("Profile", false);
        btnProfile.setOnAction(e -> navigator.showProfile());

        navBar.getChildren().addAll(logo, spacer, btnHome, btnExercises, btnHistory, btnProfile);
        return navBar;
    }

    private Button createNavButton(String text, boolean active) {
        Button button = new Button(text);
        button.getStyleClass().add("nav-link");
        if (active) {
            button.getStyleClass().add("nav-link-active");
        }
        return button;
    }

    private VBox createHeader() {
        VBox wrapper = new VBox(10);
        wrapper.setMaxWidth(1460);
        wrapper.setAlignment(Pos.TOP_LEFT);
        wrapper.setPadding(new Insets(0, 44, 0, 44));

        Label title = new Label("Progress History");
        title.getStyleClass().add("page-title");

        Label subtitle = new Label("Track your form improvement over time");
        subtitle.getStyleClass().add("page-subtitle");

        wrapper.getChildren().addAll(title, subtitle);
        return wrapper;
    }

    private StackPane createHistoryCard() {
        StackPane wrapper = new StackPane();
        wrapper.setMaxWidth(1460);
        wrapper.setPadding(new Insets(0, 44, 0, 44));
        wrapper.setAlignment(Pos.TOP_CENTER);

        VBox card = new VBox(12);
        card.getStyleClass().add("history-card");
        card.setAlignment(Pos.CENTER);
        card.setPrefWidth(980);
        card.setMaxWidth(980);
        card.setPrefHeight(340);

        Label title = new Label("No sessions yet");
        title.getStyleClass().add("history-empty-title");

        Label subtitle = new Label("Complete a form assessment to see your progress here");
        subtitle.getStyleClass().add("history-empty-subtitle");

        card.getChildren().addAll(title, subtitle);
        wrapper.getChildren().add(card);

        return wrapper;
    }

    private StackPane createChatbotWidget() {
        StackPane outer = new StackPane();
        outer.setPickOnBounds(false);
        outer.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        HBox widget = new HBox(12);
        widget.getStyleClass().add("chat-widget");
        widget.setAlignment(Pos.CENTER_LEFT);
        widget.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        StackPane iconCircle = new StackPane();
        iconCircle.getStyleClass().add("chat-icon-circle");

        SVGPath chatIcon = new SVGPath();
        chatIcon.setContent("M6 6 H18 Q20 6 20 8 V14 Q20 16 18 16 H11 L8 19 V16 H6 Q4 16 4 14 V8 Q4 6 6 6 Z");
        chatIcon.setStyle("-fx-fill: white;");
        iconCircle.getChildren().add(chatIcon);

        widget.getChildren().add(iconCircle);
        widget.setOnMouseClicked(e -> chatbot.showChatbot(stage));

        outer.getChildren().add(widget);
        return outer;
    }

    private void applyCss(Scene scene) {
        URL css = getClass().getResource("/styles/selection.css");
        if (css == null) {
            throw new IllegalStateException("Could not load /styles/selection.css");
        }
        scene.getStylesheets().add(css.toExternalForm());
    }
}