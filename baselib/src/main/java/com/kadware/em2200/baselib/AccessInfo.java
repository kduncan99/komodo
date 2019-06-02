/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.baselib;

/**
 * Describes a ring/domain combination
 */
public class AccessInfo {

    public final long _domain;
    public final int _ring;

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
        _ring = (int)(value >> 16) & 0x3;
        _domain = value & 0xFFFF;
    }

    /**
     * Initial value constructor
     * @param ring ring value
     * @param domain domain value
     */
    public AccessInfo(
        final int ring,
        final long domain
    ) {
        _ring = ring & 03;
        _domain = domain & 0xFFFF;
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

    /**
     * Equality check
     * @param obj comparison objectc
     * @return true if objects are equal
     */
    @Override
    public boolean equals(
        final Object obj
    ) {
        if (obj instanceof AccessInfo) {
            AccessInfo aiObj = (AccessInfo) obj;
            return (_domain == aiObj._domain) && (_ring == aiObj._ring);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return (int) ((_ring << 16) | _domain);
    }

    @Override
    public String toString() {
        return String.format("%d:%d", _ring, _domain);
    }
}
