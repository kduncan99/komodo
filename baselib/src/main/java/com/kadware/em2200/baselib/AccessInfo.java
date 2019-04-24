/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.baselib;

/**
 * Describes a ring/domain combination
 */
public class AccessInfo {

    private short _domain;
    private byte _ring;

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
     * <p>
     * @param source
     */
    public AccessInfo(
        final AccessInfo source
    ) {
        _domain = source._domain;
        _ring = source._ring;
    }

    /**
     * Initial value constructor
     * <p>
     * @param ring
     * @param domain
     */
    public AccessInfo(
        final byte ring,
        final short domain
    ) {
        _ring = ring;
        _domain = domain;
    }

    /**
     * Getter
     * <p>
     * @return
     */
    public short getDomain(
    ) {
        return _domain;
    }

    /**
     * Getter
     * <p>
     * @return
     */
    public byte getRing(
    ) {
        return _ring;
    }

    /**
     * Retrieves ring/domain into a Word36 value formatted as such:
     *      bits18,19   ring
     *      bits20-35   domain
     * <p>
     * @return
     */
    public long get(
    ) {
        return ((long)(_ring) << 16) | _domain;
    }

    /**
     * Setter
     * <p>
     * @param value
     */
    public void setDomain(
        final short value
    ) {
        _domain = value;
    }

    /**
     * Setter
     * <p>
     * @param value
     */
    public void setRing(
        final byte value
    ) {
        _ring = value;
    }

    /**
     * Sets ring and domain from a single value, where bits 18,19 contain the ring, and bits 20-35 contain the domain.
     * @param value
     */
    public void set(
        final long value
    ) {
        _ring = (byte)((value >> 16) & 0x3);
        _domain = (short)value;
    }

    /**
     * Setter
     * <p>
     * @param info
     */
    public void set(
        final AccessInfo info
    ) {
        _ring = info._ring;
        _domain = info._domain;
    }
}
