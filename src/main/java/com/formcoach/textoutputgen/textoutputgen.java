package com.formcoach.textoutputgen;

import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Converts raw pose-comparison data into human-readable feedback and a 0–10 form score.
 * Call {@link #output} each feedback interval with the user's and ideal pose coordinates;
 * the returned {@link PoseResult} contains a summary string, per-joint movement hints,
 * and the numeric score ready for display.
 */
/**
 * Generates human-readable feedback from pose landmark comparisons.
 *
 * <p>This class compares a user's detected pose landmarks against an ideal pose
 * and produces:
 *
 * <ul>
 *     <li>A summary message</li>
 *     <li>Movement correction suggestions</li>
 *     <li>A weighted form score from 0.0 to 10.0</li>
 *     <li>A severity classification</li>
 * </ul>
 *
 * <p>The comparison system:
 *
 * <ul>
 *     <li>Normalizes landmark positions relative to shoulder width</li>
 *     <li>Uses weighted joints to prioritize important body parts</li>
 *     <li>Evaluates pose similarity independently on X, Y, and Z axes</li>
 *     <li>Produces directional movement guidance</li>
 * </ul>
 *
 * <p>Supported exercises:
 *
 * <ul>
 *     <li>Pushup</li>
 *     <li>Situp</li>
 *     <li>Squat</li>
 * </ul>
 */
public class textoutputgen {

    /** Constructs a new textoutputgen instance with default feedback strings and tolerances. */
    public textoutputgen() {}

    // =========================================================
    // CONFIG
    // =========================================================

    /**
     * MediaPipe index for the left shoulder landmark.
     */
    private static final int LEFT_SHOULDER = 11;

    /**
     * MediaPipe index for the right shoulder landmark.
     */
    private static final int RIGHT_SHOULDER = 12;

    /**
     * Per-joint weighting values used during score calculation.
     *
     * <p>Higher values increase the impact of a landmark on the final score.
     */
    private static final double[] JOINT_WEIGHTS = new double[33];

    static {

        // default weight
        Arrays.fill(JOINT_WEIGHTS, 0.5);

        // high importance joints
        JOINT_WEIGHTS[11] = 2.0; // left shoulder
        JOINT_WEIGHTS[12] = 2.0; // right shoulder
        JOINT_WEIGHTS[23] = 2.0; // left hip
        JOINT_WEIGHTS[24] = 2.0; // right hip
        JOINT_WEIGHTS[25] = 2.5; // left knee
        JOINT_WEIGHTS[26] = 2.5; // right knee
        JOINT_WEIGHTS[13] = 1.8; // left elbow
        JOINT_WEIGHTS[14] = 1.8; // right elbow

        // medium importance
        JOINT_WEIGHTS[15] = 1.2;
        JOINT_WEIGHTS[16] = 1.2;
    }

    /**
     * Indicates which landmark indices should be ignored during analysis.
     */
    private final Boolean[] skipIndex = new Boolean[]{
            true, true, true, true, true, true, true, true, true, true, true,
            false, false, false, false, false, false,
            true, true, true, true, true, true,
            false, false, false, false, false, false, false, false,
            true, true
    };

    /**
     * Tolerance thresholds for pushup analysis.
     */
    private final Double[] pushupTols = new Double[]{0.35, 0.15, 0.05, 0.0};

    /**
     * Tolerance thresholds for situp analysis.
     */
    private final Double[] situpTols = new Double[]{0.35, 0.15, 0.05, 0.0};

    /**
     * Tolerance thresholds for squat analysis.
     */
    private final Double[] squatTols = new Double[]{0.35, 0.15, 0.05, 0.0};
    private final String[] poseLandmarkNames = new String[]{"nose", "left eye (inner)", "left eye", "left eye (outer)", "right eye (inner)", "right eye", "right eye (outer)", "left ear", "right ear", "mouth (left)", "mouth (right)", "left shoulder", "right shoulder", "left elbow", "right elbow", "left wrist", "right wrist", "left pinky", "right pinky", "left index", "right index", "left thumb", "right thumb", "left hip", "right hip", "left knee", "right knee", "left ankle", "right ankle", "left heel", "right heel", "left foot index", "right foot index"};
    /** When {@code true}, no random encouragement suffix is appended to feedback. */

    /**
     * Human-readable landmark names indexed by MediaPipe landmark ID.
     */
    private final String[] poseLandmarkNames = new String[]{
            "nose", "left eye (inner)", "left eye", "left eye (outer)",
            "right eye (inner)", "right eye", "right eye (outer)",
            "left ear", "right ear", "mouth (left)", "mouth (right)",
            "left shoulder", "right shoulder",
            "left elbow", "right elbow",
            "left wrist", "right wrist",
            "left pinky", "right pinky",
            "left index", "right index",
            "left thumb", "right thumb",
            "left hip", "right hip",
            "left knee", "right knee",
            "left ankle", "right ankle",
            "left heel", "right heel",
            "left foot index", "right foot index"
    };

    /**
     * If true, disables randomized encouragement text.
     */
    public Boolean disableFlavourText = true;
    /** Feedback shown when form is dangerously wrong (severity 0). */

    /**
     * Summary message for severe pose errors.
     */
    public String VBadText = "WARNING. Stop immediately. You may injure yourself.";
    /** Feedback shown when form needs significant correction (severity 1). */

    /**
     * Summary message for poor form.
     */
    public String BadText = "Your form isn't quite right. Here's where you need to adjust.";
    /** Feedback shown when form is acceptable but improvable (severity 2). */

    /**
     * Summary message for acceptable form.
     */
    public String GoodText = "Your form is pretty good, but you can adjust a bit.";
    /** Feedback shown when form is perfect (severity 3). */

    /**
     * Summary message for excellent form.
     */
    public String PerfectText = "Your form is perfect!";
    /** Pool of random encouragement messages appended when flavour text is enabled. */
    public String[] FlavourText = new String[]{"Good job!", "Keep it up!", "Great work!", "Keep on improving!", "Don't give up!"};

    /**
     * Optional randomized encouragement phrases.
     */
    public String[] FlavourText = new String[]{
            "Good job!",
            "Keep it up!",
            "Great work!",
            "Keep on improving!",
            "Don't give up!"
    };

    // =========================================================
    // The main output method to get the results
    // =========================================================

    /**
     * Scores the user's pose against the ideal pose and produces feedback text.
     * @param userX        x-coordinates of the user's 33 pose landmarks
     * @param userY        y-coordinates of the user's 33 pose landmarks
     * @param userZ        z-coordinates of the user's 33 pose landmarks
     * @param idealX       x-coordinates of the reference pose's 33 landmarks
     * @param idealY       y-coordinates of the reference pose's 33 landmarks
     * @param idealZ       z-coordinates of the reference pose's 33 landmarks
     * @param exerciseType one of {@code "Pushup"}, {@code "Situp"}, or {@code "Squat"}
     * @return a {@link PoseResult} containing summary text, movement feedback, and a 0–10 score
     * @throws PoseValidationException if any input array contains null or NaN values,
     *                                 or if {@code exerciseType} is not recognised
     */
    public PoseResult output(Double[] userX, Double[] userY, Double[] userZ, Double[] idealX, Double[] idealY, Double[] idealZ, String exerciseType) {
    /**
     * Compares a user's pose against an ideal pose and generates feedback.
     *
     * <p>The method:
     *
     * <ol>
     *     <li>Validates all landmark arrays</li>
     *     <li>Normalizes pose coordinates</li>
     *     <li>Calculates weighted similarity scores</li>
     *     <li>Determines movement corrections</li>
     *     <li>Generates a final {@link PoseResult}</li>
     * </ol>
     *
     * @param userX user's X-axis landmark coordinates
     * @param userY user's Y-axis landmark coordinates
     * @param userZ user's Z-axis landmark coordinates
     * @param idealX ideal X-axis landmark coordinates
     * @param idealY ideal Y-axis landmark coordinates
     * @param idealZ ideal Z-axis landmark coordinates
     * @param exerciseType exercise identifier ("Pushup", "Situp", or "Squat")
     * @return populated pose analysis result
     * @throws PoseValidationException if input arrays are invalid
     */
    public PoseResult output(
            Double[] userX,
            Double[] userY,
            Double[] userZ,
            Double[] idealX,
            Double[] idealY,
            Double[] idealZ,
            String exerciseType
    ) {

        validate(userX, userY, userZ, idealX, idealY, idealZ);

        Double[] tols = switch (exerciseType) {
            case "Pushup" -> pushupTols;
            case "Situp" -> situpTols;
            case "Squat" -> squatTols;
            default -> throw new PoseValidationException("INVALID_EXERCISE", "Exercise type not recognized");
        };

        Map<String, MovementInfo> movementMap = new LinkedHashMap<>();

        Double[] ux = normalize(userX, userY, userX);
        Double[] uy = normalize(userX, userY, userY);
        Double[] uz = normalizeZ(userZ, userX, userY);

        Double[] ix = normalize(idealX, idealY, idealX);
        Double[] iy = normalize(idealX, idealY, idealY);
        Double[] iz = normalizeZ(idealZ, idealX, idealY);

        ParseStats xStats = parse(ux, ix, tols, 'x', movementMap);
        ParseStats yStats = parse(uy, iy, tols, 'y', movementMap);
        ParseStats zStats = parse(uz, iz, tols, 'z', movementMap);

        int worstTol = Math.min(xStats.worstTol, Math.min(yStats.worstTol, zStats.worstTol));

        double averageScore = (xStats.average() + yStats.average() + zStats.average()) / 3.0;

        double score = Math.pow(averageScore, 1.35) * 10.0;

        score = Math.max(0.0, Math.min(10.0, score));
        score = Math.round(score * 10.0) / 10.0;

        List<String> movementOutput = new ArrayList<>();

        for (var e : movementMap.entrySet()) {

            String part = e.getKey();
            MovementInfo info = e.getValue();

            String directions = String.join(" and ", info.directions);

            if (info.worstTol <= 1) {
                movementOutput.add(MessageFormat.format("Move your {0} {1}.", part, directions));
            } else if (info.worstTol == 2) {
                movementOutput.add(MessageFormat.format("Move your {0} {1} a little.", part, directions));
            }
        }

        String summary = switch (worstTol) {
            case 0 -> VBadText;
            case 1 -> BadText;
            case 2 -> GoodText;
            default -> PerfectText;
        };

        String flavour = null;

        if (!Boolean.TRUE.equals(disableFlavourText)) {
            flavour = FlavourText[ThreadLocalRandom.current().nextInt(FlavourText.length)];
        }

        return new PoseResult(summary, movementOutput, score, worstTol, flavour, true);
    }

    /**
     * Validates all pose arrays and values.
     *
     * @param arrays pose coordinate arrays to validate
     * @throws PoseValidationException if any array or value is invalid
     */
    private void validate(Double[]... arrays) {

        for (Double[] arr : arrays) {

            if (arr == null) {
                throw new PoseValidationException("NULL_INPUT", "Null pose array");
            }

            for (Double v : arr) {

                if (v == null || Double.isNaN(v) || Double.isInfinite(v)) {
                    throw new PoseValidationException("INVALID_VALUE", "NaN detected in pose data");
                }
            }
        }
    }

    /**
     * Parses normalized landmark data and calculates scoring statistics.
     *
     * @param user normalized user coordinates
     * @param ideal normalized ideal coordinates
     * @param tols tolerance thresholds
     * @param axis current axis being analyzed
     * @param map movement aggregation map
     * @return statistics for the parsed axis
     */
    private ParseStats parse(
            Double[] user,
            Double[] ideal,
            Double[] tols,
            char axis,
            Map<String, MovementInfo> map
    ) {

        ParseStats stats = new ParseStats();

        for (int i = 0; i < user.length; i++) {

            if (skipIndex[i]) continue;

            double diff = user[i] - ideal[i];

            String dir = switch (axis) {
                case 'x' -> diff > 0 ? "left" : "right";
                case 'y' -> diff > 0 ? "up" : "down";
                case 'z' -> diff > 0 ? "forward" : "backward";
                default -> "";
            };

            int tl = tolerance(diff, tols);

            stats.add(tl, JOINT_WEIGHTS[i]);

            String part = normalize(poseLandmarkNames[i]);

            map.putIfAbsent(part, new MovementInfo());

            MovementInfo info = map.get(part);

            if (tl < 3) {
                info.directions.add(dir);
            }

            info.worstTol = Math.min(info.worstTol, tl);
        }

        return stats;
    }

    /**
     * Removes left/right prefixes from landmark names.
     *
     * @param name landmark name
     * @return normalized body-part name
     */
    private String normalize(String name) {
        if (name.startsWith("left ")) return name.substring(5);
        if (name.startsWith("right ")) return name.substring(6);
        return name;
    }

    /**
     * Converts a positional difference into a tolerance category.
     *
     * @param diff positional difference
     * @param t tolerance thresholds
     * @return tolerance level:
     *         <ul>
     *             <li>0 = severe</li>
     *             <li>1 = poor</li>
     *             <li>2 = acceptable</li>
     *             <li>3 = perfect</li>
     *         </ul>
     */
    private int tolerance(double diff, Double[] t) {

        final double EPS = 1e-9;

        double a = Math.abs(diff);

        if (a - t[0] > EPS) return 0;
        if (a - t[1] > EPS) return 1;
        if (a - t[2] > EPS) return 2;

        return 3;
    }

    /**
     * Normalizes X or Y coordinates relative to shoulder midpoint and width.
     *
     * @param x x-coordinate array
     * @param y y-coordinate array
     * @param target target coordinate array
     * @return normalized coordinates
     */
    private Double[] normalize(
            Double[] x,
            Double[] y,
            Double[] target
    ) {

        Double[] out = new Double[target.length];

        double cx = (x[LEFT_SHOULDER] + x[RIGHT_SHOULDER]) / 2.0;
        double cy = (y[LEFT_SHOULDER] + y[RIGHT_SHOULDER]) / 2.0;

        double w = Math.sqrt(Math.pow(x[LEFT_SHOULDER] - x[RIGHT_SHOULDER], 2) + Math.pow(y[LEFT_SHOULDER] - y[RIGHT_SHOULDER], 2));

        if (w < 1e-6) w = 1e-6;

        for (int i = 0; i < target.length; i++) {

            double v = (target == x) ? x[i] - cx : y[i] - cy;

            out[i] = v / w;
        }

        return out;
    }

    /**
     * Normalizes Z coordinates relative to shoulder width.
     *
     * @param z z-coordinate array
     * @param x x-coordinate array
     * @param y y-coordinate array
     * @return normalized z coordinates
     */
    private Double[] normalizeZ(
            Double[] z,
            Double[] x,
            Double[] y
    ) {

        Double[] out = new Double[z.length];

        double w = Math.sqrt(Math.pow(x[LEFT_SHOULDER] - x[RIGHT_SHOULDER], 2) + Math.pow(y[LEFT_SHOULDER] - y[RIGHT_SHOULDER], 2));

        if (w < 1e-6) w = 1e-6;

        double cz = (z[LEFT_SHOULDER] + z[RIGHT_SHOULDER]) / 2.0;

        for (int i = 0; i < z.length; i++) {
            out[i] = (z[i] - cz) / w;
        }

        return out;
    }

    /**
     * Prints a formatted pose result to standard output.
     *
     * @param result result object to print
     */
    public void printPoseResult(PoseResult result) {

        if (result == null) {
            System.out.println("PoseResult is null.");
            return;
        }

        System.out.println(result.summaryText());

        if (result.movementFeedback() == null || result.movementFeedback().isEmpty()) {
            System.out.println("- No feedback needed!");
        } else {
            for (String feedback : result.movementFeedback()) {
                System.out.println("- " + feedback);
            }
        }

        System.out.println("Score: " + result.score());
        System.out.println("Severity: " + result.severity());

        String flavour = result.flavourText();

        if (flavour == null || flavour.isBlank()) {
            flavour = "";
        }

        System.out.println(flavour);
    }

    // =========================================================
    // Data structures
    // =========================================================

    /**
     * Stores movement correction details for a body part.
     */
    static class MovementInfo {

        /**
         * Suggested movement directions.
         */
        Set<String> directions = new LinkedHashSet<>();

        /**
         * Worst detected tolerance level.
         */
        int worstTol = 3;
    }

    /**
     * Immutable result object produced after pose analysis.
     *
     * @param summaryText overall summary message
     * @param movementFeedback detailed movement corrections
     * @param score weighted score from 0.0 to 10.0
     * @param severity severity category
     * @param flavourText optional encouragement message
     * @param valid indicates successful processing
     */
    public record PoseResult(
            String summaryText,
            List<String> movementFeedback,
            double score,
            int severity,
            String flavourText,
            boolean valid
    ) {
    }

    /**
     * Stores weighted scoring statistics during parsing.
     */
    static class ParseStats {

        /**
         * Accumulated weighted score total.
         */
        double weightedSum = 0;

        /**
         * Total applied joint weight.
         */
        double weightTotal = 0;

        /**
         * Worst tolerance level encountered.
         */
        int worstTol = 3;

        /**
         * Adds a weighted tolerance score.
         *
         * @param tol tolerance level
         * @param weight joint importance weight
         */
        void add(int tol, double weight) {

            worstTol = Math.min(worstTol, tol);

            double score = switch (tol) {
                case 3 -> 1.0;
                case 2 -> 0.7;
                case 1 -> 0.3;
                default -> 0.0;
            };

            weightedSum += score * weight;
            weightTotal += weight;
        }

        /**
         * Calculates the weighted average score.
         *
         * @return weighted average between 0.0 and 1.0
         */
        double average() {
            return weightTotal == 0 ? 1.0 : weightedSum / weightTotal;
        }
    }
}