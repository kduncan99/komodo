/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

import com.kadware.komodo.baselib.AccessInfo;
import com.kadware.komodo.baselib.Word36;

/**
 * An extension of Word36 which describes an indicator key register
 */
public class IndicatorKeyRegister {

    private long _value = 0;

    public IndicatorKeyRegister() {}
    public IndicatorKeyRegister(long value)                 { _value = value & Word36.BIT_MASK; }

    public void clear()                                     { _value = 0; }
    public int getShortStatusField()                        { return (int) Word36.getS1(_value); }
    public int getMidInstructionDescription()               { return (int) (Word36.getS2(_value) >> 3); }
    public int getPendingInterruptInformation()             { return (int) (Word36.getS2(_value) & 07); }
    public int getInterruptClassField()                     { return (int) Word36.getS3(_value); }
    public int getAccessKey()                               { return (int) Word36.getH2(_value); }
    public AccessInfo getAccessInfo()                       { return new AccessInfo(getAccessKey()); }
    public boolean getInstructionInF0()                     { return (getMidInstructionDescription() & 04) != 0; }
    public boolean getExecuteRepeatedInstruction()          { return (getMidInstructionDescription() & 02) != 0; }
    public boolean getBreakpointRegisterMatchCondition()    { return (getPendingInterruptInformation() & 04) != 0; }
    public boolean getSoftwareBreak()                       { return (getPendingInterruptInformation() & 02) != 0; }

    public void setShortStatusField(int value)              { _value = Word36.setS1(_value, value); }
    public void setMidInstructionDescription(int value)     { _value = (_value & 0_770777_777777L) | ((value & 07) << 27); }
    public void setPendingInterruptInformation(int value)   { _value = (_value & 0_777077_777777L) | ((value & 07) << 24); }
    public void setInterruptClassField(int value)           { _value = Word36.setS3(_value, value); }
    public void setAccessKey(int value)                     { _value = Word36.setH2(_value, value); }

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
}
