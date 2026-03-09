/*
 * Copyright (c) 2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec;

import com.bearsnake.komodo.baselib.Word36;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestDateConverter {

    private static final Instant SYSTIME_EPOCH = Instant.parse("1899-12-31T00:00:00.00Z");

    @Test
    public void testGetSingleWordTime() {
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        long expected = java.time.Duration.between(SYSTIME_EPOCH, now).getSeconds();
        assertEquals(expected, DateConverter.getSingleWordTime(now));
    }

    @Test
    public void testFromSingleWordTime() {
        long seconds = 123456789L;
        Instant expected = SYSTIME_EPOCH.plus(seconds, ChronoUnit.SECONDS);
        assertEquals(expected, DateConverter.fromSingleWordTime(seconds));
    }

    @Test
    public void testSingleWordTimeRoundTrip() {
        Instant original = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        long word = DateConverter.getSingleWordTime(original);
        Instant result = DateConverter.fromSingleWordTime(word);
        assertEquals(original, result);
    }

    @Test
    public void testGetModifiedSingleWordTime() {
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        long base = DateConverter.getSingleWordTime(now);
        long expected = base | 0_400000_000000L;
        assertEquals(expected, DateConverter.getModifiedSingleWordTime(now));
    }

    @Test
    public void testFromModifiedSingleWordTime() {
        long word = 0_400000_123456L;
        long base = word & 0_177777_777777L;
        Instant expected = SYSTIME_EPOCH.plus(base, ChronoUnit.SECONDS);
        assertEquals(expected, DateConverter.fromModifiedSingleWordTime(word));
    }

    @Test
    public void testModifiedSingleWordTimeRoundTrip() {
        Instant original = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        long word = DateConverter.getModifiedSingleWordTime(original);
        Instant result = DateConverter.fromModifiedSingleWordTime(word);
        assertEquals(original, result);
    }

    @Test
    public void testGetDoubleWordTime() {
        Instant instant = SYSTIME_EPOCH.plus(1, ChronoUnit.NANOS);
        long[] result = DateConverter.getDoubleWordTime(instant);
        assertEquals(0L, result[0]);
        assertEquals(1L, result[1]);

        // Test a larger value
        // 1 second = 1,000,000,000 nanos
        instant = SYSTIME_EPOCH.plus(1, ChronoUnit.SECONDS);
        result = DateConverter.getDoubleWordTime(instant);
        long expectedNanos = 1_000_000_000L;
        assertEquals(expectedNanos >> 36, result[0]);
        assertEquals(expectedNanos & Word36.BIT_MASK, result[1]);
    }

    @Test
    public void testFromDoubleWordTime() {
        long msWord = 1L;
        long lsWord = 2L;
        long expectedNanos = (msWord << 36) | (lsWord & Word36.BIT_MASK);
        Instant expected = SYSTIME_EPOCH.plus(expectedNanos, ChronoUnit.NANOS);
        assertEquals(expected, DateConverter.fromDoubleWordTime(msWord, lsWord));
    }

    @Test
    public void testDoubleWordTimeRoundTrip() {
        Instant original = Instant.now();
        long[] words = DateConverter.getDoubleWordTime(original);
        Instant result = DateConverter.fromDoubleWordTime(words);
        // Instant.now() has higher precision than what our double word (nanos) might store if it were different,
        // but here it's nanos vs nanos. However, Duration.between uses nanos.
        // SYSTIME_EPOCH is 1899. Instant.now() is 2026. 127 years.
        // 127 * 365 * 24 * 3600 * 10^9 = 4 * 10^18.
        // 2^64 is ~1.8 * 10^19. So it fits in a long.
        assertEquals(original, result);
    }

    @Test
    public void testFromDoubleWordTimeArray() {
        long[] source = {1L, 2L};
        Instant result = DateConverter.fromDoubleWordTime(source);
        Instant expected = DateConverter.fromDoubleWordTime(1L, 2L);
        assertEquals(expected, result);
    }
}
