/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine;

/**
 * Represents a virtual address for Basic or Extended mode.
 * Be careful with this - we don't want to create and discard lots of these.
 */
public class VirtualAddress {

    // BDT level (which refers to a given bank descriptor table) is relative to a particular
    // arrangement of base register values (generally, this means relative to an addressing environment)...
    // that is to say, level 3 (as an example) could refer to a different BDT for each current activity
    // in the system (including OS activities), and thus to different sets of bank descriptors.
    // Conversely, any number of bank descriptor tables could correspond to *someone's* level 3,
    // and thus, the same holds for all the bank descriptors in that table.
    private short _level;

    // Bank Descriptor Index indicates the nth bank descriptor in the BDT identified by the level value,
    // ranging from 0 (the first) to k-1 (the last, assuming the table has space for k bank descriptors).
    private int _bdi;

    // The (relative) address indicates a particular word within the indicated bank.
    // This value is relative to the actual lower-limit of the bank, which is specified in the bank's descriptor
    // (although possibly shifted right in order to save space).
    // Thus, if the bank's actual lower limit is 01000, a relative address of 01005 would refer to an
    // offset of 05 from the start of the bank (the 6th word).
    // The relative address should fall within the actual (unshifted) lower and upper limits of the bank (inclusive).
    private int _address;

    public VirtualAddress() {
        _level = 0;
        _bdi = 0;
        _address = 0;
    }

    public VirtualAddress(
        final short level,
        final int bdi,
        final int address
    ) {
        _level = (short)(level & 07);
        _bdi = bdi & 077777;
        _address = address & 0777777;
    }

    public VirtualAddress(
        final long composite
    ) {
        fromComposite(composite);
    }

    public int getBankDescriptorIndex() { return _bdi; }
    public short getBankLevel() { return _level; }
    public short getLevel() { return _level; }
    public int getOffset() { return _address; }
    public int getLBDI() { return (_level << 15) | _bdi; }

    public VirtualAddress setBankDescriptorIndex(
        final int bdi
    ) {
        _bdi = bdi & 0_077777;
        return this;
    }

    public VirtualAddress setBankLevel(
        final short level
    ) {
        _level = (short)(level & 0_07);
        return this;
    }

    public VirtualAddress setAddress(
        final int address
    ) {
        _address = address & 0_777777;
        return this;
    }

    /**
     * Converts a composite 36-bit value wrapped in a long integer to discrete values
     */
    public void fromComposite(final long composite) {
        _level = (short)((composite >> 33) & 07);
        _bdi = (int)((composite >> 18) & 077777);
        _address = (int)(composite & 0777777);
    }

    /**
     * Converts discrete values to a composite 36-bit value wrapped in a long integer
     * @return composite 36-bit value
     */
    public long toCompositeValue() {
        long value = (long) (_level) << 33;
        value |= (long) _bdi << 18;
        value |= _address;
        return value;
    }

    public static long toCompositeValue(final short level,
                                        final int bdi,
                                        final int offset) {
        return ((long) (level & 07) << 33) | ((long) (bdi & 077777) << 18) | (offset & 0777777);
    }
}
