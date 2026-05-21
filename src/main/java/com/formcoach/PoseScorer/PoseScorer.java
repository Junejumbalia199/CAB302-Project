package com.formcoach.poseanalysis;

import java.util.List;

/**
 * Scores how closely a live pose matches the reference data for a given exercise.
 * The returned score is between 0 (nothing matches) and 1 (perfect match).
 *
 * Scoring works by computing the same 7 joint angles used in the dataset, then
 * finding the closest reference frame and measuring how far off each angle is.
 * An error of 45 degrees on any single angle contributes the maximum penalty for
 * that joint, so small deviations have little effect on the overall score.
 */
public final class PoseScorer {

    // per-angle error threshold in degrees - anything beyond this scores 0 for that joint
    private static final double MAX_ANGLE_ERROR_DEG = 45.0;

    private final PoseReference reference;

    public PoseScorer(String exerciseName) {
        reference = new PoseReference(exerciseName);
        System.out.println("[form] loaded " + reference.getAngles().size()
                + " reference poses for: " + exerciseName);
    }

    /**
     * Returns a 0-1 form score for the given landmark list, or -1 if scoring
     * isn't possible (no landmarks, not enough points, unsupported exercise).
     */
    public double score(List<float[]> landmarks) {
        if (landmarks == null || landmarks.size() < 33) return -1;
        if (reference.getAngles().isEmpty()) return -1;

        double[] live = computeAngles(landmarks);
        if (live == null) return -1;

        // find the reference row that best matches the current pose
        double best = 0;
        for (double[] ref : reference.getAngles()) {
            double s = scoreAgainstRow(live, ref);
            if (s > best) best = s;
        }
        return best;
    }

    // computes the 7 joint angles that match the dataset's angle columns
    private static double[] computeAngles(List<float[]> lm) {
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

        double dot = bax * bcx + bay * bcy;
        double magBA = Math.sqrt(bax * bax + bay * bay);
        double magBC = Math.sqrt(bcx * bcx + bcy * bcy);

        // avoid divide-by-zero if two landmarks are on the same pixel
        if (magBA < 1e-6 || magBC < 1e-6) return 0;

        // clamp for floating point rounding that might push past +/-1
        double cosA = Math.max(-1.0, Math.min(1.0, dot / (magBA * magBC)));
        return Math.toDegrees(Math.acos(cosA));
    }

    // returns the midpoint of two landmarks as a dummy landmark array
    private static float[] midpoint(float[] a, float[] b) {
        return new float[]{(a[0] + b[0]) / 2f, (a[1] + b[1]) / 2f, 0f, 0f};
    }

    // scores one live angle vector against one reference row, returns 0-1
    private static double scoreAgainstRow(double[] live, double[] ref) {
        double totalError = 0;
        for (int i = 0; i < 7; i++) {
            double normError = Math.abs(live[i] - ref[i]) / MAX_ANGLE_ERROR_DEG;
            totalError += Math.min(1.0, normError);
        }
        // average error across 7 joints, inverted so 1 = perfect
        return Math.max(0.0, 1.0 - totalError / 7.0);
    }
}