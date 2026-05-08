package com.formcoach.textoutputgen;

import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import com.formcoach.textoutputgen.PoseValidationException;

public class textoutputgen {

    // =========================================================
    // CONFIG
    // =========================================================

    private static final int LEFT_SHOULDER = 11;
    private static final int RIGHT_SHOULDER = 12;
    private final Boolean[] skipIndex = new Boolean[]{true, true, true, true, true, true, true, true, true, true, true, false, false, false, false, false, false, true, true, true, true, true, true, false, false, false, false, false, false, false, false, true, true};
    private final Double[] pushupTols = new Double[]{0.35, 0.15, 0.05, 0.0};
    private final Double[] situpTols = new Double[]{0.35, 0.15, 0.05, 0.0};
    private final Double[] squatTols = new Double[]{0.35, 0.15, 0.05, 0.0};
    private final String[] poseLandmarkNames = new String[]{"nose", "left eye (inner)", "left eye", "left eye (outer)", "right eye (inner)", "right eye", "right eye (outer)", "left ear", "right ear", "mouth (left)", "mouth (right)", "left shoulder", "right shoulder", "left elbow", "right elbow", "left wrist", "right wrist", "left pinky", "right pinky", "left index", "right index", "left thumb", "right thumb", "left hip", "right hip", "left knee", "right knee", "left ankle", "right ankle", "left heel", "right heel", "left foot index", "right foot index"};
    public Boolean disableFlavourText = true;
    public String VBadText = "WARNING. Stop immediately. You may injure yourself.";
    public String BadText = "Your form isn't quite right. Here's where you need to adjust.";
    public String GoodText = "Your form is pretty good, but you can adjust a bit.";
    public String PerfectText = "Your form is perfect!";
    public String[] FlavourText = new String[]{"Good job!", "Keep it up!", "Great work!", "Keep on improving!", "Don't give up!"};

    // =========================================================
    // PUBLIC API
    // =========================================================

    public PoseResult output(Double[] userX, Double[] userY, Double[] userZ, Double[] idealX, Double[] idealY, Double[] idealZ, String exerciseType) {

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

        int worstTol = 3;

        worstTol = Math.min(parse(ux, ix, tols, 'x', movementMap), Math.min(parse(uy, iy, tols, 'y', movementMap), parse(uz, iz, tols, 'z', movementMap)));

        // =====================================================
        // SCORE FIX (IMPORTANT)
        // =====================================================

        double score = switch (worstTol) {
            case 0 -> 0;
            case 1 -> 3;
            case 2 -> 6;
            default -> 10;
        };

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

    // =========================================================
    // VALIDATION (FIXED FOR TESTS)
    // =========================================================

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

    // =========================================================
    // CORE LOGIC
    // =========================================================

    private int parse(Double[] user, Double[] ideal, Double[] tols, char axis, Map<String, MovementInfo> map) {

        int worst = 3;

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
            worst = Math.min(worst, tl);

            String part = normalize(poseLandmarkNames[i]);

            map.putIfAbsent(part, new MovementInfo());
            MovementInfo info = map.get(part);

            info.directions.add(dir);
            info.worstTol = Math.min(info.worstTol, tl);
        }

        return worst;
    }

    private String normalize(String name) {
        if (name.startsWith("left ")) return name.substring(5);
        if (name.startsWith("right ")) return name.substring(6);
        return name;
    }

    private int tolerance(double diff, Double[] t) {

        double a = Math.abs(diff);

        if (a > t[0]) return 0;
        if (a > t[1]) return 1;
        if (a > t[2]) return 2;
        return 3;
    }

    // =========================================================
    // NORMALISATION (UNCHANGED)
    // =========================================================

    private Double[] normalize(Double[] x, Double[] y, Double[] target) {

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

    private Double[] normalizeZ(Double[] z, Double[] x, Double[] y) {

        Double[] out = new Double[z.length];

        double w = Math.sqrt(Math.pow(x[LEFT_SHOULDER] - x[RIGHT_SHOULDER], 2) + Math.pow(y[LEFT_SHOULDER] - y[RIGHT_SHOULDER], 2));

        if (w < 1e-6) w = 1e-6;

        double cz = (z[LEFT_SHOULDER] + z[RIGHT_SHOULDER]) / 2.0;

        for (int i = 0; i < z.length; i++) {
            out[i] = (z[i] - cz) / w;
        }

        return out;
    }

    // =========================================================
    // DATA STRUCTURES
    // =========================================================

    static class MovementInfo {
        Set<String> directions = new LinkedHashSet<>();
        int worstTol = 3;
    }

    public record PoseResult(String summaryText, List<String> movementFeedback, double score, int severity,
                             String flavourText, boolean valid) {

    }
}