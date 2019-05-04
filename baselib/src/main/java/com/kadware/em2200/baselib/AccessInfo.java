/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.baselib;

/**
 * Describes a ring/domain combination
 */
public class AccessInfo {

    public final short _domain;
    public final byte _ring;

    /**
     * Standard constructor
     */
    public AccessInfo(
    ) {
        _domain = 0;
        _ring = 0;
    }

    /**
     * Copy constructor
     * @param source object
     */
    public AccessInfo(
        final AccessInfo source
    ) {
        _domain = source._domain;
        _ring = source._ring;
    }

    /**
     * Sets ring and domain from a single value, where bits 18,19 contain the ring, and bits 20-35 contain the domain.
     * @param value compositional value
     */
    public AccessInfo(
        final long value
    ) {
        _ring = (byte)((value >> 16) & 0x3);
        _domain = (short)value;
    }

    /**
     * Initial value constructor
     * @param ring ring value
     * @param domain domain value
     */
    public AccessInfo(
        final byte ring,
        final short domain
    ) {
        _ring = ring;
        _domain = domain;
    }

    /**
     * Retrieves ring/domain into a Word36 value formatted as such:
     *      bits18,19   ring
     *      bits20-35   domain
     * @return compositional value
     */
    public long get(
    ) {
        return ((long)(_ring) << 16) | _domain;
    }
}
