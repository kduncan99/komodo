/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib.misc;

import com.kadware.em2200.baselib.Word36;

/**
 * An extension of Word36 which describes an indicator key register
 */
public class IndicatorKeyRegister extends Word36 {

    /**
     * Standard constructor
     */
    public IndicatorKeyRegister(
    ) {
    }

    /**
     * Initial value constructor
     * <p>
     * @param value
     */
    public IndicatorKeyRegister(
        final long value
    ) {
        super(value);
    }

    /**
     * Getter
     * <p>
     * @return
     */
    public long getShortStatusField(
    ) {
        return getS1();
    }

    /**
     * Getter
     * <p>
     * @return
     */
    public long getMidInstructionDescription(
    ) {
        return getS2() >> 3;
    }

    /**
     * Getter
     * <p>
     * @return
     */
    public long getPendingInterruptInformation(
    ) {
        return getS2() & 07;
    }

    /**
     * Getter
     * <p>
     * @return
     */
    public long getInterruptClassField(
    ) {
        return getS3();
    }

    /**
     * Getter
     * <p>
     * @return
     */
    public long getAccessKey(
    ) {
        return getH2();
    }

    /**
     * Getter
     * <p>
     * @return
     */
    public boolean getInstructionInF0(
    ) {
        return (getMidInstructionDescription() & 04) != 0;
    }

    /**
     * Getter
     * <p>
     * @return
     */
    public boolean getExecuteRepeatedInstruction(
    ) {
        return (getMidInstructionDescription() & 02) != 0;
    }

    /**
     * Getter
     * <p>
     * @return
     */
    public boolean getBreakpointRegisterMatchCondition(
    ) {
        return (getPendingInterruptInformation() & 04) != 0;
    }

    /**
     * Getter
     * <p>
     * @return
     */
    public boolean getSoftwareBreak(
    ) {
        return (getPendingInterruptInformation() & 02) != 0;
    }

    /**
     * Setter
     * <p>
     * @param value
     */
    public void setShortStatusField(
        final long value
    ) {
        setS1(value);
    }

    /**
     * Setter
     * <p>
     * @param value
     */
    public void setMidInstructionDescription(
        final long value
    ) {
        _value = (_value & 0_770777_777777l) | ((value & 07) << 27);
    }

    /**
     * Setter
     * <p>
     * @param value
     */
    public void setPendingInterruptInformation(
        final long value
    ) {
        _value = (_value & 0_777077_777777l) | ((value & 07) << 24);
    }

    /**
     * Setter
     * <p>
     * @param value
     */
    public void setInterruptClassField(
        final long value
    ) {
        setS3(value);
    }

    /**
     * Setter
     * <p>
     * @param value
     */
    public void setAccessKey(
        final long value
    ) {
        setH2(value);
    }

    /**
     * Setter
     * <p>
     * @param flag
     */
    public void setInstructionInF0(
        final boolean flag
    ) {
        _value &= 0_773777_777777l;
        if (flag) {
            _value |= 0_004000_000000l;
        }
    }

    /**
     * Setter
     * <p>
     * @param flag
     */
    public void setExecuteRepeatedInstruction(
        final boolean flag
    ) {
        _value &= 0_775777_777777l;
        if (flag) {
            _value |= 0_002000_000000l;
        }
    }

    /**
     * Setter
     * <p>
     * @param flag
     */
    public void setBreakpointRegisterMatchCondition(
        final boolean flag
    ) {
        _value &= 0_777377_777777l;
        if (flag) {
            _value |= 0_000400_000000l;
        }
    }

    /**
     * Setter
     * <p>
     * @param flag
     */
    public void setSoftwareBreak(
        final boolean flag
    ) {
        _value &= 0_777577_777777l;
        if (flag) {
            _value |= 0_000200_000000l;
        }
    }

    /**
     * Clear the value to zero
     */
    public void clear(
    ) {
        _value = 0;
    }
}
