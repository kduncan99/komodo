package com.kadware.komodo.hardwarelib;

import com.kadware.komodo.baselib.Word36;

/**
 * Represents a virtual address for Basic or Extended mode.
 */
class VirtualAddress extends Word36 {

    public VirtualAddress()         {}
    VirtualAddress(long value)      { super(value); }

    /**
     * Basic mode initial value constructor
     * @param execFlag true for exec bank, else false
     * @param levelFlag level portion of the bank name, true for 1, false for 0
     * @param bdIndex bank descriptor index 0:07777
     * @param offset offset 0:0777777 - represents an offset from the start of the bank
     */
    public VirtualAddress(
        final boolean execFlag,
        final boolean levelFlag,
        final int bdIndex,
        final int offset
    ) {
        super(getValue(execFlag, levelFlag, bdIndex, offset));
    }

    /**
     * Extended mode initial value constructor
     * @param level level, 0:7
     * @param bdIndex bank descriptor index 0:077777
     * @param offset offset 0:0777777 - represents an offset from the start of the bank
     */
    public VirtualAddress(
        final int level,
        final int bdIndex,
        final int offset
    ) {
        super(getValue(level, bdIndex, offset));
    }

    public int getBankDescriptorIndex() { return (int) (getH1() & 077777); }
    public int getLevel()               { return (int) (getW() >> 33); }
    public int getOffset()              { return (int) getH2(); }

    /**
     * Converts discrete values to a composite 36-bit value wrapped in a long integer
     * @param level level, 0:7
     * @param bdIndex bank descriptor index 0:077777
     * @param offset offset 0:0777777 - represents an offset from the start of the bank
     * @return composite 36-bit value
     */
    public static long getValue(
        final int level,
        final int bdIndex,
        final int offset
    ) {
        long value = (long)(level & 07) << 33;
        value |= (long)(bdIndex & 077777) << 18;
        value |= (offset & 0777777);
        return value;
    }

    /**
     * Converts discrete values to a composite 36-bit value wrapped in a long integer
     * @param execFlag basic mode exec flag
     * @param levelFlag basic mode level flag
     * @param bdIndex bank descriptor index 0:07777
     * @param offset offset 0:0777777 - represents an offset from the start of the bank
     * @return composite 36-bit value
     */
    public static long getValue(
        final boolean execFlag,
        final boolean levelFlag,
        final int bdIndex,
        final int offset
    ) {
        long value = (long)(translateBasicToExtendedLevel(execFlag, levelFlag) & 07) << 33;
        value |= (long)(bdIndex & 07777) << 18;
        value |= (offset & 0777777);
        return value;
    }

    /**
     * Translates basic mode E/L flags to extended mode bank level (see 4.6.3.1 in docs)
     */
    public static int translateBasicToExtendedLevel(
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
     * Translates this L,BDI,OFFSET bank name to E,LS,BDI,OFFSET format
     */
    public long translateToBasicMode() {
        boolean execFlag = true;
        boolean levelSpecFlag = true;
        int bdi = 0;

        if (getBankDescriptorIndex() <= 07777) {
            bdi = getBankDescriptorIndex();
            switch (getLevel()) {
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
               | (bdi << 18)
               | getOffset();
    }

    /**
     * Translates an extended mode bank name to the corresponding basic mode name
     */
    public static long translateToBasicMode(
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
