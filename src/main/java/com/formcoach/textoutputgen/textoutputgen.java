package com.formcoach.textoutputgen;

import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class textoutputgen {

    // text output composite parts
    public Boolean disableFlavourText;

    private Boolean[] skipIndex = new Boolean[]{
            true,true,true,true,true,true,true,true,true,true,true,false,
            false,false,false,false,false,true,true,true,true,true,true,false,false,false,false,false,
            false,false,false,true,true
    };

    public String VBadText = "WARNING. Stop immediately. You may injure yourself.";
    public String BadText = "Your form isn't quite right. Here's where you need to adjust.";
    public String GoodText = "Your form is pretty good, but you can adjust a bit.";
    public String PerfectText = "Your form is perfect!";

    private String BadOutputFormat = "Move your {0} {1}.";
    private String GoodOutputFormat = "Move your {0} {1} a little.";

    public String[] FlavourText = new String[]{
            "Good job!", "Keep it up!", "Great work!", "Keep on improving!", "Don't give up!"
    };

    private Double[] pushupTols = new Double[]{0.1, 0.01, 0.001, 0.0001};
    private Double[] situpTols = new Double[]{0.1, 0.01, 0.001, 0.0001};
    private Double[] squatTols = new Double[]{0.1, 0.01, 0.001, 0.0001};

    private String[] poseLandmarkNames = new String[]{
            "nose", "left eye (inner)", "left eye", "left eye (outer)",
            "right eye (inner)", "right eye", "right eye (outer)", "left ear",
            "right ear", "mouth (left)", "mouth (right)",
            "left shoulder", "right shoulder", "left elbow", "right elbow",
            "left wrist", "right wrist", "left pinky", "right pinky",
            "left index", "right index", "left thumb", "right thumb",
            "left hip", "right hip", "left knee", "right knee",
            "left ankle", "right ankle", "left heel", "right heel",
            "left foot index", "right foot index"
    };

    static class ParseResult {
        String text;
        int worstTol;

        ParseResult(String text, int worstTol) {
            this.text = text;
            this.worstTol = worstTol;
        }
    }

    // Stores movement info for each body part
    static class MovementInfo {
        Set<String> directions = new LinkedHashSet<>();
        int worstTol = 3;
    }

    public String output(Double[] userPoseX, Double[] userPoseY, Double[] userPoseZ,
                         Double[] idealPoseX, Double[] idealPoseY, Double[] idealPoseZ,
                         String exerciseType) {

        int worstTol = 3;
        StringBuilder result = new StringBuilder();

        Double[] tols = switch (exerciseType) {
            case "Pushup" -> pushupTols;
            case "Situp" -> situpTols;
            case "Squat" -> squatTols;
            default -> throw new IllegalArgumentException(
                    "Unsupported exercise type: " + exerciseType
            );
        };

        // Combined movement map
        Map<String, MovementInfo> movementMap = new LinkedHashMap<>();

        worstTol = Math.min(
                parseCoordinates(userPoseX, idealPoseX, tols, 'x', movementMap),
                Math.min(
                        parseCoordinates(userPoseY, idealPoseY, tols, 'y', movementMap),
                        parseCoordinates(userPoseZ, idealPoseZ, tols, 'z', movementMap)
                )
        );

        // Build final text
        for (Map.Entry<String, MovementInfo> entry : movementMap.entrySet()) {

            String bodyPart = entry.getKey();
            MovementInfo info = entry.getValue();

            String directions = String.join(" and ", info.directions);

            switch (info.worstTol) {
                case 1:
                    result.append(
                            MessageFormat.format(BadOutputFormat, bodyPart, directions)
                    ).append("\n");
                    break;

                case 2:
                    result.append(
                            MessageFormat.format(GoodOutputFormat, bodyPart, directions)
                    ).append("\n");
                    break;
            }
        }

        // prepend summary text
        switch (worstTol) {
            case 0 -> result.insert(0, VBadText + "\n");
            case 1 -> result.insert(0, BadText + "\n");
            case 2 -> result.insert(0, GoodText + "\n");
            case 3 -> result.insert(0, PerfectText + "\n");
        }

        if (disableFlavourText) {
            return result.toString();
        }

        int rand = ThreadLocalRandom.current().nextInt(FlavourText.length);

        return result.append("\n").append(FlavourText[rand]).toString();
    }

    // Parse coordinates and combine movement instructions
    private int parseCoordinates(Double[] userPose,
                                 Double[] idealPose,
                                 Double[] tols,
                                 char axis,
                                 Map<String, MovementInfo> movementMap) {

        int worstTol = 3;

        for (int i = 0; i < userPose.length; i++) {

            if (skipIndex[i]) continue;

            double diff = userPose[i] - idealPose[i];

            String direction = switch (axis) {
                case 'x' -> diff > 0 ? "left" : "right";
                case 'y' -> diff > 0 ? "up" : "down";
                case 'z' -> diff > 0 ? "forward" : "backward";
                default -> "";
            };

            int tl = toleranceLevel(diff, tols);

            if (tl < worstTol) {
                worstTol = tl;
            }

            // Only output bad/good feedback
            if (tl == 1 || tl == 2) {

                String bodyPart = normalizeBodyPart(poseLandmarkNames[i]);

                movementMap.putIfAbsent(bodyPart, new MovementInfo());

                MovementInfo info = movementMap.get(bodyPart);

                info.directions.add(direction);

                if (tl < info.worstTol) {
                    info.worstTol = tl;
                }
            }
        }

        return worstTol;
    }

    // Combines left/right body parts
    private String normalizeBodyPart(String name) {

        if (name.startsWith("left ")) {
            return name.substring(5);
        }

        if (name.startsWith("right ")) {
            return name.substring(6);
        }

        return name;
    }

    // return appropriate tolerance level indexes
    private Integer toleranceLevel(double diff, Double[] tols) {

        double abs = Math.abs(diff);

        if (abs > tols[0]) {
            return 0;
        } else if (abs > tols[1]) {
            return 1;
        } else if (abs > tols[2]) {
            return 2;
        } else {
            return 3;
        }
    }
}