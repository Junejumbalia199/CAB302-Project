package com.formcoach.home;

import com.formcoach.chatbot.chatbot;
import com.formcoach.landingpage.landingpage;
import com.formcoach.selection.ExerciseSelectionPage;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;

/**
 * My home / dashboard view. It's a greeting and a couple of quick-action
 * cards for the main things I want a logged-in user to do — browse
 * exercises or jump into a chat. I kept it simple on purpose; the real
 * work happens in the other views.
 */
public class home extends StackPane {

    public home() {
        getStylesheets().add(getClass().getResource("/styles/styles.css").toExternalForm());

        VBox root = new VBox(24);
        root.setPadding(new Insets(40));
        root.setAlignment(Pos.TOP_LEFT);

        // Top row: page title on the left, back-to-landing button on the right.
        HBox topBar = new HBox(16);
        topBar.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Home");
        title.getStyleClass().add("section-title");
        title.setStyle("-fx-font-size: 32px; -fx-font-weight: 800;");

        Region topSpacer = new Region();
        HBox.setHgrow(topSpacer, Priority.ALWAYS);

        Button back = new Button("← Back");
        back.getStyleClass().add("btn-secondary");
        back.setOnAction(e -> swap(new landingpage()));

        topBar.getChildren().addAll(title, topSpacer, back);

        Label welcome = new Label("Welcome back — ready to train?");
        welcome.getStyleClass().add("subtext");

        // Two cards side by side — the two flows I expect people to hit
        // most often. I can slot more in here later.
        HBox actions = new HBox(16);
        actions.getChildren().addAll(
                card("Browse Exercises", "Pick a move and check your form.",
                        "Start",     () -> swap(new ExerciseSelectionPage())),
                card("Chat with Coach", "Ask the AI coach anything.",
                        "Open chat", () -> swap(new chatbot()))
        );

        root.getChildren().addAll(topBar, welcome, actions);
        getChildren().add(root);
    }

    private VBox card(String heading, String body, String buttonText, Runnable onClick) {
        VBox c = new VBox(10);
        c.getStyleClass().add("card");
        c.setPadding(new Insets(22));
        c.setPrefWidth(320);

        Label h = new Label(heading);
        h.setStyle("-fx-font-size: 18px; -fx-font-weight: 700;");

        Label b = new Label(body);
        b.getStyleClass().add("subtext");
        b.setWrapText(true);

        Button btn = new Button(buttonText);
        btn.getStyleClass().add("btn-primary");
        btn.setOnAction(e -> onClick.run());

        c.getChildren().addAll(h, b, btn);
        return c;
    }

    /** Same little nav helper I've got on every view. */
    private void swap(javafx.scene.Parent next) {
        Scene scene = getScene();
        if (scene != null) scene.setRoot(next);
    }
}
