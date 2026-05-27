package com.formcoach;

import com.formcoach.auth.AuthPage;
import com.formcoach.profile.ProfilePage;
import com.formcoach.chatbot.chatbot;
import com.formcoach.landingpage.landingpage;
import com.formcoach.posedetection.posedetection;
import com.formcoach.runexercise.runexercise;
import com.formcoach.selection.ExerciseSelectionPage;
import com.formcoach.videomodal.ExerciseVideoView;
import com.formcoach.auth.DatabaseAuthService;
import com.formcoach.historypage.HistoryPage;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

/**
 * One navigation primitive for the whole app. Each show... method swaps
 * the stage's scene to a different page. Teammate-owned pages
 * (landingpage, AuthPage, ExerciseSelectionPage) stay untouched — I wire
 * their buttons from out here by walking the scene graph after the page
 * is shown. AuthPage / ExerciseSelectionPage already expose callbacks
 * via their constructors, so those wires go in through the constructor.
 */
public final class Navigator {

    private final Stage stage;

    /**
     * Constructs a Navigator bound to the given stage.
     * @param stage the primary application stage all pages will be shown on
     */
    public Navigator(Stage stage) {
        this.stage = stage;
    }

    // ── page openers ─────────────────────────────────────────────────────────

    /** App entry point — opens the marketing / hero page. */
    public void showLanding() {
        landingpage root = new landingpage();
        Scene scene = new Scene(root, 1280, 800);
        stage.setScene(scene);
        stage.setTitle("FormCoach - AI Powered Form Coaching");
        stage.show();
        wireLandingButtons(root);
    }

    /** Login / register card. Successful auth -> selection. Back -> landing. */
    public void showAuth() {
        new AuthPage(stage, this::showLanding, this::showSelection, new DatabaseAuthService(), this::showHistory, this::showProfile).show();
    }

    /** Paginated exercise picker. After show(), wire up all of its buttons. */
    public void showSelection() {
        ExerciseSelectionPage page = new ExerciseSelectionPage(
                stage,
                this::showLanding,
                this::showProfile,
                this::showHistory
        );
        page.show();
        wireSelectionScene(stage.getScene());
    }


    /** Opens the user profile page. */
    public void showProfile() {
        new ProfilePage(
                stage,
                this::showLanding,
                this::showSelection,
                this::showAuth,
                this::showHistory
        ).show();
    }

    /** Opens the workout history page. */
    public void showHistory() {
        new HistoryPage(stage).show();
    }

    /** AI coach chat popup. */
    public void showChatbot() {
        chatbot.showChatbot(stage);
    }

    /**
     * Exercise demo preview — plays a short video clip for the picked
     * exercise. ✕ / Back go to selection; "Start Live Session →" goes
     * through to the live pose screen.
     */
    /**
     * Opens the tutorial video preview for the given exercise.
     * @param exerciseName the display name of the exercise (e.g. {@code "Push-ups"})
     */
    public void showVideoPreview(String exerciseName) {
        ExerciseVideoView view = new ExerciseVideoView(
                exerciseName,
                this::showSelection,
                () -> showRunexercise(exerciseName));
        Scene scene = new Scene(view, 1280, 800);
        stage.setScene(scene);
        stage.setTitle("FormCoach - " + exerciseName);
        stage.show();
    }

    /**
     * Opens the live exercise session for the given exercise.
     * @param exerciseName the display name of the exercise (e.g. {@code "Push-ups"})
     */
    public void showRunexercise(String exerciseName) {
        new runexercise(stage, exerciseName, this::showSelection).show();
    }

    /** Standalone pose tool. Back -> landing. */
    public void showPosedetection() {
        new posedetection(stage, this::showLanding).show();
    }

    // ── landingpage wiring ───────────────────────────────────────────────────
    //
    // landingpage's buttons are local variables inside its constructor, so
    // I can't reach them by field. I walk the scene graph and match by the
    // button's display text. Cheap and stable.

    private void wireLandingButtons(Parent root) {
        for (Button b : findAllButtons(root)) {
            String text = b.getText() == null ? "" : b.getText().trim();
            switch (text) {
                case "Home"                 -> b.setOnAction(e -> showLanding());
                case "Exercises"            -> b.setOnAction(e -> showSelection());
                case "History"              -> b.setOnAction(e -> showHistory());
                case "Profile"              -> b.setOnAction(e -> showProfile());
                case "Browse Exercises  →"  -> b.setOnAction(e -> showSelection());
                case "View Progress"        -> b.setOnAction(e -> showHistory());
                case "💬"                   -> b.setOnAction(e -> showChatbot());
                default -> { /* leave unrecognised buttons alone */ }
            }
        }
    }

    // ── selection wiring ─────────────────────────────────────────────────────

    /**
     * Called right after ExerciseSelectionPage.show(). Walks the live scene
     * graph and replaces the placeholder handlers on Profile / History /
     * exercise Select buttons / chatbot widget.
     *
     * Also attaches a listener to the card container so that when the user
     * pages forward/back and the cards get rebuilt, the Select buttons on
     * the new cards get re-wired.
     */
    private void wireSelectionScene(Scene scene) {
        if (scene == null) return;
        Parent root = scene.getRoot();

        // Nav buttons + other top-level buttons.
        for (Button b : findAllButtons(root)) {
            String text = b.getText() == null ? "" : b.getText().trim();
            switch (text) {
                case "Profile"  -> b.setOnAction(e -> showProfile());
                case "History"  -> b.setOnAction(e -> showHistory());
                // Home is already wired via onBack.
                // Exercises is the current page; leave it.
                default -> { /* pass */ }
            }
            if ("Select".equals(text)) {
                rewireSelectButton(b);
            }
        }

        // Chatbot widget is a StackPane, not a Button — find and hook it.
        for (Node n : findAllByClass(root, StackPane.class)) {
            if (containsChatEmoji(n)) {
                n.setOnMouseClicked(e -> showChatbot());
            }
        }

        // Card container: rebuild Select wiring whenever pagination swaps cards.
        HBox cardContainer = findCardContainer(root);
        if (cardContainer != null) {
            cardContainer.getChildren().addListener(
                    (ListChangeListener<Node>) change -> {
                        // On any change, re-walk just this container.
                        for (Button b : findAllButtons(cardContainer)) {
                            if ("Select".equals(b.getText())) {
                                rewireSelectButton(b);
                            }
                        }
                    });
        }
    }

    /**
     * Replaces a Select button's action with one that navigates to
     * runexercise, using the card's title label as the exercise name.
     * A Select button lives inside a VBox alongside the title Label, so
     * I climb to the parent and look for the first Label.
     */
    private void rewireSelectButton(Button selectBtn) {
        String cardTitle = findCardTitle(selectBtn);
        if (cardTitle == null) cardTitle = "Exercise";
        final String title = cardTitle;
        // Select goes to the video demo first; the demo's "Start Live
        // Session" button then advances to runexercise.
        selectBtn.setOnAction(e -> showVideoPreview(title));
    }

    /** Walk up from the Select button to the enclosing card and grab its title. */
    private static String findCardTitle(Button selectBtn) {
        Parent p = selectBtn.getParent();
        while (p != null) {
            if (p instanceof VBox vbox) {
                for (Node child : vbox.getChildren()) {
                    if (child instanceof Label l && l.getText() != null && !l.getText().isBlank()
                            && !"Select".equalsIgnoreCase(l.getText())) {
                        return l.getText();
                    }
                }
            }
            p = p.getParent();
        }
        return null;
    }

    private static HBox findCardContainer(Parent root) {
        // The card container is the HBox that directly holds the card VBoxes.
        // Match by: an HBox whose children are mostly VBoxes (the cards).
        for (Node n : findAllByClass(root, HBox.class)) {
            if (n instanceof HBox hb) {
                long vboxChildren = hb.getChildrenUnmodifiable().stream()
                        .filter(c -> c instanceof VBox)
                        .count();
                if (vboxChildren >= 1 && vboxChildren == hb.getChildrenUnmodifiable().size()) {
                    return hb;
                }
            }
        }
        return null;
    }

    // ── scene-graph helpers ──────────────────────────────────────────────────

    private static List<Button> findAllButtons(Parent root) {
        List<Button> out = new ArrayList<>();
        collectByType(root, Button.class, out);
        return out;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Node> List<T> findAllByClass(Parent root, Class<T> cls) {
        List<T> out = new ArrayList<>();
        collectByType(root, cls, out);
        return out;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Node> void collectByType(Node node, Class<T> cls, List<T> out) {
        if (cls.isInstance(node)) {
            out.add((T) node);
        }
        if (node instanceof Parent p) {
            ObservableList<Node> kids = p.getChildrenUnmodifiable();
            for (Node child : kids) {
                collectByType(child, cls, out);
            }
        }
    }

    /** True if the node's subtree contains a Label with a "💬" glyph. */
    private static boolean containsChatEmoji(Node node) {
        if (node instanceof Label l && l.getText() != null && l.getText().contains("💬")) {
            return true;
        }
        if (node instanceof Parent p) {
            for (Node child : p.getChildrenUnmodifiable()) {
                if (containsChatEmoji(child)) return true;
            }
        }
        return false;
    }
}
