package com.formcoach.landingpage;

import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class landingpageTest {

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
    void shouldBeStackPaneInstance() {
        landingpage page = new landingpage();
        assertTrue(page instanceof javafx.scene.layout.StackPane);
    }
}