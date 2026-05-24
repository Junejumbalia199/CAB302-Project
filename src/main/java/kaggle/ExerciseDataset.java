package kaggle;

/**
 * Single row from megaGymDataset.csv (Title, Desc, Type, BodyPart, Equipment, Level).
 * @param title       display name of the exercise
 * @param description plain-text description of the exercise
 * @param type        category (e.g. Strength, Cardio)
 * @param bodyPart    targeted body area (e.g. Chest, Legs)
 * @param equipment   required equipment (e.g. Barbell, Body Only)
 * @param level       difficulty level (e.g. Beginner, Intermediate)
 */
public record ExerciseDataset(
        String title,
        String description,
        String type,
        String bodyPart,
        String equipment,
        String level
) {
    /**
     * Returns {@code true} if the title is non-blank.
     * @return {@code true} if this row has a valid non-empty title
     */
    public boolean isValid() {
        return title != null && !title.isBlank();
    }
}
