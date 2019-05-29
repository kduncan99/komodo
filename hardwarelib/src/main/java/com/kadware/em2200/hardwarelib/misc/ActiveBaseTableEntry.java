/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib.misc;

/**
* Represents an absolute address - this is a composite value which identifies a particular
* MainStorageProcessor, and an offset from the beginning of the storage of that processor
* which identifies a particular word of storage.
*/
public class AbsoluteAddress {

    /**
     * The UPI (Unique Processor Index) value identifying a particular MSP
     */
    public short _upi;

    /**
     * A value corresponding to an offset from the start of that MSP's storage.
     * This value *can* be negative, but it should not be used to access storage until it is adjust upward.
     */
    public int _offset;

    /**
     * Standard constructor
     */
    public AbsoluteAddress(
    ) {
        _upi = 0;
        _offset = 0;
    }

    /**
     * Initial value constructor
     * <p>
     * @param upi
     * @param offset
     */
    public AbsoluteAddress(
        final short upi,
        final int offset
    ) {
        _upi = upi;
        _offset = offset;
    }

    /**
     * Adds another offset to the offset in this object
     * <p>
     * @param offset
     */
    public void addOffset(
        final int offset
    ) {
        _offset += offset;
    }

    /**
     * Generate a hash code based solely on the values of this object
     * <p>
     * @return
     */
    @Override
    public int hashCode(
    ) {
        return (new Integer(_upi)).hashCode() ^ (new Long(_offset)).hashCode();
    }

    /**
     * equals method
     * <p>
     * @param obj
     * <p>
     * @return
     */
    @Override
    public boolean equals(
        final Object obj
    ) {
        if ((obj != null) && (obj instanceof AbsoluteAddress)) {
            AbsoluteAddress comp = (AbsoluteAddress) obj;
            return (_upi == comp._upi) && (_offset == comp._offset);
        } else {
            return false;
        }
    }

    /**
     * Setter
     * <p>
     * @param upi
     * @param offset
     */
    public void set(
        final short upi,
        final int offset
    ) {
        _upi = upi;
        _offset = offset;
    }

    /**
     * Setter
     * <p>
     * @param address
     */
    public void set(
        final AbsoluteAddress address
    ) {
        _upi = address._upi;
        _offset = address._offset;
    }

    @Override
    public String toString(
    ) {
        return String.format("0%o:%012o", _upi, _offset);
    }
}
