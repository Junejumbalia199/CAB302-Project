import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.*;
import javafx.stage.Stage;
import userdatabase.IUserDAO;
import userdatabase.SqliteUserDAO;
import userdatabase.User;

public class LoginApp extends Application {

    private Stage primaryStage;
    private IUserDAO userDAO = new SqliteUserDAO();

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        stage.setTitle("Exercise Tracker");
        stage.setResizable(false);
        showLoginScreen();
        stage.show();
    }

    private void showLoginScreen() {
        VBox root = buildRoot(buildLoginCard());
        primaryStage.setScene(new Scene(root, 480, 520));
    }

    private VBox buildLoginCard() {

        Label title = new Label("LOG IN");
        title.setStyle(
                "-fx-font-size: 22px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: #1a1a1a;"
        );

        TextField usernameField = styledTextField("Username");
        PasswordField passwordField = styledPasswordField("Password");

        Button loginBtn = primaryButton("Log In");
        loginBtn.setOnAction(e -> handleLogin(usernameField.getText(), passwordField.getText()));

        Label switchLabel = new Label("Don't have an account?");
        switchLabel.setStyle("-fx-text-fill: #888888; -fx-font-size: 13px;");
        Hyperlink signUpLink = new Hyperlink("Sign Up");
        signUpLink.setStyle("-fx-font-size: 13px; -fx-text-fill: #3a7bd5; -fx-border-color: transparent;");
        signUpLink.setOnAction(e -> showSignUpScreen());
        HBox switchRow = new HBox(4, switchLabel, signUpLink);
        switchRow.setAlignment(Pos.CENTER);

        VBox card = new VBox(18, title, usernameField, passwordField, loginBtn, switchRow);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(36, 40, 36, 40));
        return styleCard(card);
    }

    private void handleLogin(String username, String password) {
        if (username.isEmpty() || password.isEmpty()) {
            showAlert("Please fill in all fields.");
        } else if (userDAO.validateLogin(username, password)) {
            showAlert("Login successful!");
        } else {
            showAlert("Invalid username or password.");
        }
    }

    private void showSignUpScreen() {
        VBox root = buildRoot(buildSignUpCard());
        primaryStage.setScene(new Scene(root, 480, 580));
    }

    private VBox buildSignUpCard() {

        Label title = new Label("SIGN UP");
        title.setStyle(
                "-fx-font-size: 22px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: #1a1a1a;"
        );

        TextField usernameField    = styledTextField("Username");
        TextField emailField       = styledTextField("Email");
        PasswordField passwordField  = styledPasswordField("Password");
        PasswordField confirmField   = styledPasswordField("Confirm Password");

        Button signUpBtn = primaryButton("Create Account");
        signUpBtn.setOnAction(e -> handleSignUp(
                usernameField.getText(),
                emailField.getText(),
                passwordField.getText(),
                confirmField.getText()
        ));

        Label switchLabel = new Label("Already have an account?");
        switchLabel.setStyle("-fx-text-fill: #888888; -fx-font-size: 13px;");
        Hyperlink loginLink = new Hyperlink("Log In");
        loginLink.setStyle("-fx-font-size: 13px; -fx-text-fill: #3a7bd5; -fx-border-color: transparent;");
        loginLink.setOnAction(e -> showLoginScreen());
        HBox switchRow = new HBox(4, switchLabel, loginLink);
        switchRow.setAlignment(Pos.CENTER);

        VBox card = new VBox(16, title, usernameField, emailField, passwordField, confirmField, signUpBtn, switchRow);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(36, 40, 36, 40));
        return styleCard(card);
    }

    private void handleSignUp(String username, String email, String password, String confirm) {
        if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
            showAlert("Please fill in all fields.");
        } else if (!password.equals(confirm)) {
            showAlert("Passwords do not match.");
        } else if (userDAO.getUserByUsername(username) != null) {
            showAlert("Username already exists.");
        } else {
            User newUser = new User(username, password, email);
            userDAO.addUser(newUser);
            showAlert("Account created! You can now log in.");
            showLoginScreen();
        }
    }

    private VBox buildRoot(VBox card) {
        Label appName = new Label("Exercise Tracker");
        appName.setStyle(
                "-fx-font-size: 15px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: #1a1a1a;"
        );
        HBox logoBar = new HBox(appName);
        logoBar.setAlignment(Pos.CENTER);
        logoBar.setPadding(new Insets(10, 28, 10, 28));
        logoBar.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 10;" +
                        "-fx-border-color: #1a1a1a;" +
                        "-fx-border-width: 2;" +
                        "-fx-border-radius: 10;"
        );

        VBox root = new VBox(16, logoBar, card);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(40));
        root.setStyle("-fx-background-color: #f0f0f0;");
        return root;
    }

    /**
     * Applies the card styling (white box with border, matching the wireframe).
     */
    private VBox styleCard(VBox card) {
        card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 6;" +
                        "-fx-border-color: #1a1a1a;" +
                        "-fx-border-width: 2;" +
                        "-fx-border-radius: 6;" +
                        "-fx-max-width: 380;"
        );
        return card;
    }

    private TextField styledTextField(String prompt) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.setMaxWidth(Double.MAX_VALUE);
        field.setStyle(fieldStyle());
        return field;
    }

    private PasswordField styledPasswordField(String prompt) {
        PasswordField field = new PasswordField();
        field.setPromptText(prompt);
        field.setMaxWidth(Double.MAX_VALUE);
        field.setStyle(fieldStyle());
        return field;
    }

    private Button primaryButton(String text) {
        Button btn = new Button(text);
        btn.setMaxWidth(160);
        btn.setStyle(
                "-fx-background-color: #1a1a1a;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 13px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 4;" +
                        "-fx-padding: 9 20 9 20;" +
                        "-fx-cursor: hand;"
        );
        btn.setOnMouseEntered(e -> btn.setStyle(
                "-fx-background-color: #3a7bd5;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 13px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 4;" +
                        "-fx-padding: 9 20 9 20;" +
                        "-fx-cursor: hand;"
        ));
        btn.setOnMouseExited(e -> btn.setStyle(
                "-fx-background-color: #1a1a1a;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 13px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 4;" +
                        "-fx-padding: 9 20 9 20;" +
                        "-fx-cursor: hand;"
        ));
        return btn;
    }

    private String fieldStyle() {
        return  "-fx-background-color: white;" +
                "-fx-border-color: #1a1a1a;" +
                "-fx-border-width: 1.5;" +
                "-fx-border-radius: 4;" +
                "-fx-background-radius: 4;" +
                "-fx-padding: 8 10 8 10;" +
                "-fx-font-size: 13px;";
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        alert.setHeaderText(null);
        alert.setTitle("Exercise Tracker");
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}