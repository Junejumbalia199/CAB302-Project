package com.formcoach.landingpage;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class landingpageTest {

    @Test
    void test1() {
        // Test object creation
        landingpage page = new landingpage();
        assertNotNull(page);
    }

    @Test
    void test2() {
        // Test that main layout (VBox) exists
        landingpage page = new landingpage();

        boolean hasVBox = page.getChildren().stream()
                .anyMatch(node -> node instanceof VBox);

        assertTrue(hasVBox);
    }

    @Test
    void test3() {
        // Test that chatbot FAB button exists
        landingpage page = new landingpage();

        boolean hasFab = page.getChildren().stream()
                .flatMap(node -> ((StackPane) page).getChildren().stream())
                .filter(node -> node instanceof Button)
                .map(node -> (Button) node)
                .anyMatch(btn -> btn.getText().equals("💬"));

        assertTrue(hasFab);
    }

    @Test
    void test4() {
        // Test that navigation buttons exist
        landingpage page = new landingpage();

        boolean hasHome = findButton(page, "Home");
        boolean hasExercises = findButton(page, "Exercises");
        boolean hasHistory = findButton(page, "History");
        boolean hasProfile = findButton(page, "Profile");

        assertTrue(hasHome && hasExercises && hasHistory && hasProfile);
    }

    @Test
    void test5() {
        // Test that action buttons exist
        landingpage page = new landingpage();

        boolean hasBrowse = findButton(page, "Browse Exercises  →");
        boolean hasProgress = findButton(page, "View Progress");

        assertTrue(hasBrowse && hasProgress);
    }

    // Helper method to search buttons recursively
    private boolean findButton(Node root, String text) {
        if (root instanceof Button) {
            return ((Button) root).getText().equals(text);
        }
        if (root instanceof javafx.scene.Parent) {
            for (Node child : ((javafx.scene.Parent) root).getChildrenUnmodifiable()) {
                if (findButton(child, text)) return true;
            }
        }
        return false;
    }
}