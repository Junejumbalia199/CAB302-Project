package com.formcoach.landingpage;

import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.*;

class landingpageTest {

    @BeforeAll
    static void initJavaFX() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        Platform.startup(() -> {
            latch.countDown();
        });

        latch.await();
    }

    private landingpage createPageOnFXThread() throws Exception {
        final landingpage[] page = new landingpage[1];
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            page[0] = new landingpage();
            latch.countDown();
        });

        latch.await();
        return page[0];
    }

    @Test
    void shouldCreateLandingPageInstance() throws Exception {
        landingpage page = createPageOnFXThread();
        assertNotNull(page);
    }

    @Test
    void shouldContainChildNodes() throws Exception {
        landingpage page = createPageOnFXThread();
        assertTrue(page.getChildren().size() > 0);
    }

    @Test
    void shouldContainMainLayoutAndFabButton() throws Exception {
        landingpage page = createPageOnFXThread();
        assertEquals(2, page.getChildren().size());
    }

    @Test
    void shouldLoadStylesheet() throws Exception {
        landingpage page = createPageOnFXThread();
        assertNotNull(page.getStylesheets());
        assertFalse(page.getStylesheets().isEmpty());
    }

    @Test
    void shouldBeStackPaneInstance() throws Exception {
        landingpage page = createPageOnFXThread();
        assertTrue(page instanceof javafx.scene.layout.StackPane);
    }
}