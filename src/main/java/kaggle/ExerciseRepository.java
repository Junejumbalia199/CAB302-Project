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
 * Wraps the Kaggle megaGymDataset for the rest of the app. First time it's
 * called, I download data/exercises.csv from Kaggle. After that the file
 * stays on disk and I just read it straight back — no need to hit Kaggle
 * on every launch.
 *
 * I wrote my own CSV parser instead of pulling in OpenCSV. This dataset
 * only uses the basic quoted-field case (descriptions with commas wrapped
 * in double quotes) so the hand-rolled version is fine. If I ever swap
 * datasets and hit one with newlines inside quoted fields, I'll bring in
 * OpenCSV then — no point paying for it now.
 */
public final class ExerciseRepository {

    // I pinned the exact dataset + filename on purpose. I'd rather find out
    // loudly on the next run if the upstream owner renames something than
    // silently start loading a different file.
    private static final String OWNER     = "niharika41298";
    private static final String DATASET   = "gym-exercise-data";
    private static final String FILE_NAME = "megaGymDataset.csv";

    private static final Path CACHE = Paths.get("data", "exercises.csv");

    private List<ExerciseDataset> cache;

    /**
     * Makes sure the CSV is on disk and parsed into memory. I made this
     * synchronized and idempotent so I can call it from the startup warmup
     * thread and from any screen that needs the data without worrying
     * about double-loading.
     */
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

    /** Whatever I've loaded so far. Empty list if ensureLoaded() hasn't run. */
    public List<ExerciseDataset> all() {
        return cache == null ? List.of() : cache;
    }

    // ── CSV parsing ───────────────────────────────────────────────────────────

    private static List<ExerciseDataset> parseCsv(Path path) throws IOException {
        List<ExerciseDataset> rows = new ArrayList<>();
        try (BufferedReader r = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String headerLine = r.readLine();
            if (headerLine == null) return rows;

            // Look up columns by name so I'm not relying on a specific order.
            // If the upstream CSV ever reorders columns I'd rather keep working.
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

    /** Small helper so I don't have to null-check every column lookup. */
    private static String get(List<String> cols, int i) {
        if (i < 0 || i >= cols.size()) return "";
        return cols.get(i);
    }

    /**
     * My mini CSV splitter. Handles double-quoted fields (where a comma is
     * part of the value, like a description). Doesn't handle newlines
     * inside a quoted field — see the class comment.
     */
    private static List<String> splitCsvRow(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                // Inside a quoted field, "" means a literal quote — emit one
                // and skip the second.
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
