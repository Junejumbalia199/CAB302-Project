package com.formcoach.camera;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the static helper inside CameraView.
 * Mocking technique: temporarily override the {@code os.name} system
 * property to simulate each platform, then restore it after, same idea
 * as the MockContactDAO pattern from the unit, just lighter weight
 * because the dependency is a single string.
 */
class CameraViewTest {

    private String originalOsName;

    @BeforeEach
    void saveOsName() {
        originalOsName = System.getProperty("os.name");
    }

    @AfterEach
    void restoreOsName() {
        if (originalOsName != null) {
            System.setProperty("os.name", originalOsName);
        }
    }

    /** Feature 5: OS hint dispatches to platform-specific guidance. */
    @Test
    void osHintReturnsPlatformSpecificMessage() {
        // Windows branch.
        System.setProperty("os.name", "Windows 11");
        String win = CameraView.osHint();
        assertNotNull(win);
        assertTrue(win.toLowerCase().contains("settings"));
        assertTrue(win.toLowerCase().contains("camera"));

        // macOS branch.
        System.setProperty("os.name", "Mac OS X");
        String mac = CameraView.osHint();
        assertTrue(mac.toLowerCase().contains("system settings"));

        // Linux branch.
        System.setProperty("os.name", "Linux");
        String linux = CameraView.osHint();
        assertTrue(linux.contains("/dev/video0"));

        // Fallback branch — unknown OS still returns useful guidance.
        System.setProperty("os.name", "BeOS");
        String fallback = CameraView.osHint();
        assertNotNull(fallback);
        assertFalse(fallback.isBlank());
    }
}
