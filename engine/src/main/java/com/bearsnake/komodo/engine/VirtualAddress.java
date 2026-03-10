/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine;

/**
 * Represents a virtual address for Basic or Extended mode.
 * Be careful with this - we don't want to create and discard lots of these.
 */
public class VirtualAddress {

    private short _level;
    private int _bdi;
    private int _offset;

    public VirtualAddress() {
        _level = 0;
        _bdi = 0;
        _offset = 0;
    }

    public VirtualAddress(
        final short level,
        final int bdi,
        final int offset
    ) {
        _level = (short)(level & 07);
        _bdi = bdi & 077777;
        _offset = offset & 0777777;
    }

    public VirtualAddress(
        final long composite
    ) {
        fromComposite(composite);
    }

    public int getBankDescriptorIndex() { return _bdi; }
    public short getBankLevel() { return _level; }
    public short getLevel() { return _level; }
    public int getOffset() { return _offset; }
    public int getLBDI() { return (_level << 15) | _bdi; }

    public VirtualAddress setBankDescriptorIndex(final int bdi) { _bdi = bdi; return this; }
    public VirtualAddress setBankLevel(final short level) { _level = level; return this; }
    public VirtualAddress setOffset(final int offset) { _offset = offset; return this; }

    /**
     * Converts a composite 36-bit value wrapped in a long integer to discrete values
     */
    public void fromComposite(final long composite) {
        _level = (short)((composite >> 33) & 07);
        _bdi = (int)((composite >> 18) & 077777);
        _offset = (int)(composite & 0777777);
    }

    /**
     * Converts discrete values to a composite 36-bit value wrapped in a long integer
     * @return composite 36-bit value
     */
    public long getCompositeValue() {
        long value = (long) (_level) << 33;
        value |= (long) _bdi << 18;
        value |= _offset;
        return value;
    }

    public static long getCompositeValue(final int level,
                                         final int bdi,
                                         final int offset) {
        return ((long) (level & 07) << 33) | ((long) (bdi & 077777) << 18) | (offset & 0777777);
    }

    public static long getCompositeValue(final boolean execFlag,
                                         final boolean levelFlag,
                                         final int bdi,
                                         final int offset) {
        return getCompositeValue(translateBasicToExtendedLevel(execFlag, levelFlag), bdi, offset);
    }

    /**
     * Translates basic mode E/L flags to extended mode bank level (see 4.6.3.1 in docs)
     */
    public static int translateBasicToExtendedLevel(final boolean execFlag,
                                                    final boolean levelFlag) {
        if (execFlag) {
            return levelFlag ? (byte) 0 : (byte) 2;
        } else {
            return levelFlag ? (byte) 6 : (byte) 4;
        }
    }

    /**
     * Translates this object's (extended mode) L,BDI,OFFSET bank name to E,LS,BDI,OFFSET format.
     * Effectively converts an EM VA to BM.
     */
    public long translateToBasicMode() {
        return translateToBasicMode(_level, _bdi, _offset);
    }

    public static long translateToBasicMode(final int bankLevel,
                                            final int bankDescriptorIndex,
                                            final int offset) {
        boolean execFlag = true;
        boolean levelSpecFlag = true;

        if (bankDescriptorIndex <= 07777) {
            switch (bankLevel) {
                case 0:
                    break;

                case 2:
                    levelSpecFlag = false;
                    break;

                case 4:
                    execFlag = false;
                    levelSpecFlag = false;
                    break;

                case 6:
                    execFlag = false;
            }
        }

        return (execFlag ? 0_400000_000000L : 0)
               | (levelSpecFlag ? 0_040000_000000L : 0)
               | ((long) bankDescriptorIndex << 18)
               | offset;
    }

    /**
     * Translates an extended mode bank name to the corresponding basic mode name
     */
    public static long translateToBasicModeName(
        final int bankLevel,
        final int bankDescriptorIndex,
        final int offset
    ) {
        if ((bankDescriptorIndex >= 0) && (bankDescriptorIndex <= 07777)) {
            long result = ((long) (bankDescriptorIndex & 07777) << 18) | offset & 0777777;
            switch (bankLevel) {
                case 0: return result | 0_440000_000000L;
                case 2: return result | 0_400000_000000L;
                case 4: return result;
                case 6: return result | 0_040000_000000L;
            }
        }

        return 0_440000_000000L | (offset & 0777777);
    }
}
