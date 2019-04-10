/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib.misc;

import com.kadware.em2200.baselib.Word36;

/**
* Represents a virtual address for Basic or Extended mode.
*/
public class VirtualAddress extends Word36 {

    //???? need unit tests for this
    /**
     * Standard constructor
     */
    public VirtualAddress(
    ) {
    }

    /**
     * Initial value constructor
     * <p>
     * @param value
     */
    public VirtualAddress(
        final long value
    ) {
        super(value);
    }

    public VirtualAddress(
        final boolean execFlag,
        final boolean levelFlag,
        final short bdIndex,
        final int offset
    ) {
        super(getValue(execFlag, levelFlag, bdIndex, offset));
    }

    /**
     * Extended mode initial value constructor
     * <p>
     * @param level
     * @param bdIndex
     * @param offset
     */
    public VirtualAddress(
        final byte level,
        final short bdIndex,
        final int offset
    ) {
        super(getValue(level, bdIndex, offset));
    }

    /**
     * Getter
     * <p>
     * @return
     */
    public long getBankDescriptorIndex(
    ) {
        return getH1() & 077777;
    }

    /**
     * Getter
     * <p>
     * @return
     */
    public long getLevel(
    ) {
        return getW() >> 33;
    }

    /**
     * Getter
     * <p>
     * @return
     */
    public long getOffset(
    ) {
        return getH2();
    }

    /**
     * Translates basic mode E/L flags to extended mode bank level (see 4.6.3.1 in docs)
     * <p>
     * @param execFlag
     * @param levelFlag
     * <p>
     * @return
     */
    public static byte translateBasicToExtended(
        final boolean execFlag,
        final boolean levelFlag
    ) {
        if (execFlag) {
            return levelFlag ? (byte)0 : (byte)2;
        } else {
            return levelFlag ? (byte)6 : (byte)4;
        }
    }

    /**
     * Converts discrete values to a composite 36-bit value wrapped in a long integer
     * <p>
     * @param level
     * @param bdIndex
     * @param offset
     * <p>
     * @return
     */
    public static long getValue(
        final byte level,
        final short bdIndex,
        final int offset
    ) {
        long value = (long)(level & 07) << 33;
        value |= (long)(bdIndex & 077777) << 18;
        value |= (offset & 0777777);
        return value;
    }

    /**
     * Converts discrete values to a composite 36-bit value wrapped in a long integer
     * <p>
     * @param execFlag
     * @param levelFlag
     * @param bdIndex
     * @param offset
     * <p>
     * @return
     */
    public static long getValue(
        final boolean execFlag,
        final boolean levelFlag,
        final short bdIndex,
        final int offset
    ) {
        long value = (long)(translateBasicToExtended(execFlag, levelFlag) & 07) << 33;
        value |= (long)(bdIndex & 077777) << 18;
        value |= (offset & 0777777);
        return value;
    }

    /**
     * Sets the BDI portion of this value
     * <p>
     * @param bdIndex
     */
    public void setBankDescriptorIndex(
        final int bdIndex
    ) {
        setH1((getH1() & 0700000) | (bdIndex & 077777));
    }

    /**
     * Sets the level portion of this value
     * <p>
     * @param level
     */
    public void setLevel(
        final byte level
    ) {
        setH1((getH1() & 077777) | ((level & 07) << 15));
    }

    /**
     * Sets the offset portion of this value
     * <p>
     * @param offset
     */
    public void setOffset(
        final int offset
    ) {
        setH2(offset);
    }
}
