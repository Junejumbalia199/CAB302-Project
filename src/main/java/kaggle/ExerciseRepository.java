package kaggle;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Disk-cached Kaggle megaGymDataset. Downloads on first call, then reads local copy.
 * Hand-rolled CSV parser (handles quoted fields, no embedded newlines).
 */
public final class ExerciseRepository {

    private static final String OWNER     = "niharika41298";
    private static final String DATASET   = "gym-exercise-data";
    private static final String FILE_NAME = "megaGymDataset.csv";

    private static final Path CACHE = Paths.get("data", "exercises.csv");

    private List<ExerciseDataset> cache;

    /** Downloads (if needed) and parses the CSV. Idempotent and thread-safe. */
    public synchronized List<ExerciseDataset> ensureLoaded() throws KaggleException {
        if (cache != null) return cache;

        if (!Files.exists(CACHE)) {
            KaggleClient client = new KaggleClient(KaggleConfig.load());
            client.downloadFile(OWNER, DATASET, FILE_NAME, CACHE);
        }

        try {
            cache = Collections.unmodifiableList(parseCsv(CACHE));
        } catch (IOException e) {
            throw new KaggleException(
                    "Failed to parse cached dataset at " + CACHE + ": " + e.getMessage(), e);
        }
        return cache;
    }

    /** Loaded rows, or empty list if ensureLoaded() hasn't run. */
    public List<ExerciseDataset> all() {
        return cache == null ? List.of() : cache;
    }

    // ── CSV parsing ───────────────────────────────────────────────────────────

    private static List<ExerciseDataset> parseCsv(Path path) throws IOException {
        List<ExerciseDataset> rows = new ArrayList<>();
        try (BufferedReader r = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String headerLine = r.readLine();
            if (headerLine == null) return rows;

            // Look up by header name to survive column reordering.
            List<String> headers = splitCsvRow(headerLine);
            int iTitle = headers.indexOf("Title");
            int iDesc  = headers.indexOf("Desc");
            int iType  = headers.indexOf("Type");
            int iBody  = headers.indexOf("BodyPart");
            int iEquip = headers.indexOf("Equipment");
            int iLevel = headers.indexOf("Level");

            String line;
            while ((line = r.readLine()) != null) {
                if (line.isBlank()) continue;
                List<String> cols = splitCsvRow(line);

                ExerciseDataset row = new ExerciseDataset(
                        get(cols, iTitle),
                        get(cols, iDesc),
                        get(cols, iType),
                        get(cols, iBody),
                        get(cols, iEquip),
                        get(cols, iLevel)
                );
                if (row.isValid()) rows.add(row);
            }
        }
        return rows;
    }

    /** Safe column lookup; returns "" when index is out of range. */
    private static String get(List<String> cols, int i) {
        if (i < 0 || i >= cols.size()) return "";
        return cols.get(i);
    }

    /** Splits one CSV line. Handles "" escapes inside quoted fields. */
    private static List<String> splitCsvRow(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                // "" inside a quoted field = literal quote.
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                out.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        out.add(current.toString());
        return out;
    }
}
