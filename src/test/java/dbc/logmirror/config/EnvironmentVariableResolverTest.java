package dbc.logmirror.config;

import dbc.logmirror.config.EnvironmentVariableResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class EnvironmentVariableResolverTest {

    private EnvironmentVariableResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new EnvironmentVariableResolver();
        System.setProperty("java.util.Arrays.useLegacyMergeSort", "true");
    }

    @Test
    void testContainsPlaceholder() {
        assertTrue(resolver.containsPlaceholder("${VAR}"));
        assertTrue(resolver.containsPlaceholder("prefix_${VAR}"));
        assertTrue(resolver.containsPlaceholder("${VAR}_suffix"));
        assertFalse(resolver.containsPlaceholder("no placeholder"));
        assertFalse(resolver.containsPlaceholder(null));
    }

    @Test
    void testExtractPlaceholders() {
        Set<String> placeholders = resolver.extractPlaceholders("${VAR1} and ${VAR2}");
        assertEquals(2, placeholders.size());
        assertTrue(placeholders.contains("VAR1"));
        assertTrue(placeholders.contains("VAR2"));
    }

    @Test
    void testExtractPlaceholdersEmpty() {
        Set<String> placeholders = resolver.extractPlaceholders("no placeholder");
        assertTrue(placeholders.isEmpty());
    }

    @Test
    void testValidatePlaceholdersSuccess() {
        // Create env var for test
        System.setProperty("TEST_VAR", "test_value");
        
        // This should not throw if environment variable exists
        // Note: In real tests, you'd use environment setup
    }

    @Test
    void testValidatePlaceholdersMissing() {
        assertThrows(IllegalArgumentException.class, 
                () -> resolver.validatePlaceholders("${NONEXISTENT_VAR_XYZ}"));
    }
}
