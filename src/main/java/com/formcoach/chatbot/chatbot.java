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
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.Window;
import io.github.cdimascio.dotenv.Dotenv;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.json.JSONObject;
import org.json.JSONArray;

public class chatbot {

    private static final Dotenv dotenv = Dotenv.load();
    private static final String API_KEY = (dotenv.get("API_KEY") != null) ? dotenv.get("API_KEY").trim() : "";
    private static final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + API_KEY;

    private static Popup chatPopup;
    private static VBox messageContainer;
    private static TextField input;
    private static ScrollPane scrollPane;

    public static void showChatbot(Window owner) {
        if (chatPopup == null) {
            chatPopup = new Popup();

            // Root VBox
            VBox root = new VBox();
            root.getStyleClass().add("chat-root");
            root.setPrefWidth(350);

            // Load External CSS
            root.getStylesheets().add(chatbot.class.getResource("/styles/chatbot.css").toExternalForm());

            // --- HEADER ---
            VBox headerText = new VBox(2);
            Label title = new Label("FormCoach AI");
            title.getStyleClass().add("chat-title");
            Label subTitle = new Label("Ask me anything about fitness");
            subTitle.getStyleClass().add("chat-subtitle");
            headerText.getChildren().addAll(title, subTitle);

            Button closeBtn = new Button("✕");
            closeBtn.getStyleClass().add("close-button");
            closeBtn.setOnAction(e -> hideChatbot());

            HBox header = new HBox();
            header.getStyleClass().add("chat-header");
            header.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(headerText, Priority.ALWAYS);
            header.getChildren().addAll(headerText, closeBtn);

            // --- MESSAGE AREA ---
            messageContainer = new VBox(12);
            messageContainer.setPadding(new Insets(15));

            scrollPane = new ScrollPane(messageContainer);
            scrollPane.getStyleClass().add("chat-scroll-pane");
            scrollPane.setFitToWidth(true);
            scrollPane.setPrefHeight(400);
            VBox.setVgrow(scrollPane, Priority.ALWAYS);

            addCoachMessage("Hi! I'm your FormCoach assistant. Ask me anything about exercises, proper form, workout routines, or injury prevention.");

            // --- INPUT AREA ---
            input = new TextField();
            input.setPromptText("Ask about form, exercises...");
            input.getStyleClass().add("chat-input");
            HBox.setHgrow(input, Priority.ALWAYS);

            // Replicating the circle 'X' or Send feel from the image
            Button sendBtn = new Button("✕");
            sendBtn.getStyleClass().add("close-button");
            sendBtn.setOnAction(e -> onSend());

            input.setOnAction(e -> onSend());

            HBox inputRow = new HBox(10, input, sendBtn);
            inputRow.getStyleClass().add("input-container");
            inputRow.setAlignment(Pos.CENTER);

            root.getChildren().addAll(header, scrollPane, inputRow);
            chatPopup.getContent().add(root);
        }

        if (owner != null) {
            chatPopup.setX(owner.getX() + owner.getWidth() - 380);
            chatPopup.setY(owner.getY() + owner.getHeight() - 620);
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

        Thread thread = new Thread(() -> {
            String aiResponse = getGeminiResponse(msg);
            Platform.runLater(() -> addCoachMessage(aiResponse));
        });
        thread.setDaemon(true);
        thread.start();
    }

    private static void addUserMessage(String text) {
        Label message = new Label(text);
        message.setWrapText(true);
        message.getStyleClass().add("user-text");

        VBox bubble = new VBox(message);
        bubble.getStyleClass().add("user-bubble");
        bubble.setMaxWidth(260);

        HBox row = new HBox(bubble);
        row.setAlignment(Pos.CENTER_RIGHT);
        messageContainer.getChildren().add(row);
        scrollToBottom();
    }

    private static void addCoachMessage(String text) {
        Label message = new Label(text);
        message.setWrapText(true);
        message.getStyleClass().add("coach-text");

        VBox bubble = new VBox(message);
        bubble.getStyleClass().add("coach-bubble");
        bubble.setMaxWidth(260);

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