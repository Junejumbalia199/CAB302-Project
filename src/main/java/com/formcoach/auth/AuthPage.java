package com.formcoach.auth;

import com.formcoach.home.home;
import com.formcoach.landingpage.landingpage;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;

/**
 * My sign-in / register page.
 *
 * Right now neither button actually validates anything — both just drop
 * the user on the home dashboard. I did that so the click-through flow
 * works end-to-end while the AuthService interface is still just an
 * interface. As soon as someone implements it I'll swap the no-op
 * handlers for real calls.
 */
public class AuthPage extends StackPane {

    public AuthPage() {
        getStylesheets().add(getClass().getResource("/styles/styles.css").toExternalForm());

        VBox card = new VBox(14);
        card.getStyleClass().add("login-card");
        card.setPadding(new Insets(36));
        card.setMaxWidth(420);
        card.setAlignment(Pos.TOP_LEFT);

        Label title = new Label("Sign in to FormCoach");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: 800;");

        Label sub = new Label("Use any credentials for now — auth isn't wired yet.");
        sub.getStyleClass().add("subtext");
        sub.setWrapText(true);

        TextField username = new TextField();
        username.setPromptText("Username or email");

        PasswordField password = new PasswordField();
        password.setPromptText("Password");

        Button signIn = new Button("Sign In");
        signIn.getStyleClass().add("btn-primary");
        signIn.setMaxWidth(Double.MAX_VALUE);
        signIn.setDefaultButton(true);
        signIn.setOnAction(e -> swap(new home()));

        Button register = new Button("Create account");
        register.getStyleClass().add("btn-secondary");
        register.setMaxWidth(Double.MAX_VALUE);
        register.setOnAction(e -> swap(new home()));

        Button back = new Button("← Back to landing");
        back.getStyleClass().add("btn-secondary");
        back.setMaxWidth(Double.MAX_VALUE);
        back.setOnAction(e -> swap(new landingpage()));

        card.getChildren().addAll(title, sub, username, password, signIn, register, back);

        // Park the card in the middle of the page.
        StackPane.setAlignment(card, Pos.CENTER);
        setPadding(new Insets(40));
        getChildren().add(card);
    }

    private void swap(javafx.scene.Parent next) {
        Scene scene = getScene();
        if (scene != null) scene.setRoot(next);
    }
}
