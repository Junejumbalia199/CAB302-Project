package com.formcoach.textoutputgen;

import java.text.MessageFormat;
import java.util.concurrent.ThreadLocalRandom;

public class textoutputgen {
    // text output composite parts
    private Boolean disableFlavourText;
    private Boolean[] skipIndex = new Boolean[]{true,true,true,true,true,true,true,true,true,true,true,false,
            false,false,false,false,false,true,true,true,true,true,true,false,false,false,false,false,
            false,false,false,true,true};
    private String VBadText = "WARNING. Stop immediately. You may injure yourself.";
    private String BadText = "Your form isn't quite right. Here's where you need to adjust.";
    private String GoodText = "Your form is pretty good, but you can adjust a bit.";
    private String PerfectText = "Your form is perfect!";
    private String BadOutputFormat = "Move your ${posepoint} ${direction}.";
    private String GoodOutputFormat = "Move your ${posepoint} ${direction} a little.";
    private String[] FlavourText = new String[]
            {"Good job!", "Keep it up!", "Great work!", "Keep on improving!", "Don't give up!"};
    private Double[] pushupTols = new Double[]{}; //define tols for vbad, bad, good, vgood
    private Double[] situpTols = new Double[]{};
    private Double[] squatTols = new Double[]{};
    private String[] poseLandmarkNames = new String[]{"nose", "left eye (inner)", "left eye", "left eye (outer)",
    "right eye (inner)", "right eye", "right eye (outer)", "left ear", "right ear", "mouth (left)", "mouth (right)",
    "left shoulder", "right shoulder", "left elbow", "right elbow", "left wrist", "right wrist", "left pinky",
    "right pinky", "left index", "right index", "left thumb", "right thumb", "left hip", "right hip",
    "left knee", "right knee", "left ankle", "right ankle", "left heel", "right heel", "left foot index",
            "right foot index"};

    // eventually going to be a method that takes the coordinate inputs and gets text outputs
    public String output(Integer[] userPose, Integer[] idealPose, String exerciseType) { // will take two inputs, need to check type
        Integer worstTol = 0;
        String result = "";
        Double[] tols = new Double[4];
        Boolean[] skipIndex = new Boolean[33];
        switch (exerciseType) {
            case "Pushup":
                tols = pushupTols;
                break;
            case "Situp":
                tols = situpTols;
                break;
            case "Squat":
                tols = squatTols;
                break;
        }
        // do all the stuff
        // run parseCoordinatesToString
        switch (worstTol) {
            case 0:
                result = VBadText + "%n" + result;
                break;
            case 1:
                result = BadText + "%n" + result;
                break;
            case 2:
                result = GoodText + "%n" + result;
                break;
            case 3:
                result = PerfectText + "%n" + result;
                break;
        }
        Integer rand = ThreadLocalRandom.current().nextInt(0, FlavourText.length + 1);
        return result + "%n" + FlavourText[rand];
    }

    // take two arrays of coordinates and return the tolerances needed, skipping the specified
    // number of indexes off the arrays
    private String parseCoordinatesToString(Double[] userPose, Double[] idealPose, Double[] tols) {
        String result = "";
        String direction = "";
        // fix to take into account up/down, left/right, forward/backward
        for (int i = 0; i < userPose.length; i++) {
            if (skipIndex[i]) {
                continue;
            }
            Integer tl = toleranceLevel(userPose[i], idealPose[i], tols);
            switch (tl) {
                case 0:
                    break;
                case 1:
                    result = result + MessageFormat.format(BadOutputFormat, poseLandmarkNames[i], direction) + "%n";
                    break;
                case 2:
                    result = result + MessageFormat.format(GoodOutputFormat, poseLandmarkNames[i], direction) + "%n";
                    break;
                case 3:
                    break;
            }
        }
        return result;
    }

    // return appropriate tolerance level indexes
    private Integer toleranceLevel(Double a, Double b, Double[] tols) {
        if (a - b > tols[0]) {
            return 0;
        }
        else if (a - b < tols[1]) {
            return 1;
        }
        else if (b - a < tols[2]) {
            return 2;
        }
        else {
            return 3;
        }
    }
}
