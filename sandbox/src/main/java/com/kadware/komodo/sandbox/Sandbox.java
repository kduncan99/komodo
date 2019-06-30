package com.kadware.komodo.sandbox;

import java.time.Instant;
import java.time.temporal.ChronoField;

public class Sandbox {

    static long _lastMicros = 0;
    static int _uniqueness = 0;
    static long foo() {
        synchronized (Sandbox.class) {
            Instant i = Instant.now();
            long instantSeconds = i.getLong(ChronoField.INSTANT_SECONDS);
            long microOfSecond = i.getLong(ChronoField.MICRO_OF_SECOND);
            long micros = (instantSeconds * 1000 * 1000) + microOfSecond;
            if (micros != _lastMicros) {
                _lastMicros = micros;
                _uniqueness = 0;
                return _lastMicros << 5;
            } else {
                ++_uniqueness;
                return (_lastMicros << 5) + _uniqueness;
            }
        }
    }

    static public void main(
        final String[] args
    ) {
        for (int a = 0; a < 20; ++a) {
            System.out.println(String.format("micros:%d", foo()));
        }
    }
}
