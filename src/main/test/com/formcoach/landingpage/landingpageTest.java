package com.formcoach.landingpage;

import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class landingpageTest {

    @BeforeAll
    static void initJavaFX() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException e) {
            // JavaFX already started
        }
    }

    @Test
    void shouldCreateLandingPageInstance() {
        landingpage page = new landingpage();
        assertNotNull(page);
    }

    @Test
    void shouldContainChildNodes() {
        landingpage page = new landingpage();
        assertTrue(page.getChildren().size() > 0);
    }

    @Test
    void shouldContainMainLayoutAndFabButton() {
        landingpage page = new landingpage();
        assertEquals(2, page.getChildren().size()); // VBox + FAB
    }

    @Test
    void shouldLoadStylesheet() {
        landingpage page = new landingpage();
        assertNotNull(page.getStylesheets());
        assertFalse(page.getStylesheets().isEmpty());
    }

    @Test
    void shouldHaveDefaultStyleClass() {
        landingpage page = new landingpage();
        assertTrue(page.getStyleClass().isEmpty()); // StackPane default
    }
}