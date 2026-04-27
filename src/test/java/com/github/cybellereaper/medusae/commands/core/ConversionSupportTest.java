package com.github.cybellereaper.medusae.commands.core;

import com.github.cybellereaper.medusae.commands.core.exception.ResolutionException;
import com.github.cybellereaper.medusae.commands.core.resolve.ConversionSupport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConversionSupportTest {
    @Test
    void parsesRawStringsWithBoundaryValues() {
        assertEquals(Integer.MIN_VALUE, ConversionSupport.parseInt(Integer.toString(Integer.MIN_VALUE)));
        assertEquals(Integer.MAX_VALUE, ConversionSupport.parseInt(Integer.toString(Integer.MAX_VALUE)));
        assertNull(ConversionSupport.parseInt(Long.toString((long) Integer.MAX_VALUE + 1L)));

        assertEquals(Long.MIN_VALUE, ConversionSupport.parseLong(Long.toString(Long.MIN_VALUE)));
        assertEquals(Long.MAX_VALUE, ConversionSupport.parseLong(Long.toString(Long.MAX_VALUE)));
        assertNull(ConversionSupport.parseLong("not-a-number"));

        assertEquals(1.25D, ConversionSupport.parseDouble("1.25"));
        assertNull(ConversionSupport.parseDouble(""));
        assertTrue(ConversionSupport.parseBooleanStrict("TrUe"));
        assertFalse(ConversionSupport.parseBooleanStrict("false"));
        assertNull(ConversionSupport.parseBooleanStrict("yes"));
        assertFalse(ConversionSupport.parseBooleanLenient("yes"));
    }

    @Test
    void normalizesEntityIdsFromStrings() {
        assertEquals("123", ConversionSupport.normalizeEntityId("123"));
        assertEquals("abc", ConversionSupport.normalizeEntityId(" abc "));
        assertNull(ConversionSupport.normalizeEntityId("   "));
        assertNull(ConversionSupport.normalizeEntityId(null));
    }

    @Test
    void convertsScalarsAndThrowsResolutionErrorsForMalformedValues() {
        assertEquals(10, ConversionSupport.convertScalar("10", int.class, "count"));
        assertEquals(10L, ConversionSupport.convertScalar("10", long.class, "count"));
        assertEquals(2.0D, ConversionSupport.convertScalar("2", double.class, "ratio"));
        assertEquals(false, ConversionSupport.convertScalar("not-true", boolean.class, "enabled"));

        ResolutionException malformed = assertThrows(ResolutionException.class,
                () -> ConversionSupport.convertScalar("x", int.class, "count"));
        assertTrue(malformed.getMessage().contains("Failed to convert value 'count': x"));
    }
}
