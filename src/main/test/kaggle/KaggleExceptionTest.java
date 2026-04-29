package kaggle;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for KaggleException, verifies both constructors round-trip the
 * data they're handed. Important because the cause chain is what tells
 * callers (and stack traces) what actually went wrong upstream.
 */
class KaggleExceptionTest {

    /** Feature 4: exception preserves message and cause through constructors. */
    @Test
    void preservesMessageAndCause() {
        // Two-arg constructor: both message and cause must survive.
        Throwable root = new RuntimeException("network down");
        KaggleException wrapped = new KaggleException("kaggle search failed", root);
        assertEquals("kaggle search failed", wrapped.getMessage());
        assertSame(root, wrapped.getCause());

        // One-arg constructor: message survives, no cause attached.
        KaggleException msgOnly = new KaggleException("just a message");
        assertEquals("just a message", msgOnly.getMessage());
        assertNull(msgOnly.getCause());

        // Type sanity: it's a checked exception (throws clauses depend on this).
        assertTrue(Exception.class.isAssignableFrom(KaggleException.class));
        assertFalse(RuntimeException.class.isAssignableFrom(KaggleException.class));
    }
}
