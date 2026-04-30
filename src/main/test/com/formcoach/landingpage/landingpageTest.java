package com.formcoach.landingpage;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class landingpageTest {

    @Test
    void shouldConstructLandingPageWithoutThrowing() {
        assertDoesNotThrow(() -> new landingpage());
    }

    @Test
    void shouldReturnNonNullInstance() {
        landingpage page = new landingpage();
        assertNotNull(page);
    }

    @Test
    void shouldBeInstanceOfStackPane() {
        landingpage page = new landingpage();
        assertTrue(page instanceof javafx.scene.layout.StackPane);
    }

    @Test
    void shouldHaveChildrenListInitialized() {
        landingpage page = new landingpage();
        assertNotNull(page.getChildren());
    }

    @Test
    void shouldAllowMultipleInstancesCreation() {
        landingpage page1 = new landingpage();
        landingpage page2 = new landingpage();

        assertNotSame(page1, page2);
    }
}