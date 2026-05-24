package com.formcoach.poseanalysis;

import java.util.List;

/**
 * Scores how closely a live pose matches the reference data for a given exercise.
 * Call score() each frame to get a ScorerResult containing the 0-1 similarity,
 * the best-matching ideal pose xyz, and the exercise type string ready for
 * passing straight into textoutputgen.
 */
public final class PoseScorer {

    // per-angle error threshold in degrees - anything beyond this scores 0 for that joint
    private static final double MAX_ANGLE_ERROR_DEG = 45.0;

    /** Sentinel returned when scoring cannot produce a valid result. Always check {@link ScorerResult#isValid()}. */
    public static final ScorerResult INVALID = new ScorerResult(-1, null, null, null, null);

    private final PoseReference reference;
    private final String textOutputType;

    // cached angles from the most recent score() call — used by RepetitionCounter
    private double[] lastAngles;

    /**
     * Constructs a PoseScorer and loads reference pose data for the given exercise.
     * @param exerciseName the display name of the exercise (e.g. {@code "Push-ups"})
     */
    public PoseScorer(String exerciseName) {
        reference = new PoseReference(exerciseName);
        textOutputType = toTextOutputType(exerciseName);
        System.out.println("[form] loaded " + reference.getRows().size()
                + " reference poses for: " + exerciseName);
    }

    /**
     * Finds the best-matching reference pose for the given landmarks and returns a ScorerResult.
     * Check {@link ScorerResult#isValid()} before using the result.
     * @param landmarks the 33-point landmark list from MediaPipe (each element is {x, y, z, visibility})
     * @return the best-matching {@link ScorerResult}, or {@link #INVALID} if landmarks are incomplete
     */
    public ScorerResult score(List<float[]> landmarks) {
        if (landmarks == null || landmarks.size() < 33) return INVALID;
        if (reference.getRows().isEmpty()) return INVALID;

        double[] live = computeAngles(landmarks);
        if (live == null) return INVALID;
        lastAngles = live;

        // find the reference row with the highest angle similarity score
        double bestScore = -1;
        PoseReference.ReferenceRow bestRow = null;
        for (PoseReference.ReferenceRow row : reference.getRows()) {
            double s = scoreAgainstAngles(live, row.angles());
            if (s > bestScore) {
                bestScore = s;
                bestRow = row;
            }
        }

        if (bestRow == null) return INVALID;
        return new ScorerResult(bestScore, bestRow.x(), bestRow.y(), bestRow.z(), textOutputType);
    }

    /**
     * Holds the result of one scoring pass. Use {@link #isValid()} before accessing fields.
     * @param score        similarity score in the range 0.0 (no match) to 1.0 (perfect match)
     * @param idealX       x-coordinates of the best-matching reference pose landmarks
     * @param idealY       y-coordinates of the best-matching reference pose landmarks
     * @param idealZ       z-coordinates of the best-matching reference pose landmarks
     * @param exerciseType exercise type string expected by {@code textoutputgen}
     */
    public record ScorerResult(double score, Double[] idealX, Double[] idealY, Double[] idealZ, String exerciseType) {
        /**
         * Returns {@code true} if this result contains valid scoring data.
         * @return {@code true} if score is non-negative and all coordinate arrays are present
         */
        public boolean isValid() {
            return score >= 0 && idealX != null && exerciseType != null;
        }
    }

    // maps the display name to the string textoutputgen expects in its switch statement
    private static String toTextOutputType(String exerciseName) {
        if (exerciseName == null) return null;
        return switch (exerciseName.toLowerCase()) {
            case "push-ups" -> "Pushup";
            case "sit-ups"  -> "Situp";
            case "squats"   -> "Squat";
            default         -> null;
        };
    }

    /**
     * Returns the 7 joint angles computed during the most recent {@link #score} call,
     * or {@code null} if {@code score()} has not been called yet or returned {@link #INVALID}.
     * Indices: 0=right elbow, 1=left elbow, 2=knee, 3=right hip-knee-ankle,
     * 4=left hip-knee-ankle, 5=right wrist-elbow-shoulder, 6=left wrist-elbow-shoulder.
     * @return 7-element angle array in degrees, or {@code null}
     */
    public double[] getLastAngles() {
        return lastAngles;
    }

    /**
     * Computes the 7 joint angles from a raw landmark list.
     * @param lm the 33-point landmark list from MediaPipe
     * @return 7-element angle array in degrees, or {@code null} if landmarks are out of bounds
     */
    public static double[] computeAngles(List<float[]> lm) {
        try {
            double[] a = new double[7];

            // right_elbow(14) - right_shoulder(12) - right_hip(24)
            a[0] = angleDeg(lm.get(14), lm.get(12), lm.get(24));
            // left_elbow(13) - left_shoulder(11) - left_hip(23)
            a[1] = angleDeg(lm.get(13), lm.get(11), lm.get(23));
            // right_knee(26) - mid_hip - left_knee(25)
            float[] midHip = midpoint(lm.get(23), lm.get(24));
            a[2] = angleDeg(lm.get(26), midHip, lm.get(25));
            // right_hip(24) - right_knee(26) - right_ankle(28)
            a[3] = angleDeg(lm.get(24), lm.get(26), lm.get(28));
            // left_hip(23) - left_knee(25) - left_ankle(27)
            a[4] = angleDeg(lm.get(23), lm.get(25), lm.get(27));
            // right_wrist(16) - right_elbow(14) - right_shoulder(12)
            a[5] = angleDeg(lm.get(16), lm.get(14), lm.get(12));
            // left_wrist(15) - left_elbow(13) - left_shoulder(11)
            a[6] = angleDeg(lm.get(15), lm.get(13), lm.get(11));

            return a;
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }

    // angle in degrees at vertex B, formed by points A-B-C, using x,y coords only
    private static double angleDeg(float[] a, float[] b, float[] c) {
        double bax = a[0] - b[0];
        double bay = a[1] - b[1];
        double bcx = c[0] - b[0];
        double bcy = c[1] - b[1];

        double dot    = bax * bcx + bay * bcy;
        double magBA  = Math.sqrt(bax * bax + bay * bay);
        double magBC  = Math.sqrt(bcx * bcx + bcy * bcy);

        // avoid divide-by-zero if two landmarks overlap
        if (magBA < 1e-6 || magBC < 1e-6) return 0;

        // clamp for floating point rounding that might push past +/-1
        double cosA = Math.max(-1.0, Math.min(1.0, dot / (magBA * magBC)));
        return Math.toDegrees(Math.acos(cosA));
    }

    // returns the midpoint of two landmarks as a dummy landmark array
    private static float[] midpoint(float[] a, float[] b) {
        return new float[]{(a[0] + b[0]) / 2f, (a[1] + b[1]) / 2f, 0f, 0f};
    }

    // scores one live angle vector against one reference row's angles, returns 0-1
    private static double scoreAgainstAngles(double[] live, double[] ref) {
        double totalError = 0;
        for (int i = 0; i < 7; i++) {
            double normError = Math.abs(live[i] - ref[i]) / MAX_ANGLE_ERROR_DEG;
            totalError += Math.min(1.0, normError);
        }
        // average error across 7 joints, inverted so 1 = perfect
        return Math.max(0.0, 1.0 - totalError / 7.0);
    }
}