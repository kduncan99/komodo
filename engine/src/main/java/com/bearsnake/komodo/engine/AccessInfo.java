/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine;

/**
 * Describes a ring/domain combination
 */
public class AccessInfo {

    private int _domain;
    private short _ring;

    public AccessInfo() {
        _domain = 0;
        _ring = 0;
    }

    public AccessInfo(
        final AccessInfo source
    ) {
        _domain = source._domain;
        _ring = source._ring;
    }

    public AccessInfo(
        int domain,
        short ring
    ) {
        _domain = domain;
        _ring = ring;
    }

    public AccessInfo(
        final long value
    ) {
        _ring = (short)((value >> 16) & 0x3);
        _domain = (int)(value & 0xFFFF);
    }

    /**
     * Retrieves ring/domain into a Word36 value formatted as such:
     *      bits18,19   ring
     *      bits20-35   domain
     * @return compositional value
     */
    public long get() {
        return ((long)(_ring) << 16) | _domain;
    }

    public int getDomain() { return _domain; }
    public short getRing() { return _ring; }
    public AccessInfo setDomain(final int domain) { _domain = domain; return this; }
    public AccessInfo setRing(final short ring) { _ring = ring; return this; }

    /**
     * Equality check
     * @param obj comparison objectc
     * @return true if objects are equal
     */
    @Override
    public boolean equals(
        final Object obj
    ) {
        if (obj instanceof AccessInfo ai) {
            return (_domain == ai._domain) && (_ring == ai._ring);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return ((_ring << 16) | _domain);
    }

    @Override
    public String toString() {
        return String.format("%d:%d", _ring, _domain);
    }
}
