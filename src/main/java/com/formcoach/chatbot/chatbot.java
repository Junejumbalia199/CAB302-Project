package com.formcoach.chatbot;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.Window;

// imports for gemini api handling
import io.github.cdimascio.dotenv.Dotenv;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.json.JSONObject;
import org.json.JSONArray;


/**
 * AI Coach Chatbot - displays as a modern popup overlay on the existing window.
 * Messages are displayed as chat bubbles with different styling for user vs. coach.
 */
public class chatbot {

    private static final Dotenv dotenv = Dotenv.load();
    private static final String API_KEY = dotenv.get("API_KEY");
    private static final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + API_KEY;

    private static Popup chatPopup;
    private static VBox messageContainer;
    private static TextField input;
    private static ScrollPane scrollPane;

    /**
     * Show the chatbot popup overlay positioned at the bottom right of the owner window.
     */
    public static void showChatbot(Window owner) {
        if (chatPopup == null) {
            chatPopup = new Popup();

            // Header with close button
            Label title = new Label("💡 AI Coach");
            title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Button closeBtn = new Button("✕");
            closeBtn.setStyle("-fx-background-color: #e2e8f0; -fx-text-fill: #64748b; -fx-font-size: 16px; -fx-font-weight: bold; -fx-padding: 4 8; -fx-background-radius: 50; -fx-cursor: hand; -fx-border-color: transparent;");
            closeBtn.setOnAction(e -> hideChatbot());

            HBox header = new HBox(8, title, spacer, closeBtn);
            header.setAlignment(Pos.CENTER_LEFT);
            header.setPadding(new Insets(0, 0, 8, 0));
            header.setStyle("-fx-border-color: #e2e8f0; -fx-border-width: 0 0 1 0;");

            // Main container
            VBox root = new VBox(12);
            root.setPadding(new Insets(16));
            root.setStyle("-fx-background-color: #ffffff; -fx-border-color: #e2e8f0; -fx-border-width: 1; -fx-background-radius: 12; -fx-border-radius: 12; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.15), 15, 0, 0, 5);");

            // Scrollable message area
            messageContainer = new VBox(8);
            messageContainer.setPadding(new Insets(8));
            messageContainer.setStyle("-fx-background-color: transparent;");

            scrollPane = new ScrollPane(messageContainer);
            scrollPane.setFitToWidth(true);
            scrollPane.setStyle("-fx-control-inner-background: #f8fafc; -fx-background-color: #f8fafc;");
            scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            scrollPane.setPrefHeight(350);

            // Add initial coach message
            addCoachMessage("Hey! 👋 Ask me anything about your form.");

            VBox.setVgrow(scrollPane, Priority.ALWAYS);

            // Input area
            input = new TextField();
            input.setPromptText("Ask about your form...");
            input.setStyle("-fx-padding: 10; -fx-font-size: 12px; -fx-background-radius: 6; -fx-border-radius: 6;");
            HBox.setHgrow(input, Priority.ALWAYS);

            Button send = new Button("Send");
            send.setStyle("-fx-background-color: #0ea5e9; -fx-text-fill: white; -fx-padding: 8 16; "
                    + "-fx-background-radius: 6; -fx-cursor: hand; -fx-font-weight: bold;");
            send.setOnAction(e -> onSend());

            HBox inputRow = new HBox(8, input, send);
            inputRow.setPadding(new Insets(8, 0, 0, 0));
            inputRow.setStyle("-fx-border-color: #e2e8f0; -fx-border-width: 1 0 0 0;");

            root.getChildren().addAll(header, scrollPane, inputRow);

            chatPopup.getContent().add(root);
        }

        // Position at bottom right of owner window
        if (owner != null) {
            double x = owner.getX() + owner.getWidth() - 400 - 20; // 400 is approx width
            double y = owner.getY() + owner.getHeight() - 550 - 20; // 550 is height
            chatPopup.setX(x);
            chatPopup.setY(y);
        }

        chatPopup.show(owner);
    }

    public static void hideChatbot() {
        if (chatPopup != null) {
            chatPopup.hide();
        }
    }

    private static void onSend() {
        String msg = input.getText();
        if (msg == null || msg.isBlank()) return;

        addUserMessage(msg);
        input.clear();

        // Create a "Thinking..." bubble that we can update later
        addCoachMessage("Coach is thinking...");

        // Run the API call in a background thread
        Thread thread = new Thread(() -> {
            String aiResponse = getGeminiResponse(msg);

            // Update the UI back on the JavaFX Application Thread
            Platform.runLater(() -> {
                // Remove the "Thinking..." message (the last child)
                messageContainer.getChildren().remove(messageContainer.getChildren().size() - 1);
                addCoachMessage(aiResponse);
            });
        });
        thread.setDaemon(true);
        thread.start();
    }

    private static void addUserMessage(String text) {
        Label message = new Label(text);
        message.setWrapText(true);
        message.setStyle("-fx-text-fill: white; -fx-font-size: 12px; -fx-padding: 10 12;");

        VBox bubble = new VBox(message);
        bubble.setStyle("-fx-background-color: #0ea5e9; -fx-background-radius: 12; -fx-padding: 0;");
        bubble.setMaxWidth(280);
        bubble.setPadding(new Insets(0));

        HBox row = new HBox(bubble);
        row.setAlignment(Pos.CENTER_RIGHT);
        row.setPadding(new Insets(4, 0, 4, 0));

        messageContainer.getChildren().add(row);
        scrollToBottom();
    }

    private static void addCoachMessage(String text) {
        Label message = new Label(text);
        message.setWrapText(true);
        message.setStyle("-fx-text-fill: #1e293b; -fx-font-size: 12px; -fx-padding: 10 12;");

        VBox bubble = new VBox(message);
        bubble.setStyle("-fx-background-color: #e2e8f0; -fx-background-radius: 12; -fx-padding: 0;");
        bubble.setMaxWidth(280);
        bubble.setPadding(new Insets(0));

        HBox row = new HBox(bubble);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(4, 0, 4, 0));

        messageContainer.getChildren().add(row);
        scrollToBottom();
    }

    private static void scrollToBottom() {
        Platform.runLater(() -> {
            if (scrollPane != null) {
                scrollPane.setVvalue(1.0);
            }
        });
    }

    private static String getGeminiResponse(String userMessage) {
        try {
            HttpClient client = HttpClient.newHttpClient();

            // System instructions to keep the bot on track
            String systemInstruction = "You are a professional fitness coach. " +
                    "Only provide advice related to exercise, form, and fitness.";

            // Construct the JSON payload
            JSONObject json = new JSONObject();
            JSONArray contents = new JSONArray();
            JSONObject parts = new JSONObject();
            parts.put("text", systemInstruction + "\n\nUser Question: " + userMessage);
            contents.put(new JSONObject().put("parts", new JSONArray().put(parts)));
            json.put("contents", contents);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GEMINI_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // Parse the response
            JSONObject responseJson = new JSONObject(response.body());
            return responseJson.getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text");

        } catch (Exception e) {
            e.printStackTrace();
            return "Sorry, I'm having trouble connecting to my fitness brain right now. 😅";
        }
    }
}
