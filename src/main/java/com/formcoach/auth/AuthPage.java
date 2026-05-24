package com.formcoach.auth;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.net.URL;

/**
 * JavaFX screen that provides login and registration forms.
 * Switches between login and register modes in-place without a scene change.
 */
public class AuthPage {

    private final Stage stage;
    private final Runnable onBack;
    private final Runnable onAuthSuccess;
    private final Runnable onHistory;
    private final Runnable onProfile;
    private final AuthService authService;

    private boolean loginMode = true;

    private VBox authCard;

    private Label formTitle;
    private Label formSubtitle;
    private Label statusLabel;
    private Label usernameLabel;

    private TextField usernameField;
    private TextField emailField;
    private PasswordField passwordField;
    private PasswordField confirmPasswordField;

    private VBox emailBox;
    private VBox confirmPasswordBox;

    private Button submitButton;
    private Hyperlink switchLink;

    /**
     * Constructs a new AuthPage.
     * @param stage         the primary application stage
     * @param onBack        callback invoked when the user navigates back to the landing page
     * @param onAuthSuccess callback invoked after a successful login or registration
     * @param authService   the authentication service used to validate credentials
     * @param onHistory     callback invoked when the user navigates to the history page
     * @param onProfile     callback invoked when the user navigates to the profile page
     */
    public AuthPage(Stage stage, Runnable onBack, Runnable onAuthSuccess, AuthService authService, Runnable onHistory, Runnable onProfile) {
        this.stage = stage;
        this.onBack = onBack;
        this.onAuthSuccess = onAuthSuccess;
        this.authService = authService;
        this.onHistory = onHistory;
        this.onProfile = onProfile;
    }

    /** Builds and displays the authentication screen on the primary stage. */
    public void show() {
        Scene scene = createScene();
        stage.setScene(scene);
        stage.setTitle("FormCoach - Login / Sign Up");
        stage.setMinWidth(1100);
        stage.setMinHeight(720);
        stage.show();
    }

    private Scene createScene() {
        StackPane root = new StackPane();

        BorderPane layout = new BorderPane();
        layout.getStyleClass().add("root-pane");

        VBox topWrapper = new VBox(createNavigationBar());
        topWrapper.setPadding(new Insets(14, 24, 0, 24));
        layout.setTop(topWrapper);

        VBox content = new VBox(24);
        content.setAlignment(Pos.TOP_CENTER);
        content.setPadding(new Insets(24, 24, 120, 24));
        content.getChildren().addAll(
                createHeader(),
                createAuthCardWrapper()
        );

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.getStyleClass().add("auth-scroll");
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setPannable(true);

        layout.setCenter(scrollPane);

        Button backButton = createBackButton();
        StackPane.setAlignment(backButton, Pos.BOTTOM_LEFT);
        StackPane.setMargin(backButton, new Insets(0, 0, 24, 24));

        root.getChildren().addAll(layout, backButton);

        Scene scene = new Scene(root, 1280, 800);
        applyCss(scene);
        updateMode();
        return scene;
    }

    private HBox createNavigationBar() {
        HBox navBar = new HBox(40);
        navBar.getStyleClass().add("nav-bar");

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

        Button btnHome = createNavButton("Home", true, () -> { if (onBack != null) onBack.run(); });
        Button btnExercises = createNavButton("Exercises", false, () -> { if (onBack != null) onBack.run(); });
        Button btnHistory = createNavButton("History", false, () -> { if (onHistory != null) onHistory.run(); });
        Button btnProfile = createNavButton("Profile", false, () -> { if (onProfile != null) onProfile.run(); });

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
        VBox header = new VBox(6);
        header.setAlignment(Pos.CENTER);

        Label title = new Label("Welcome to FormCoach");
        title.getStyleClass().add("page-title");

        Label subtitle = new Label("Log in or create an account to continue");
        subtitle.getStyleClass().add("page-subtitle");

        header.getChildren().addAll(title, subtitle);
        return header;
    }

    private StackPane createAuthCardWrapper() {
        authCard = new VBox(16);
        authCard.getStyleClass().add("auth-card");
        authCard.setAlignment(Pos.TOP_CENTER);
        authCard.setMaxWidth(480);

        formTitle = new Label();
        formTitle.getStyleClass().add("auth-title");

        formSubtitle = new Label();
        formSubtitle.getStyleClass().add("auth-subtitle");

        VBox header = new VBox(4, formTitle, formSubtitle);
        header.setAlignment(Pos.CENTER);

        usernameField = createTextField("Enter your username or email");
        emailField = createTextField("Enter your email");
        passwordField = createPasswordField("Enter your password");
        confirmPasswordField = createPasswordField("Re-enter your password");

        VBox usernameBox = createFieldGroup("", usernameField);
        usernameLabel = (Label) usernameBox.getChildren().get(0);

        emailBox = createFieldGroup("Email", emailField);
        VBox passwordBox = createFieldGroup("Password", passwordField);
        confirmPasswordBox = createFieldGroup("Confirm Password", confirmPasswordField);

        statusLabel = new Label();
        statusLabel.setWrapText(true);
        statusLabel.setVisible(false);
        statusLabel.setManaged(false);
        statusLabel.getStyleClass().add("status-message");

        submitButton = new Button();
        submitButton.getStyleClass().add("btn-primary");
        submitButton.setPrefWidth(210);
        submitButton.setPrefHeight(48);
        submitButton.setOnAction(e -> handleSubmit());

        switchLink = new Hyperlink();
        switchLink.getStyleClass().add("auth-switch-link");
        switchLink.setBorder(Border.EMPTY);
        switchLink.setOnAction(e -> {
            loginMode = !loginMode;
            clearStatus();
            updateMode();
        });

        VBox actions = new VBox(12, submitButton, switchLink);
        actions.setAlignment(Pos.CENTER);
        actions.setPadding(new Insets(8, 0, 0, 0));

        authCard.getChildren().addAll(
                header,
                usernameBox,
                emailBox,
                passwordBox,
                confirmPasswordBox,
                statusLabel,
                actions
        );

        StackPane wrapper = new StackPane(authCard);
        wrapper.setAlignment(Pos.TOP_CENTER);
        return wrapper;
    }

    private VBox createFieldGroup(String labelText, Control field) {
        Label label = new Label(labelText);
        label.getStyleClass().add("form-label");

        field.getStyleClass().add("form-field");

        VBox box = new VBox(8, label, field);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private TextField createTextField(String prompt) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.setPrefHeight(50);
        return field;
    }

    private PasswordField createPasswordField(String prompt) {
        PasswordField field = new PasswordField();
        field.setPromptText(prompt);
        field.setPrefHeight(50);
        return field;
    }

    private Button createBackButton() {
        Button backButton = new Button("←");
        backButton.getStyleClass().add("floating-back");
        backButton.setOnAction(e -> {
            if (onBack != null) {
                onBack.run();
            }
        });
        return backButton;
    }

    private void updateMode() {
        formTitle.setText(loginMode ? "Login" : "Create Account");
        formSubtitle.setText(loginMode
                ? "Enter your account details to continue"
                : "Set up your account to get started");

        usernameLabel.setText(loginMode ? "Username or Email" : "Username");
        usernameField.setPromptText(loginMode ? "Enter your username or email" : "Enter your username");

        emailBox.setManaged(!loginMode);
        emailBox.setVisible(!loginMode);

        confirmPasswordBox.setManaged(!loginMode);
        confirmPasswordBox.setVisible(!loginMode);

        submitButton.setText(loginMode ? "Login" : "Sign Up");

        switchLink.setText(loginMode
                ? "Don't have an account? Sign up"
                : "Already have an account? Login");

        authCard.setMaxWidth(loginMode ? 480 : 540);

        usernameField.clear();
        emailField.clear();
        passwordField.clear();
        confirmPasswordField.clear();
    }

    private void handleSubmit() {
        clearStatus();

        String usernameOrEmail = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (usernameOrEmail.isEmpty()) {
            showError(loginMode
                    ? "Please enter your username or email."
                    : "Please enter your username.");
            return;
        }

        if (password.isEmpty()) {
            showError("Please enter your password.");
            return;
        }

        if (authService == null) {
            showError("Authentication backend is not connected yet.");
            return;
        }

        if (loginMode) {
            AuthResult result = authService.login(usernameOrEmail, password);

            if (result.isSuccess()) {
                if (onAuthSuccess != null) {
                    onAuthSuccess.run();
                }
            } else {
                showError(result.getMessage());
            }
            return;
        }

        if (email.isEmpty()) {
            showError("Please enter your email.");
            return;
        }

        if (!isValidEmail(email)) {
            showError("Please enter a valid email address.");
            return;
        }

        if (password.length() < 6) {
            showError("Password must be at least 6 characters long.");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showError("Passwords do not match.");
            return;
        }

        AuthResult result = authService.register(
                new UserRegistrationRequest(usernameOrEmail, email, password)
        );

        if (result.isSuccess()) {
            if (onAuthSuccess != null) {
                onAuthSuccess.run();
            }
        } else {
            showError(result.getMessage());
        }
    }

    private void showError(String message) {
        statusLabel.setText(message);
        statusLabel.getStyleClass().removeAll("status-success");
        if (!statusLabel.getStyleClass().contains("status-error")) {
            statusLabel.getStyleClass().add("status-error");
        }
        statusLabel.setManaged(true);
        statusLabel.setVisible(true);
    }

    private void clearStatus() {
        statusLabel.setText("");
        statusLabel.setManaged(false);
        statusLabel.setVisible(false);
        statusLabel.getStyleClass().removeAll("status-error", "status-success");
    }

    private boolean isValidEmail(String email) {
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    }

    private void applyCss(Scene scene) {
        URL css = getClass().getResource("/styles/auth.css");
        if (css == null) {
            throw new IllegalStateException("Could not load /styles/auth.css");
        }
        scene.getStylesheets().add(css.toExternalForm());
    }
}