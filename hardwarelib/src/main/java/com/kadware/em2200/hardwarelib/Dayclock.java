/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib;

import java.time.Instant;
import java.time.temporal.ChronoField;

/**
 * Implements an interface to the system time, reporting time in microseconds since epoch.
 * If the system time is to be adjusted from the host time, we store an offset here, and then
 * apply that offset on all future retrievals.
 *
 * This is a singleton.
 */
public class Dayclock {

    private static long _offsetMicros = 0;

    public static long getDayclockMicros() {
        Instant i = Instant.now();
        long instantSeconds = i.getLong(ChronoField.INSTANT_SECONDS);
        long microOfSecond = i.getLong(ChronoField.MICRO_OF_SECOND);
        long systemMicros = (instantSeconds * 1000 * 1000) + microOfSecond;
        return systemMicros + _offsetMicros;
    }

    public static void setDayclockMicros(
        final long value
    ) {
        Instant i = Instant.now();
        long instantSeconds = i.getLong(ChronoField.INSTANT_SECONDS);
        long microOfSecond = i.getLong(ChronoField.MICRO_OF_SECOND);
        long systemMicros = (instantSeconds * 1000 * 1000) + microOfSecond;
        _offsetMicros = value - systemMicros;
    }
}
