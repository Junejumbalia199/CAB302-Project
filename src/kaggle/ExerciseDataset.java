package kaggle;

/**
 * One row out of the Kaggle megaGymDataset.csv, trimmed down to the columns
 * I actually show in the UI. I made it a record because I want it immutable
 * and I don't need getters/setters ceremony for six strings.
 *
 * The full CSV (from niharika41298/gym-exercise-data) has more columns:
 *   Title, Desc, Type, BodyPart, Equipment, Level, Rating, RatingDesc
 * I skipped Rating and RatingDesc — I'm not displaying them anywhere yet.
 */
public record ExerciseDataset(
        String title,
        String description,
        String type,
        String bodyPart,
        String equipment,
        String level
) {
    /** Quick check for a usable row. The raw CSV has a handful of blank titles. */
    public boolean isValid() {
        return title != null && !title.isBlank();
    }
}
