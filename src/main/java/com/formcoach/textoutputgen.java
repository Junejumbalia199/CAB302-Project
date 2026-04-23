package com.formcoach;

import java.util.Dictionary;

public class textoutputgen {
    // text output composite parts
    private Boolean disableFlavourText;
    private String VBadText;
    private String BadText;
    private String GoodText;
    private String PerfectText;
    private String BadOutputFormat;
    private String GoodOutputFormat;
    private String FlavourTextPos1 = "Good job!";
    private String FlavourTextPos2 = "Keep it up!";
    private String FlavourTextPos3 = "Great work!";
    private String FlavourTextNeg1 = "Keep on improving!";
    private String FlavourTextNeg2 = "Don't give up!";
    private Integer[] pushupTols = new Integer[]{0, 1, 2, 3};
    private Integer[] situpTols = new Integer[]{0, 1, 2, 3};
    private Integer[] squatTols = new Integer[]{0, 1, 2, 3};
    private Dictionary<String, Integer> PosePoints; //dictionary to assign pose point indexes from result array to names for output

    // eventually going to be a method that takes the coordinate inputs and gets text outputs
    public String output(Integer[] userPose, Integer[] idealPose, String exerciseType) { // will take two inputs, need to check type
        Integer[] tols = new Integer[4];
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

        for (int i = 0; i < userPose.length; i++) {
            //check pose points array, likely need a double for loop as there's 3 coordinates for each pose point
            //may need extra logic to check
            Integer tl = toleranceLevel(userPose[i], idealPose[i], tols);
            //general idea, iterate through all pose points looking for tolerance levels and construct a result array
        }
        return "";
    }

    private Integer toleranceLevel(Integer a, Integer b, Integer[] tols) {
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
