package com.formcoach.chatbot;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Chat with the AI coach. This is a placeholder — the "Send" button
 * just drops a canned coach-style reply into the transcript so the
 * flow is clickable end-to-end. When there's a real LLM wired up,
 * swap the body of {@link #reply(String)} out.
 */
public class chatbot {

    private final Stage stage;
    private final Runnable onBack;

    private final TextArea  transcript = new TextArea();
    private final TextField input      = new TextField();

    public chatbot(Stage stage, Runnable onBack) {
        this.stage  = stage;
        this.onBack = onBack;
    }

    public void show() {
        VBox root = new VBox(16);
        root.setPadding(new Insets(32));
        root.setStyle("-fx-background-color: linear-gradient(to bottom right, #f5f7fa, #e9eef5);");

        // Header: title + back.
        Label title = new Label("AI Coach");
        title.setStyle("-fx-font-size: 26px; -fx-font-weight: 800;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button back = new Button("← Back");
        styleSecondary(back);
        back.setOnAction(e -> { if (onBack != null) onBack.run(); });

        HBox header = new HBox(16, title, spacer, back);
        header.setAlignment(Pos.CENTER_LEFT);

        // Transcript — read-only, fills available space.
        transcript.setEditable(false);
        transcript.setWrapText(true);
        transcript.setText("Coach: Hey! Ask me anything about your form.\n");
        VBox.setVgrow(transcript, Priority.ALWAYS);

        // Input row: text field + Send.
        input.setPromptText("Type a message…");
        HBox.setHgrow(input, Priority.ALWAYS);

        Button send = new Button("Send");
        stylePrimary(send);
        send.setDefaultButton(true);
        send.setOnAction(e -> onSend());

        HBox inputRow = new HBox(10, input, send);

        root.getChildren().addAll(header, transcript, inputRow);

        Scene scene = new Scene(root, 1280, 760);
        stage.setScene(scene);
        stage.setTitle("FormCoach - AI Coach");
        stage.show();
    }

    private void onSend() {
        String msg = input.getText();
        if (msg == null || msg.isBlank()) return;
        transcript.appendText("\nYou:    " + msg + "\n");
        transcript.appendText("Coach: " + reply(msg) + "\n");
        input.clear();
    }

    /** Keyword-matching stand-in until there's a real model behind it. */
    private String reply(String userMessage) {
        String lower = userMessage.toLowerCase();
        if (lower.contains("pushup") || lower.contains("push-up"))
            return "Keep your core tight and your elbows at ~45° from your torso.";
        if (lower.contains("squat"))
            return "Knees track over your toes, chest up, sit back into your heels.";
        if (lower.contains("plank"))
            return "Straight line from shoulders to heels, don't let your hips sag.";
        return "Good question! I'll have a smarter answer once I'm wired to a model.";
    }

    private static void stylePrimary(Button b) {
        b.setStyle("-fx-background-color: #4a90e2; -fx-text-fill: white;"
                + "-fx-padding: 10 20; -fx-background-radius: 8; -fx-cursor: hand;");
    }

    private static void styleSecondary(Button b) {
        b.setStyle("-fx-background-color: white; -fx-text-fill: #1f2937;"
                + "-fx-border-color: #e5e7eb; -fx-border-radius: 8;"
                + "-fx-padding: 10 20; -fx-background-radius: 8; -fx-cursor: hand;");
    }
}
