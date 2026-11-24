package com.scholarbridge.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DOIValidatorTest {
    private final DOIValidator validator = new DOIValidator();

    @Test
    void acceptsValidDOI() {
        assertTrue(validator.isValidFormat("10.1000/182"));
    }

    @Test
    void rejectsInvalidDOI() {
        assertFalse(validator.isValidFormat("doi:not-a-doi"));
        assertFalse(validator.isValidFormat(""));
        assertFalse(validator.isValidFormat(null));
    }

    @Test
    void normalizesDOIUrls() {
        assertEquals("10.1234/example", validator.normalize("https://doi.org/10.1234/example"));
    }
}
