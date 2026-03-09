/*
 * Copyright (c) 2023-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec;

import com.bearsnake.komodo.baselib.Word36;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Performs various time and date conversions
 */
public class DateConverter {

    private static final Instant SYSTIME_EPOCH = Instant.parse("1899-12-31T00:00:00.00Z");

    public static long[] getDoubleWordTime(
        final Instant instant
    ) {
        // this will be problematic for values which exceed 64 bits
        var dwTrunc = Duration.between(SYSTIME_EPOCH, instant).toNanos();
        var result = new long[2];
        result[0] = (dwTrunc >> 36) & 0x0FFFFFFF;
        result[1] = dwTrunc & Word36.BIT_MASK;
        return result;
    }

    public static long getModifiedSingleWordTime(
        final Instant instant
    ) {
        return getSingleWordTime(instant) | 0_400000_000000L;
    }

    public static long getSingleWordTime(
        final Instant instant
    ) {
        return Duration.between(SYSTIME_EPOCH, instant).toSeconds();
    }

    public static Instant fromDoubleWordTime(
        final long[] source
    ) {
        // this will be problematic for values which exceed 64 bits
        return fromDoubleWordTime(source[0], source[1]);
    }

    public static Instant fromDoubleWordTime(
        final long msWord,
        final long lsWord
    ) {
        // this will be problematic for values which exceed 64 bits
        long composite = (msWord << 36) | (lsWord & 0_777777_777777L);
        return SYSTIME_EPOCH.plus(composite, ChronoUnit.NANOS);
    }

    public static Instant fromModifiedSingleWordTime(
        final long sWord
    ) {
        return SYSTIME_EPOCH.plus(sWord & 0_177777_777777L, ChronoUnit.SECONDS);
    }

    public static Instant fromSingleWordTime(
        final long sword
    ) {
        return SYSTIME_EPOCH.plus(sword, ChronoUnit.SECONDS);
    }
}
