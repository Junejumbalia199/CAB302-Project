package com.formcoach.selection;

import com.formcoach.Main;
import com.formcoach.landingpage.landingpage;
import com.formcoach.videomodal.ExerciseVideoView;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import kaggle.ExerciseDataset;

import java.util.List;

/**
 * My exercise picker. I try to use the real Kaggle dataset first (via
 * Main.exercises()). If that hasn't loaded yet — maybe no kaggle.json on
 * disk, maybe I'm offline — I fall back to a handful of hardcoded entries
 * so the page is never empty. I didn't want the "offline" case to look
 * broken; the user gets something either way.
 */
public class ExerciseSelectionPage extends StackPane {

    // My offline safety net. If Kaggle's unreachable or creds aren't set
    // up yet I still want the page to look populated.
    private static final String[][] FALLBACK = {
            {"Pushups",    "Upper Body", "Intermediate"},
            {"Squats",     "Legs",       "Beginner"},
            {"Pull Ups",   "Upper Body", "Advanced"},
            {"Lunges",     "Legs",       "Beginner"},
            {"Plank",      "Core",       "Beginner"},
            {"Deadlifts",  "Full Body",  "Advanced"},
    };

    // Cap at 20 for now. The Kaggle CSV has thousands of rows and the grid
    // chokes if I try to render them all. I'll add paging/filtering later.
    private static final int MAX_ROWS = 20;

    public ExerciseSelectionPage() {
        getStylesheets().add(getClass().getResource("/styles/styles.css").toExternalForm());

        VBox root = new VBox(20);
        root.setPadding(new Insets(36));

        // Header row — title on the left, back button on the right.
        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Choose an Exercise");
        title.setStyle("-fx-font-size: 28px; -fx-font-weight: 800;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button back = new Button("← Back");
        back.getStyleClass().add("btn-secondary");
        back.setOnAction(e -> swap(new landingpage()));

        header.getChildren().addAll(title, spacer, back);

        // 3 cards per row. I tried 4 first but the cards felt too narrow.
        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(16);

        int col = 0, row = 0;
        for (String[] entry : loadEntries()) {
            grid.add(buildCard(entry[0], entry[1], entry[2]), col, row);
            if (++col == 3) { col = 0; row++; }
        }

        ScrollPane scroll = new ScrollPane(grid);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        root.getChildren().addAll(header, scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        getChildren().add(root);
    }

    // ── data ──────────────────────────────────────────────────────────────────

    /** Grab Kaggle rows if I have them, otherwise hand back my fallback list. */
    private List<String[]> loadEntries() {
        List<ExerciseDataset> rows = Main.exercises().all();
        if (rows.isEmpty()) {
            return java.util.Arrays.asList(FALLBACK);
        }
        return rows.stream()
                .limit(MAX_ROWS)
                .map(r -> new String[] {
                        r.title(),
                        blankToDash(r.bodyPart()),
                        blankToDash(r.level())
                })
                .toList();
    }

    private static String blankToDash(String s) {
        return (s == null || s.isBlank()) ? "—" : s;
    }

    // ── card UI ───────────────────────────────────────────────────────────────

    private VBox buildCard(String name, String bodyPart, String level) {
        VBox c = new VBox(8);
        c.getStyleClass().add("card");
        c.setPadding(new Insets(18));
        c.setPrefWidth(260);

        Label n = new Label(name);
        n.setStyle("-fx-font-size: 16px; -fx-font-weight: 700;");

        Label meta = new Label(bodyPart + " · " + level);
        meta.getStyleClass().add("subtext");

        Button go = new Button("Start");
        go.getStyleClass().add("btn-primary");
        // The flow I want: pick an exercise, watch the demo, close it out
        // and move into the live camera session. The video view handles
        // the last hop to posedetection itself.
        go.setOnAction(e -> swap(new ExerciseVideoView(name)));

        c.getChildren().addAll(n, meta, go);
        return c;
    }

    private void swap(javafx.scene.Parent next) {
        Scene scene = getScene();
        if (scene != null) scene.setRoot(next);
    }
}
