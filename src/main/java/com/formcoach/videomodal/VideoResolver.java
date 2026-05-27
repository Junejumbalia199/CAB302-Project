package com.formcoach.videomodal;

import java.io.File;
import java.net.URL;

/**
 * Maps an exercise name to the demo video I want to play for it.
 *
 * My lookup order:
 *   1. Classpath /videos/{slug}.mp4   (the dedicated per-exercise clip)
 *   2. Classpath /pushup.mp4          (the generic fallback I already ship)
 *   3. On-disk   src/pushup.mp4       (last-resort for the dev workflow)
 *
 * "Slug" is just the exercise name lowercased with spaces turned into
 * hyphens and anything non-alphanumeric stripped. So "Pull Ups" becomes
 * "pull-ups", "Bench Press" becomes "bench-press", etc.
 *
 * I designed it this way because the Kaggle dataset has thousands of
 * exercises and I'm never going to have a bespoke clip for every one of
 * them. For the common moves I drop a file into /videos/; everything
 * else gets the generic pushup demo so the screen never looks broken.
 */
public final class VideoResolver {

    private VideoResolver() { /* no-op */ }

    /**
     * Resolves a video URL for the given exercise name. Never returns null.
     * @param exerciseName display name of the exercise (e.g. {@code "Push-ups"})
     * @return a URL string pointing to the best available video for this exercise
     */
    public static String resolve(String exerciseName) {
        // First: try a dedicated file under /videos/.
        String slug = slugify(exerciseName);
        if (!slug.isEmpty()) {
            URL dedicated = VideoResolver.class.getResource("/videos/" + slug + ".mp4");
            if (dedicated != null) {
                return dedicated.toExternalForm();
            }
        }

        // Second: the generic pushup demo on the classpath.
        URL generic = VideoResolver.class.getResource("/pushup.mp4");
        if (generic != null) {
            return generic.toExternalForm();
        }

        // Last resort: the file on disk relative to the working directory.
        // Handy when I'm running something that hasn't staged resources yet.
        return new File("src/pushup.mp4").toURI().toString();
    }

    /**
     * My filename-safe exercise slug: lowercase, spaces -> "-",
     * everything that isn't a-z / 0-9 / "-" gets dropped, collapse
     * runs of "-" and trim leading/trailing ones.
     */
    static String slugify(String name) {
        if (name == null) return "";
        StringBuilder out = new StringBuilder(name.length());
        boolean lastDash = false;
        for (int i = 0; i < name.length(); i++) {
            char c = Character.toLowerCase(name.charAt(i));
            if (c >= 'a' && c <= 'z' || c >= '0' && c <= '9') {
                out.append(c);
                lastDash = false;
            } else if (c == ' ' || c == '-' || c == '_') {
                if (!lastDash && out.length() > 0) {
                    out.append('-');
                    lastDash = true;
                }
            }
            // Everything else: skip.
        }
        // Trim a trailing dash if slug ended with a separator.
        if (out.length() > 0 && out.charAt(out.length() - 1) == '-') {
            out.deleteCharAt(out.length() - 1);
        }
        return out.toString();
    }
}
