package com.formcoach.chatbot;

import com.formcoach.landingpage.landingpage;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;

/**
 * My chat screen. It doesn't talk to a real LLM yet — I wanted the flow
 * wired first. "Send" just drops a canned coach-style reply into the
 * transcript so clicking around feels alive. When I have an API key to
 * hit, I'll swap out {@link #reply(String)} and everything else should
 * keep working.
 */
public class chatbot extends StackPane {

    private final TextArea  transcript = new TextArea();
    private final TextField input      = new TextField();

    public chatbot() {
        getStylesheets().add(getClass().getResource("/styles/styles.css").toExternalForm());

        VBox root = new VBox(16);
        root.setPadding(new Insets(36));

        // Header row.
        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("AI Coach");
        title.setStyle("-fx-font-size: 26px; -fx-font-weight: 800;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button back = new Button("← Back");
        back.getStyleClass().add("btn-secondary");
        back.setOnAction(e -> swap(new landingpage()));

        header.getChildren().addAll(title, spacer, back);

        // Scrollable transcript. Read-only so the user can't accidentally
        // edit what's been said, and it grows to fill the space.
        transcript.setEditable(false);
        transcript.setWrapText(true);
        transcript.setText("Coach: Hey! Ask me anything about your form.\n");
        VBox.setVgrow(transcript, Priority.ALWAYS);

        // Input row — text box + Send button.
        input.setPromptText("Type a message…");
        HBox.setHgrow(input, Priority.ALWAYS);

        Button send = new Button("Send");
        send.getStyleClass().add("btn-primary");
        send.setDefaultButton(true);   // Hitting Enter fires Send, which feels right.
        send.setOnAction(e -> onSend());

        HBox inputRow = new HBox(10, input, send);

        root.getChildren().addAll(header, transcript, inputRow);
        getChildren().add(root);
    }

    private void onSend() {
        String msg = input.getText();
        if (msg == null || msg.isBlank()) return;

        transcript.appendText("\nYou:    " + msg + "\n");
        transcript.appendText("Coach: " + reply(msg) + "\n");
        input.clear();
    }

    /** Dumb keyword-matching stand-in. Good enough until I hook up a model. */
    private String reply(String userMessage) {
        String lower = userMessage.toLowerCase();
        if (lower.contains("pushup") || lower.contains("push-up")) {
            return "Keep your core tight and your elbows at ~45° from your torso.";
        }
        if (lower.contains("squat")) {
            return "Knees track over your toes, chest up, sit back into your heels.";
        }
        return "Good question! I'll have a smarter answer once I'm wired to a model.";
    }

    private void swap(javafx.scene.Parent next) {
        Scene scene = getScene();
        if (scene != null) scene.setRoot(next);
    }
}
