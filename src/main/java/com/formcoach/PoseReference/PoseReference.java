package com.formcoach.poseanalysis;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Loads the pre-computed joint angles for a given exercise from the bundled
 * dataset CSVs. Each row holds 7 angles (in degrees) that describe one captured
 * pose frame from the reference data.
 *
 * The CSVs live at src/main/resources/assets/pose_data/ and are read straight
 * from the classpath, so no network or Kaggle credentials are needed.
 */
public final class PoseReference {

    private static final String LABELS_PATH = "/assets/pose_data/labels.csv";
    private static final String ANGLES_PATH = "/assets/pose_data/angles.csv";

    // each element is 7 angles in degrees for one reference pose frame
    private final List<double[]> angles;

    public PoseReference(String exerciseName) {
        angles = Collections.unmodifiableList(load(exerciseName));
    }

    public List<double[]> getAngles() {
        return angles;
    }

    private static List<double[]> load(String exerciseName) {
        String prefix = toLabelPrefix(exerciseName);
        if (prefix == null) return List.of();

        Set<Integer> matchingIds = loadMatchingIds(prefix);
        if (matchingIds.isEmpty()) return List.of();

        return loadAnglesForIds(matchingIds);
    }

    // maps the display name from the selection page to the label prefix in the CSV
    private static String toLabelPrefix(String exerciseName) {
        if (exerciseName == null) return null;
        return switch (exerciseName.toLowerCase()) {
            case "push-ups" -> "pushups";
            case "sit-ups"  -> "situp";
            case "squats"   -> "squats";
            default         -> null;
        };
    }

    // reads labels.csv and collects pose_ids whose label starts with the given prefix
    private static Set<Integer> loadMatchingIds(String prefix) {
        Set<Integer> ids = new HashSet<>();
        try (InputStream is = PoseReference.class.getResourceAsStream(LABELS_PATH)) {
            if (is == null) {
                System.err.println("[form] labels.csv not found - check src/main/resources/assets/pose_data/");
                return ids;
            }
            BufferedReader r = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            r.readLine(); // skip header
            String line;
            while ((line = r.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                int comma = line.indexOf(',');
                if (comma < 0) continue;
                String label = line.substring(comma + 1).trim();
                if (label.startsWith(prefix)) {
                    try {
                        ids.add(Integer.parseInt(line.substring(0, comma).trim()));
                    } catch (NumberFormatException ignored) {}
                }
            }
        } catch (IOException e) {
            System.err.println("[form] failed to read labels.csv: " + e.getMessage());
        }
        return ids;
    }

    // reads angles.csv and returns the 7-angle rows that match the given pose_ids
    private static List<double[]> loadAnglesForIds(Set<Integer> ids) {
        List<double[]> result = new ArrayList<>();
        try (InputStream is = PoseReference.class.getResourceAsStream(ANGLES_PATH)) {
            if (is == null) {
                System.err.println("[form] angles.csv not found - check src/main/resources/assets/pose_data/");
                return result;
            }
            BufferedReader r = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            r.readLine(); // skip header
            String line;
            while ((line = r.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                // columns: pose_id, angle0, angle1, ... angle6
                String[] parts = line.split(",", -1);
                if (parts.length < 8) continue;
                try {
                    int id = Integer.parseInt(parts[0].trim());
                    if (!ids.contains(id)) continue;
                    double[] row = new double[7];
                    for (int i = 0; i < 7; i++) {
                        row[i] = Double.parseDouble(parts[i + 1].trim());
                    }
                    result.add(row);
                } catch (NumberFormatException ignored) {}
            }
        } catch (IOException e) {
            System.err.println("[form] failed to read angles.csv: " + e.getMessage());
        }
        return result;
    }
}