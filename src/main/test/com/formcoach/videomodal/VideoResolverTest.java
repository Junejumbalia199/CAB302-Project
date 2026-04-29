package com.formcoach.videomodal;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for VideoResolver, the helper that maps an exercise name to the
 * mp4 path the player loads. Pure functions, no JavaFX, easy to unit test.
 */
class VideoResolverTest {

    /** Feature 1: filename slug derived from arbitrary exercise name. */
    @Test
    void slugifyMapsCommonExerciseNames() {
        // Typical: spaces become hyphens, lowercase normalized.
        assertEquals("pull-ups", VideoResolver.slugify("Pull Ups"));
        // Punctuation gets stripped.
        assertEquals("bench-press", VideoResolver.slugify("Bench Press!"));
        // Leading/trailing whitespace and existing hyphens preserved correctly.
        assertEquals("tri-cep-kickbacks", VideoResolver.slugify("  Tri-cep Kickbacks  "));
        // Underscores normalized to hyphens.
        assertEquals("ab-wheel", VideoResolver.slugify("Ab_Wheel"));
        // Boundary: null input returns empty string, never throws.
        assertEquals("", VideoResolver.slugify(null));
    }

    /** Feature 2: resolve always returns a non-null URL even for unknown names. */
    @Test
    void resolveReturnsUrlEvenForUnknownExercise() {
        // Even a made-up exercise must fall through to a usable URL
        // (classpath generic, or src/ fallback). MediaPlayer must not
        // be handed a null source.
        String url = VideoResolver.resolve("Totally Made Up Exercise");
        assertNotNull(url);
        assertFalse(url.isBlank());
    }
}
