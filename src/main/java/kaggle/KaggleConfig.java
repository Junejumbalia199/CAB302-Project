package kaggle;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Finds my Kaggle API credentials.
 *
 * I check three places in this order (first one wins):
 *   1. KAGGLE_USERNAME + KAGGLE_KEY env vars — handy for CI or if I don't
 *      want a creds file sitting on disk.
 *   2. $KAGGLE_CONFIG_DIR/kaggle.json — in case I've overridden the location.
 *   3. ~/.kaggle/kaggle.json — where the official Kaggle CLI puts it.
 *
 * I deliberately didn't pull in Jackson/Gson just to parse this file. It's
 * always got the same two fields so a tiny regex does the job. If the file
 * is weird I'd rather throw than quietly misread it.
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

    public String username() { return username; }
    public String key()      { return key; }

    /** Find my credentials and return them, or blow up with a hint if I can't. */
    public static KaggleConfig load() throws KaggleException {
        // Try env vars first. I like this path because it means I don't
        // have to leave a creds file on disk, and CI just sets them for me.
        String envUser = System.getenv("KAGGLE_USERNAME");
        String envKey  = System.getenv("KAGGLE_KEY");
        if (isNonEmpty(envUser) && isNonEmpty(envKey)) {
            return new KaggleConfig(envUser, envKey);
        }

        // No env vars — fall back to the kaggle.json file on disk.
        Path candidate = resolveJsonPath();
        if (!Files.exists(candidate)) {
            throw new KaggleException(SETUP_HINT);
        }

        String raw;
        try {
            raw = Files.readString(candidate);
        } catch (IOException e) {
            throw new KaggleException(
                    "Could not read " + candidate + ": " + e.getMessage(), e);
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

    // No toString() on purpose. The last thing I want is my API key landing
    // in a log file somewhere because someone printed a KaggleConfig.
}
