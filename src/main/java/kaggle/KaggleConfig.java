package kaggle;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Kaggle API credentials. Resolved from (in order): env vars, $KAGGLE_CONFIG_DIR/kaggle.json,
 * ~/.kaggle/kaggle.json. Regex parser — no JSON dep needed for two fields.
 */
public final class KaggleConfig {

    private static final Pattern USERNAME_PATTERN =
            Pattern.compile("\"username\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern KEY_PATTERN =
            Pattern.compile("\"key\"\\s*:\\s*\"([^\"]+)\"");

    private static final String SETUP_HINT =
            "Kaggle credentials not found.\n" +
            "  1. Go to https://www.kaggle.com/settings/account\n" +
            "  2. Click \"Create New API Token\"\n" +
            "  3. Save the downloaded kaggle.json to:\n" +
            "       " + System.getProperty("user.home") + "/.kaggle/kaggle.json\n" +
            "  (or set KAGGLE_USERNAME + KAGGLE_KEY env vars)";

    private final String username;
    private final String key;

    private KaggleConfig(String username, String key) {
        this.username = username;
        this.key      = key;
    }

    /**
     * Returns the Kaggle account username.
     * @return username string
     */
    public String username() { return username; }

    /**
     * Returns the Kaggle API key.
     * @return API key string
     */
    public String key()      { return key; }

    /**
     * Resolves and returns Kaggle credentials. Checks environment variables first,
     * then falls back to {@code kaggle.json} on disk.
     * @return resolved credentials
     * @throws KaggleException if no credentials can be found or the JSON is malformed
     */
    public static KaggleConfig load() throws KaggleException {
        // Env vars first.
        String envUser = System.getenv("KAGGLE_USERNAME");
        String envKey  = System.getenv("KAGGLE_KEY");
        if (isNonEmpty(envUser) && isNonEmpty(envKey)) {
            return new KaggleConfig(envUser, envKey);
        }

        // Fall back to kaggle.json on disk.
        Path candidate = resolveJsonPath();
        if (!Files.exists(candidate)) {
            throw new KaggleException(SETUP_HINT);
        }

        String raw;
        try {
            raw = Files.readString(candidate);
        } catch (IOException e) {
            throw new KaggleException("Could not read " + candidate + ": " + e.getMessage(), e);
        }

        String user = firstMatch(USERNAME_PATTERN, raw);
        String k    = firstMatch(KEY_PATTERN, raw);
        if (user == null || k == null) {
            throw new KaggleException(
                    "Malformed kaggle.json at " + candidate +
                    " — expected {\"username\":\"...\",\"key\":\"...\"}");
        }
        return new KaggleConfig(user, k);
    }

    // ── internals ─────────────────────────────────────────────────────────────

    private static Path resolveJsonPath() {
        String override = System.getenv("KAGGLE_CONFIG_DIR");
        if (isNonEmpty(override)) {
            return Paths.get(override, "kaggle.json");
        }
        return Paths.get(System.getProperty("user.home"), ".kaggle", "kaggle.json");
    }

    private static String firstMatch(Pattern p, String input) {
        Matcher m = p.matcher(input);
        return m.find() ? m.group(1) : null;
    }

    private static boolean isNonEmpty(String s) {
        return s != null && !s.isBlank();
    }

    // No toString() — keeps API key out of accidental logs.
}
