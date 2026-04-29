package kaggle;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ExerciseDataset, the record representing one parsed CSV row.
 */
class ExerciseDatasetTest {

    /** Feature 3: row validity check rejects blank/null titles. */
    @Test
    void isValidDistinguishesBlankFromContent() {
        // Typical valid row.
        ExerciseDataset valid = new ExerciseDataset(
                "Pushup", "desc", "Strength", "Chest", "Body", "Beginner");
        assertTrue(valid.isValid());

        // Boundary: null title.
        assertFalse(new ExerciseDataset(null, "", "", "", "", "").isValid());

        // Boundary: empty string title.
        assertFalse(new ExerciseDataset("", "", "", "", "", "").isValid());

        // Boundary: whitespace-only title (the raw CSV has rows like this).
        assertFalse(new ExerciseDataset("   ", "", "", "", "", "").isValid());
    }
}
