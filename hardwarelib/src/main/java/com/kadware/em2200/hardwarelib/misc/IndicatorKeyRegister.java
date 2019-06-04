/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib.misc;

import com.kadware.em2200.baselib.AccessInfo;
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
     */
    public IndicatorKeyRegister(
        final long value
    ) {
        super(value);
    }

    public long getShortStatusField() { return getS1(); }
    public long getMidInstructionDescription() { return getS2() >> 3; }
    public long getPendingInterruptInformation() { return getS2() & 07; }
    public long getInterruptClassField() { return getS3(); }
    public long getAccessKey() { return getH2(); }
    public AccessInfo getAccessInfo() { return new AccessInfo(getAccessKey()); }
    public boolean getInstructionInF0() { return (getMidInstructionDescription() & 04) != 0; }
    public boolean getExecuteRepeatedInstruction() { return (getMidInstructionDescription() & 02) != 0; }
    public boolean getBreakpointRegisterMatchCondition() { return (getPendingInterruptInformation() & 04) != 0; }
    public boolean getSoftwareBreak() { return (getPendingInterruptInformation() & 02) != 0; }

    public void setShortStatusField(long value) { setS1(value); }
    public void setMidInstructionDescription(long value) { _value = (_value & 0_770777_777777L) | ((value & 07) << 27); }
    public void setPendingInterruptInformation(long value) { _value = (_value & 0_777077_777777L) | ((value & 07) << 24); }
    public void setInterruptClassField(long value) { setS3(value); }
    public void setAccessKey(long value) { setH2(value); }

    public void setInstructionInF0(
        final boolean flag
    ) {
        _value &= 0_773777_777777L;
        if (flag) {
            _value |= 0_004000_000000L;
        }
    }

    public void setExecuteRepeatedInstruction(
        final boolean flag
    ) {
        _value &= 0_775777_777777L;
        if (flag) {
            _value |= 0_002000_000000L;
        }
    }

    public void setBreakpointRegisterMatchCondition(
        final boolean flag
    ) {
        _value &= 0_777377_777777L;
        if (flag) {
            _value |= 0_000400_000000L;
        }
    }

    public void setSoftwareBreak(
        final boolean flag
    ) {
        _value &= 0_777577_777777L;
        if (flag) {
            _value |= 0_000200_000000L;
        }
    }

    public void clear() { _value = 0; }
}
