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
 */
public class chatbot {

    private static final Dotenv dotenv = Dotenv.load();
    // .trim() is vital here to ensure no hidden \n or spaces from the .env break the URL
    private static final String API_KEY = (dotenv.get("API_KEY") != null) ? dotenv.get("API_KEY").trim() : "";

    // Using v1beta and the standard gemini-1.5-flash name
    private static final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent?key=" + API_KEY;

    private static Popup chatPopup;
    private static VBox messageContainer;
    private static TextField input;
    private static ScrollPane scrollPane;

    public static void showChatbot(Window owner) {
        if (chatPopup == null) {
            chatPopup = new Popup();

            Label title = new Label("💡 AI Coach");
            title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Button closeBtn = new Button("✕");
            closeBtn.setStyle("-fx-background-color: #e2e8f0; -fx-text-fill: #64748b; -fx-font-size: 16px; -fx-font-weight: bold; -fx-padding: 4 8; -fx-background-radius: 50; -fx-cursor: hand;");
            closeBtn.setOnAction(e -> hideChatbot());

            HBox header = new HBox(8, title, spacer, closeBtn);
            header.setAlignment(Pos.CENTER_LEFT);
            header.setPadding(new Insets(0, 0, 8, 0));
            header.setStyle("-fx-border-color: #e2e8f0; -fx-border-width: 0 0 1 0;");

            VBox root = new VBox(12);
            root.setPadding(new Insets(16));
            root.setStyle("-fx-background-color: #ffffff; -fx-border-color: #e2e8f0; -fx-border-width: 1; -fx-background-radius: 12; -fx-border-radius: 12; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.15), 15, 0, 0, 5);");

            messageContainer = new VBox(8);
            messageContainer.setPadding(new Insets(8));

            scrollPane = new ScrollPane(messageContainer);
            scrollPane.setFitToWidth(true);
            scrollPane.setStyle("-fx-control-inner-background: #f8fafc; -fx-background-color: #f8fafc;");
            scrollPane.setPrefHeight(350);

            addCoachMessage("Hey! 👋 Ask me anything about your form.");

            VBox.setVgrow(scrollPane, Priority.ALWAYS);

            input = new TextField();
            input.setPromptText("Ask about your form...");
            HBox.setHgrow(input, Priority.ALWAYS);

            Button send = new Button("Send");
            send.setStyle("-fx-background-color: #0ea5e9; -fx-text-fill: white; -fx-font-weight: bold;");
            send.setOnAction(e -> onSend());

            HBox inputRow = new HBox(8, input, send);
            inputRow.setPadding(new Insets(8, 0, 0, 0));

            root.getChildren().addAll(header, scrollPane, inputRow);
            chatPopup.getContent().add(root);
        }

        if (owner != null) {
            chatPopup.setX(owner.getX() + owner.getWidth() - 420);
            chatPopup.setY(owner.getY() + owner.getHeight() - 570);
        }
        chatPopup.show(owner);
    }

    public static void hideChatbot() {
        if (chatPopup != null) chatPopup.hide();
    }

    private static void onSend() {
        String msg = input.getText();
        if (msg == null || msg.isBlank()) return;

        addUserMessage(msg);
        input.clear();
        addCoachMessage("Coach is thinking...");

        Thread thread = new Thread(() -> {
            String aiResponse = getGeminiResponse(msg);
            Platform.runLater(() -> {
                if (messageContainer.getChildren().size() > 0) {
                    messageContainer.getChildren().remove(messageContainer.getChildren().size() - 1);
                }
                addCoachMessage(aiResponse);
            });
        });
        thread.setDaemon(true);
        thread.start();
    }

    private static void addUserMessage(String text) {
        Label message = new Label(text);
        message.setWrapText(true);
        message.setStyle("-fx-text-fill: white; -fx-padding: 10;");
        VBox bubble = new VBox(message);
        bubble.setStyle("-fx-background-color: #0ea5e9; -fx-background-radius: 12;");
        bubble.setMaxWidth(250);
        HBox row = new HBox(bubble);
        row.setAlignment(Pos.CENTER_RIGHT);
        messageContainer.getChildren().add(row);
        scrollToBottom();
    }

    private static void addCoachMessage(String text) {
        Label message = new Label(text);
        message.setWrapText(true);
        message.setStyle("-fx-text-fill: #1e293b; -fx-padding: 10;");
        VBox bubble = new VBox(message);
        bubble.setStyle("-fx-background-color: #e2e8f0; -fx-background-radius: 12;");
        bubble.setMaxWidth(250);
        HBox row = new HBox(bubble);
        row.setAlignment(Pos.CENTER_LEFT);
        messageContainer.getChildren().add(row);
        scrollToBottom();
    }

    private static void scrollToBottom() {
        Platform.runLater(() -> { if (scrollPane != null) scrollPane.setVvalue(1.0); });
    }

    private static String getGeminiResponse(String userMessage) {
        try {
            if (API_KEY.isEmpty()) return "Error: No API Key found in .env";

            System.out.println("DEBUG: Requesting URL (key hidden): " + GEMINI_URL.replace(API_KEY, "ACTUAL_KEY"));

            HttpClient client = HttpClient.newHttpClient();

            // system pre prompt for better fitness advice and context, along with the user question.
            JSONObject textPart = new JSONObject();
            textPart.put("text", "You are 'Coach', an expert fitness and exercise professional. " +
                    "Your goal is to provide safe, actionable, and encouraging advice on workout form, " +
                    "routines, and physical health. If a user asks something dangerous, advise them to " +
                    "consult a professional. Keep responses concise and use fitness emojis. " +
                    "\n\nUser Question: " + userMessage);

            JSONArray partsArray = new JSONArray();
            partsArray.put(textPart);

            JSONObject contentObject = new JSONObject();
            contentObject.put("parts", partsArray);

            JSONArray contentsArray = new JSONArray();
            contentsArray.put(contentObject);

            JSONObject root = new JSONObject();
            root.put("contents", contentsArray);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GEMINI_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(root.toString()))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JSONObject responseJson = new JSONObject(response.body());
                return responseJson.getJSONArray("candidates")
                        .getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text");
            } else {
                System.err.println("API Error Response: " + response.body());
                return "Coach is currently offline (Status " + response.statusCode() + ")";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "Connection Error: " + e.getMessage();
        }
    }
}