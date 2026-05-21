package com.formcoach.selection;

import com.formcoach.chatbot.chatbot;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class ExerciseSelectionPage {
    
    private static final int PAGE_SIZE = 3;

    private final Stage stage;
    private final Runnable onBack;
    private final Runnable onProfile;
    private final Runnable onHistory;

    private final List<ExerciseItem> allExercises = List.of(
            new ExerciseItem(
                    "Push-ups",
                    "Strengthen your chest, shoulders, triceps, and core through controlled bodyweight movement.",
                    "/assets/76861.png",
                    "Upper Body"
            ),
            new ExerciseItem(
                    "Sit-ups",
                    "Build core strength by lifting your torso toward your knees with controlled abdominal movement.",
                    "/assets/sit-up.png",
                    "Core"
            ),
            new ExerciseItem(
                    "Squats",
                    "Strengthen your legs and glutes by lowering your hips and standing back up with good posture.",
                    "/assets/77784-200.png",
                    "Lower Body"
            ),
            new ExerciseItem(
                    "Lunges",
                    "N/A",
                    "/assets/comingsoon.png",
                    "Lower Body"
            ),
            new ExerciseItem(
                    "Plank",
                    "N/A",
                    "/assets/comingsoon.png",
                    "Core"
            ),
            new ExerciseItem(
                    "Glute Bridges",
                    "N/A",
                    "/assets/comingsoon.png",
                    "Lower Body"
            )
    );

    private HBox cardContainer;
    private HBox pageIndicatorBox;
    private Label pageInfoLabel;
    private Button previousPageButton;
    private Button nextPageButton;
    private TextField searchField;

    private int currentPageIndex = 0;

    public ExerciseSelectionPage(Stage stage, Runnable onBack, Runnable onProfile, Runnable onHistory) {
        this.stage = stage;
        this.onBack = onBack;
        this.onProfile = onProfile;
        this.onHistory = onHistory;
    }

    public void show() {
        Scene scene = createScene();
        stage.setScene(scene);
        stage.setTitle("FormCoach - Exercise Selection");
        stage.setMinWidth(1280);
        stage.setMinHeight(800);
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
                createSearchBar(),
                createExerciseSection()
        );

        ScrollPane scrollPane = new ScrollPane(pageContent);
        scrollPane.getStyleClass().add("selection-scroll");
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setPannable(true);

        layout.setCenter(scrollPane);

        Button helpButton = createHelpButton();
        StackPane.setAlignment(helpButton, Pos.BOTTOM_LEFT);
        StackPane.setMargin(helpButton, new Insets(0, 0, 24, 24));

        StackPane chatbotWidget = createChatbotWidget();
        StackPane.setAlignment(chatbotWidget, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(chatbotWidget, new Insets(0, 24, 24, 0));

        root.getChildren().addAll(layout, helpButton, chatbotWidget);

        Scene scene = new Scene(root, 1280, 800);
        applyCss(scene);
        refreshExerciseCards();
        return scene;
    }

    private HBox createNavigationBar() {
        HBox navBar = new HBox(40);
        navBar.getStyleClass().add("nav-bar");
        navBar.setAlignment(Pos.CENTER_LEFT);

        Image logoImage = new Image(getClass().getResourceAsStream("/assets/FClogo.png"));
        ImageView logoIcon = new ImageView(logoImage);
        logoIcon.setFitHeight(50);
        logoIcon.setPreserveRatio(true);

        Text logoText = new Text("FormCoach");
        logoText.getStyleClass().add("logo-text");

        HBox logo = new HBox(10, logoIcon, logoText);
        logo.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnHome = createNavButton("Home", false, () -> {
            if (onBack != null) {
                onBack.run();
            }
        });

        Button btnExercises = createNavButton("Exercises", true, null);
        Button btnHistory = createNavButton("History", false, () -> {
            if (onHistory != null) {
                onHistory.run();
            }
        });

        Button btnProfile = createNavButton("Profile", false, () -> {
            if (onProfile != null) {
                onProfile.run();
            }
        });

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

    private VBox createHeader() {
        VBox wrapper = new VBox(10);
        wrapper.setMaxWidth(1460);
        wrapper.setAlignment(Pos.TOP_LEFT);
        wrapper.setPadding(new Insets(0, 44, 0, 44));

        Label title = new Label("Exercise Library");
        title.getStyleClass().add("page-title");

        Label subtitle = new Label("Browse exercises and get AI form coaching");
        subtitle.getStyleClass().add("page-subtitle");

        wrapper.getChildren().addAll(title, subtitle);
        return wrapper;
    }

    private HBox createSearchBar() {
        HBox wrapper = new HBox();
        wrapper.setMaxWidth(1460);
        wrapper.setAlignment(Pos.CENTER);
        wrapper.setPadding(new Insets(0, 44, 0, 44));

        HBox searchShell = new HBox(10);
        searchShell.getStyleClass().add("search-shell");
        searchShell.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(searchShell, Priority.ALWAYS);
        searchShell.setMaxWidth(Double.MAX_VALUE);

        Label searchIcon = new Label("⌕");
        searchIcon.getStyleClass().add("search-icon");

        searchField = new TextField();
        searchField.setPromptText("Search exercises...");
        searchField.getStyleClass().add("search-field");
        HBox.setHgrow(searchField, Priority.ALWAYS);

        searchField.textProperty().addListener((obs, oldValue, newValue) -> {
            currentPageIndex = 0;
            refreshExerciseCards();
        });

        searchShell.getChildren().addAll(searchIcon, searchField);
        wrapper.getChildren().add(searchShell);

        return wrapper;
    }

    private VBox createExerciseSection() {
        cardContainer = new HBox(22);
        cardContainer.setAlignment(Pos.CENTER);

        previousPageButton = createPageButton("‹", -1);
        nextPageButton = createPageButton("›", 1);

        HBox row = new HBox(16, previousPageButton, cardContainer, nextPageButton);
        row.setAlignment(Pos.CENTER);

        pageIndicatorBox = new HBox(10);
        pageIndicatorBox.setAlignment(Pos.CENTER);

        pageInfoLabel = new Label();
        pageInfoLabel.getStyleClass().add("page-info");

        VBox wrapper = new VBox(22, row, pageIndicatorBox, pageInfoLabel);
        wrapper.setAlignment(Pos.CENTER);
        return wrapper;
    }

    private Button createPageButton(String symbol, int direction) {
        Button button = new Button(symbol);
        button.getStyleClass().add("page-nav-button");
        button.setOnAction(e -> changePage(direction));
        return button;
    }

    private void changePage(int direction) {
        int pageCount = getPageCount();
        int nextIndex = currentPageIndex + direction;

        if (nextIndex >= 0 && nextIndex < pageCount) {
            currentPageIndex = nextIndex;
            refreshExerciseCards();
        }
    }

    private void refreshExerciseCards() {
        List<ExerciseItem> filteredExercises = getFilteredExercises();
        int pageCount = Math.max(1, (int) Math.ceil(filteredExercises.size() / (double) PAGE_SIZE));

        if (currentPageIndex >= pageCount) {
            currentPageIndex = pageCount - 1;
        }

        int startIndex = currentPageIndex * PAGE_SIZE;
        int endIndex = Math.min(startIndex + PAGE_SIZE, filteredExercises.size());

        cardContainer.getChildren().clear();

        if (filteredExercises.isEmpty()) {
            cardContainer.getChildren().add(createEmptyStateCard());
        } else {
            for (int i = startIndex; i < endIndex; i++) {
                cardContainer.getChildren().add(createExerciseCard(filteredExercises.get(i)));
            }
        }

        previousPageButton.setDisable(currentPageIndex == 0 || filteredExercises.isEmpty());
        nextPageButton.setDisable(currentPageIndex >= pageCount - 1 || filteredExercises.isEmpty());

        updateDots(pageCount);
        pageInfoLabel.setText(filteredExercises.isEmpty()
                ? "No matching exercises"
                : "Page " + (currentPageIndex + 1) + " of " + pageCount);
    }

    private List<ExerciseItem> getFilteredExercises() {
        String query = searchField == null ? "" : searchField.getText().trim().toLowerCase();

        if (query.isEmpty()) {
            return allExercises;
        }

        List<ExerciseItem> filtered = new ArrayList<>();
        for (ExerciseItem exercise : allExercises) {
            if (exercise.name.toLowerCase().contains(query)
                    || exercise.description.toLowerCase().contains(query)
                    || exercise.category.toLowerCase().contains(query)) {
                filtered.add(exercise);
            }
        }
        return filtered;
    }

    private int getPageCount() {
        return Math.max(1, (int) Math.ceil(getFilteredExercises().size() / (double) PAGE_SIZE));
    }

    private void updateDots(int pageCount) {
        pageIndicatorBox.getChildren().clear();

        for (int i = 0; i < pageCount; i++) {
            Region dot = new Region();
            dot.getStyleClass().add(i == currentPageIndex ? "page-dot-active" : "page-dot");

            final int pageIndex = i;
            dot.setOnMouseClicked(e -> {
                currentPageIndex = pageIndex;
                refreshExerciseCards();
            });

            pageIndicatorBox.getChildren().add(dot);
        }
    }

    private VBox createExerciseCard(ExerciseItem exercise) {
        VBox card = new VBox(14);
        card.getStyleClass().add("exercise-card");
        card.setAlignment(Pos.TOP_CENTER);

        StackPane imageBox = new StackPane();
        imageBox.getStyleClass().add("exercise-image-box");
        imageBox.getChildren().add(createExerciseGraphic(exercise));

        Label name = new Label(exercise.name);
        name.getStyleClass().add("exercise-title");

        Label category = new Label(exercise.category);
        category.getStyleClass().add("exercise-chip");

        Label description = new Label(exercise.description);
        description.getStyleClass().add("exercise-description");
        description.setWrapText(true);
        description.setTextAlignment(TextAlignment.CENTER);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Button selectButton = new Button("Select");
        selectButton.getStyleClass().add("btn-primary");
        selectButton.setPrefWidth(150);
        selectButton.setPrefHeight(42);
        selectButton.setOnAction(e -> showTutorialPlaceholder(exercise.name));

        card.getChildren().addAll(imageBox, name, category, description, spacer, selectButton);
        return card;
    }

    private VBox createEmptyStateCard() {
        VBox card = new VBox(12);
        card.getStyleClass().add("exercise-card");
        card.setAlignment(Pos.CENTER);
        card.setPrefWidth(420);
        card.setMinHeight(280);

        Label title = new Label("No exercises found");
        title.getStyleClass().add("exercise-title");

        Label subtitle = new Label("Try a different search term.");
        subtitle.getStyleClass().add("exercise-description");

        card.getChildren().addAll(title, subtitle);
        return card;
    }

    private StackPane createExerciseGraphic(ExerciseItem exercise) {
        StackPane container = new StackPane();

        if (exercise.imagePath != null) {
            URL url = getClass().getResource(exercise.imagePath);
            if (url != null) {
                ImageView imageView = new ImageView(new Image(url.toExternalForm()));
                imageView.setFitWidth(150);
                imageView.setFitHeight(110);
                imageView.setPreserveRatio(true);
                container.getChildren().add(imageView);
                return container;
            }
        }

        StackPane fallback = new StackPane();
        fallback.getStyleClass().add("exercise-fallback-icon");

        Label abbreviation = new Label(getExerciseAbbreviation(exercise.name));
        abbreviation.getStyleClass().add("exercise-fallback-text");

        fallback.getChildren().add(abbreviation);
        container.getChildren().add(fallback);
        return container;
    }

    private String getExerciseAbbreviation(String name) {
        String[] parts = name.split(" ");
        if (parts.length == 1) {
            return name.substring(0, Math.min(2, name.length())).toUpperCase();
        }

        StringBuilder result = new StringBuilder();
        for (String part : parts) {
            if (!part.isBlank()) {
                result.append(Character.toUpperCase(part.charAt(0)));
            }
        }
        return result.toString();
    }

    private Button createHelpButton() {
        Button helpButton = new Button("?");
        helpButton.getStyleClass().add("floating-help");
        helpButton.setOnAction(e -> openHelpDialog());
        return helpButton;
    }

    private StackPane createChatbotWidget() {
        StackPane outer = new StackPane();
        outer.setPickOnBounds(false);
        outer.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        Button fab = new Button("💬");
        fab.getStyleClass().add("chat-fab");
        fab.setTextFill(javafx.scene.paint.Color.WHITE);
        fab.setOnAction(e -> chatbot.showChatbot(stage));

        outer.getChildren().add(fab);
        return outer;
    }

    private void openHelpDialog() {
        Stage helpStage = new Stage();
        helpStage.initOwner(stage);
        helpStage.initModality(Modality.WINDOW_MODAL);
        helpStage.setTitle("FormCoach Help");

        VBox card = new VBox(16);
        card.getStyleClass().add("dialog-card");
        card.setMaxWidth(640);

        Label title = new Label("How to use FormCoach");
        title.getStyleClass().add("dialog-title");

        Label body = new Label(
                "1. Browse exercises with the page arrows or search bar.\n\n" +
                        "2. Press Select to open the tutorial page.\n\n" +
                        "3. Follow the exercise tutorial then open up the form tracker.\n\n" +
                        "4. Click the AI chat assistant if you need further help or information."
        );
        body.getStyleClass().add("exercise-description");
        body.setWrapText(true);
        body.setTextAlignment(TextAlignment.LEFT);

        Button close = new Button("Close");
        close.getStyleClass().add("btn-primary");
        close.setOnAction(e -> helpStage.close());

        VBox wrapper = new VBox(card);
        wrapper.getStyleClass().add("root-pane");
        wrapper.setAlignment(Pos.CENTER);
        wrapper.setPadding(new Insets(30));

        card.getChildren().addAll(title, body, close);

        Scene scene = new Scene(wrapper, 720, 420);
        applyCss(scene);
        helpStage.setScene(scene);
        helpStage.setResizable(false);
        helpStage.showAndWait();
    }

    private void showTutorialPlaceholder(String exerciseName) {
        Stage tutorialStage = new Stage();
        tutorialStage.initOwner(stage);
        tutorialStage.setTitle("FormCoach - " + exerciseName + " Tutorial");

        VBox card = new VBox(18);
        card.getStyleClass().add("dialog-card");
        card.setAlignment(Pos.CENTER);
        card.setMaxWidth(700);

        Label title = new Label(exerciseName + " Tutorial");
        title.getStyleClass().add("dialog-title");

        Label subtitle = new Label(
                "Placeholder page\n\n" +
                        "Tutorial shall go here :)"
        );
        subtitle.getStyleClass().add("exercise-description");
        subtitle.setWrapText(true);
        subtitle.setTextAlignment(TextAlignment.CENTER);

        Button back = new Button("Back");
        back.getStyleClass().add("btn-primary");
        back.setOnAction(e -> tutorialStage.close());

        VBox wrapper = new VBox(card);
        wrapper.getStyleClass().add("root-pane");
        wrapper.setAlignment(Pos.CENTER);
        wrapper.setPadding(new Insets(30));

        card.getChildren().addAll(title, subtitle, back);

        // Create chatbot button for the tutorial window
        Button chatbotFab = new Button("💬");
        chatbotFab.getStyleClass().add("chat-fab");
        chatbotFab.setTextFill(javafx.scene.paint.Color.WHITE);
        chatbotFab.setOnAction(e -> chatbot.showChatbot(tutorialStage));

        VBox chatbotContainer = new VBox(chatbotFab);
        chatbotContainer.setAlignment(Pos.BOTTOM_RIGHT);
        chatbotContainer.setPadding(new Insets(0, 24, 24, 0));

        BorderPane root = new BorderPane();
        root.setCenter(wrapper);
        root.setBottom(chatbotContainer);

        Scene scene = new Scene(root, 800, 500);
        applyCss(scene);
        tutorialStage.setScene(scene);
        tutorialStage.show();
    }

    private void applyCss(Scene scene) {
        URL css = getClass().getResource("/styles/selection.css");
        if (css == null) {
            throw new IllegalStateException("Could not load /styles/selection.css");
        }
        scene.getStylesheets().add(css.toExternalForm());

        URL landingCss = getClass().getResource("/styles/landingpage.css");
        if (landingCss != null) {
            scene.getStylesheets().add(landingCss.toExternalForm());
        }
    }

    private static class ExerciseItem {
        private final String name;
        private final String description;
        private final String imagePath;
        private final String category;

        private ExerciseItem(String name, String description, String imagePath, String category) {
            this.name = name;
            this.description = description;
            this.imagePath = imagePath;
            this.category = category;
        }
    }
}


