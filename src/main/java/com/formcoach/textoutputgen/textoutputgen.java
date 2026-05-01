package com.formcoach.textoutputgen;

import java.text.MessageFormat;
import java.util.concurrent.ThreadLocalRandom;

public class textoutputgen {
    // text output composite parts
    public Boolean disableFlavourText;
    private Boolean[] skipIndex = new Boolean[]{true,true,true,true,true,true,true,true,true,true,true,false,
            false,false,false,false,false,true,true,true,true,true,true,false,false,false,false,false,
            false,false,false,true,true};
    public String VBadText = "WARNING. Stop immediately. You may injure yourself.";
    public String BadText = "Your form isn't quite right. Here's where you need to adjust.";
    public String GoodText = "Your form is pretty good, but you can adjust a bit.";
    public String PerfectText = "Your form is perfect!";
    private String BadOutputFormat = "Move your {0} {1}.";
    private String GoodOutputFormat = "Move your {0} {1} a little.";
    public String[] FlavourText = new String[]
            {"Good job!", "Keep it up!", "Great work!", "Keep on improving!", "Don't give up!"};
    private Double[] pushupTols = new Double[]{0.1, 0.01, 0.001, 0.0001}; //define tols for vbad, bad, good, vgood
    private Double[] situpTols = new Double[]{0.1, 0.01, 0.001, 0.0001};
    private Double[] squatTols = new Double[]{0.1, 0.01, 0.001, 0.0001};
    private String[] poseLandmarkNames = new String[]{"nose", "left eye (inner)", "left eye", "left eye (outer)",
    "right eye (inner)", "right eye", "right eye (outer)", "left ear", "right ear", "mouth (left)", "mouth (right)",
    "left shoulder", "right shoulder", "left elbow", "right elbow", "left wrist", "right wrist", "left pinky",
    "right pinky", "left index", "right index", "left thumb", "right thumb", "left hip", "right hip",
    "left knee", "right knee", "left ankle", "right ankle", "left heel", "right heel", "left foot index",
            "right foot index"};

    static class ParseResult {
        String text;
        int worstTol;

        ParseResult(String text, int worstTol) {
            this.text = text;
            this.worstTol = worstTol;
        }
    }

    // a method that takes the coordinate inputs converted to separate x, y, z and gets text outputs
    public String output(Double[] userPoseX, Double[] idealPoseX,
                         Double[] userPoseY, Double[] idealPoseY,
                         Double[] userPoseZ, Double[] idealPoseZ,
                         String exerciseType) { // will take two inputs, need to check type
        int worstTol = 3; // start optimistic
        StringBuilder result = new StringBuilder();

        Double[] tols = new Double[4];

        tols = switch (exerciseType) {
            case "Pushup" -> pushupTols;
            case "Situp" -> situpTols;
            case "Squat" -> squatTols;
            default -> tols;
        };

        // run parsing for each axis
        ParseResult xRes = parseCoordinatesToString(userPoseX, idealPoseX, tols, 'x');
        ParseResult yRes = parseCoordinatesToString(userPoseY, idealPoseY, tols, 'y');
        ParseResult zRes = parseCoordinatesToString(userPoseZ, idealPoseZ, tols, 'z');

        // combine results
        result.append(xRes.text);
        result.append(yRes.text);
        result.append(zRes.text);

        // determine worst tolerance across all axes
        worstTol = Math.min(xRes.worstTol, Math.min(yRes.worstTol, zRes.worstTol));

        // prepend summary text
        switch (worstTol) {
            case 0:
                result.insert(0, VBadText + "\n");
                break;
            case 1:
                result.insert(0, BadText + "\n");
                break;
            case 2:
                result.insert(0, GoodText + "\n");
                break;
            case 3:
                result.insert(0, PerfectText + "\n");
                break;
        }

        if (disableFlavourText) {
            return result.toString();
        }

        int rand = ThreadLocalRandom.current().nextInt(FlavourText.length);

        return result.append("\n").append(FlavourText[rand]).toString();
    }

    // take two arrays of coordinates and return the tolerances needed, skipping the specified
    // number of indexes off the arrays
    private ParseResult parseCoordinatesToString(Double[] userPose, Double[] idealPose, Double[] tols, char axis) {
        StringBuilder result = new StringBuilder();
        int worstTol = 3; // start at best, go downward

        for (int i = 0; i < userPose.length; i++) {
            if (skipIndex[i]) continue;

            double diff = userPose[i] - idealPose[i];
            String direction = "";
            switch (axis) {
                case 'x':
                    direction = diff > 0 ? "left" : "right";
                    break;
                case 'y':
                    direction = diff > 0 ? "up" : "down";
                    break;
                case 'z':
                    direction = diff > 0 ? "forward" : "backward";
                    break;
            }


            int tl = toleranceLevel(diff, tols);

            // track worst (lowest number = worse)
            if (tl < worstTol) {
                worstTol = tl;
            }

            switch (tl) {
                case 1:
                    result.append(MessageFormat.format(BadOutputFormat, poseLandmarkNames[i], direction))
                            .append("\n");
                    break;
                case 2:
                    result.append(MessageFormat.format(GoodOutputFormat, poseLandmarkNames[i], direction))
                            .append("\n");
                    break;
            }
        }

        return new ParseResult(result.toString(), worstTol);
    }

    // return appropriate tolerance level indexes
    private Integer toleranceLevel(double diff, Double[] tols) {
        double abs = Math.abs(diff);

        if (abs > tols[0]) {
            return 0; // very bad
        } else if (abs > tols[1]) {
            return 1; // bad
        } else if (abs > tols[2]) {
            return 2; // good
        } else {
            return 3; // perfect
        }
    }
}
