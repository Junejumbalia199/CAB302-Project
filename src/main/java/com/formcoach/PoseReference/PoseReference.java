package com.formcoach.poseanalysis;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Loads the pre-computed joint angles and raw landmark coordinates for a given
 * exercise from the bundled dataset CSVs. Everything lives in the classpath at
 * src/main/resources/assets/pose_data/ so no network or credentials are needed.
 *
 * Each ReferenceRow holds the 7 joint angles used for finding the best pose
 * match, plus the full xyz coordinate arrays needed to generate feedback text.
 */
public final class PoseReference {

    private static final String LABELS_PATH    = "/assets/pose_data/labels.csv";
    private static final String ANGLES_PATH    = "/assets/pose_data/angles.csv";
    private static final String LANDMARKS_PATH = "/assets/pose_data/landmarks.csv";

    // one row per matching reference frame, angles and xyz aligned by index
    private final List<ReferenceRow> rows;

    public PoseReference(String exerciseName) {
        rows = Collections.unmodifiableList(load(exerciseName));
    }

    public List<ReferenceRow> getRows() {
        return rows;
    }

    // a single reference pose frame - angles for matching, xyz for feedback
    public record ReferenceRow(double[] angles, Double[] x, Double[] y, Double[] z) {}

    private static List<ReferenceRow> load(String exerciseName) {
        String prefix = toLabelPrefix(exerciseName);
        if (prefix == null) return List.of();

        Set<Integer> matchingIds = loadMatchingIds(prefix);
        if (matchingIds.isEmpty()) return List.of();

        Map<Integer, double[]>  angleMap    = loadAngleMap(matchingIds);
        Map<Integer, Double[][]> landmarkMap = loadLandmarkMap(matchingIds);

        // build the combined list in a consistent order
        List<Integer> sortedIds = new ArrayList<>(matchingIds);
        Collections.sort(sortedIds);

        List<ReferenceRow> result = new ArrayList<>();
        for (int id : sortedIds) {
            double[]   angles = angleMap.get(id);
            Double[][] xyz    = landmarkMap.get(id);
            // only include rows where both files provided data
            if (angles != null && xyz != null) {
                result.add(new ReferenceRow(angles, xyz[0], xyz[1], xyz[2]));
            }
        }
        return result;
    }

    // maps the display name from the selection page to the label prefix in the CSV
    static String toLabelPrefix(String exerciseName) {
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

    // reads angles.csv and returns the 7-angle array for each matching pose_id
    private static Map<Integer, double[]> loadAngleMap(Set<Integer> ids) {
        Map<Integer, double[]> map = new HashMap<>();
        try (InputStream is = PoseReference.class.getResourceAsStream(ANGLES_PATH)) {
            if (is == null) {
                System.err.println("[form] angles.csv not found - check src/main/resources/assets/pose_data/");
                return map;
            }
            BufferedReader r = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            r.readLine(); // skip header
            String line;
            while ((line = r.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                // columns: pose_id, angle0 ... angle6
                String[] parts = line.split(",", -1);
                if (parts.length < 8) continue;
                try {
                    int id = Integer.parseInt(parts[0].trim());
                    if (!ids.contains(id)) continue;
                    double[] angles = new double[7];
                    for (int i = 0; i < 7; i++) {
                        angles[i] = Double.parseDouble(parts[i + 1].trim());
                    }
                    map.put(id, angles);
                } catch (NumberFormatException ignored) {}
            }
        } catch (IOException e) {
            System.err.println("[form] failed to read angles.csv: " + e.getMessage());
        }
        return map;
    }

    // reads landmarks.csv and returns {x[], y[], z[]} arrays for each matching pose_id
    private static Map<Integer, Double[][]> loadLandmarkMap(Set<Integer> ids) {
        Map<Integer, Double[][]> map = new HashMap<>();
        try (InputStream is = PoseReference.class.getResourceAsStream(LANDMARKS_PATH)) {
            if (is == null) {
                System.err.println("[form] landmarks.csv not found - check src/main/resources/assets/pose_data/");
                return map;
            }
            BufferedReader r = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            r.readLine(); // skip header
            String line;
            while ((line = r.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                // columns: pose_id, then x,y,z repeated 33 times (100 total)
                String[] parts = line.split(",", -1);
                if (parts.length < 100) continue;
                try {
                    int id = Integer.parseInt(parts[0].trim());
                    if (!ids.contains(id)) continue;
                    Double[] x = new Double[33];
                    Double[] y = new Double[33];
                    Double[] z = new Double[33];
                    for (int i = 0; i < 33; i++) {
                        x[i] = Double.parseDouble(parts[1 + i * 3].trim());
                        y[i] = Double.parseDouble(parts[2 + i * 3].trim());
                        z[i] = Double.parseDouble(parts[3 + i * 3].trim());
                    }
                    map.put(id, new Double[][]{x, y, z});
                } catch (NumberFormatException ignored) {}
            }
        } catch (IOException e) {
            System.err.println("[form] failed to read landmarks.csv: " + e.getMessage());
        }
        return map;
    }
}