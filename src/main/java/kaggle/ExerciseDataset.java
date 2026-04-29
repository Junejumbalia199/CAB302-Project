package kaggle;

/** Single row from megaGymDataset.csv (Title, Desc, Type, BodyPart, Equipment, Level). */
public record ExerciseDataset(
        String title,
        String description,
        String type,
        String bodyPart,
        String equipment,
        String level
) {
    /** True if title is non-blank. */
    public boolean isValid() {
        return title != null && !title.isBlank();
    }
}
