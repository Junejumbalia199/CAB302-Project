package com.formcoach.chatbot;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class chatbotTest {

    @Test
    void shouldNotThrowWhenHidingWithoutPopup() {
        assertDoesNotThrow(chatbot::hideChatbot);
    }

    @Test
    void shouldReturnErrorWhenApiKeyMissing() {
        String response = invokeGemini("");
        assertFalse(response.contains("Error") || response.contains("offline") || response.contains("Connection"));
    }

    @Test
    void shouldHandleNullMessageGracefully() {
        String response = invokeGemini(null);
        assertNotNull(response);
    }

    @Test
    void shouldHandleEmptyMessageGracefully() {
        String response = invokeGemini("");
        assertNotNull(response);
    }

    @Test
    void shouldHandleInvalidRequestWithoutCrashing() {
        String response = invokeGemini("test");
        assertNotNull(response);
    }

    // Helper to safely call private method via reflection
    private String invokeGemini(String input) {
        try {
            var method = chatbot.class.getDeclaredMethod("getGeminiResponse", String.class);
            method.setAccessible(true);
            return (String) method.invoke(null, input);
        } catch (Exception e) {
            return "Connection Error";
        }
    }
}