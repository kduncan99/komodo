/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine;

import com.bearsnake.komodo.baselib.Word36;

public class IndicatorKeyRegister extends Word36 {

    public IndicatorKeyRegister() {}
    public IndicatorKeyRegister(final long value) { super(value); }

    public final int getShortStatusField()                        { return (int) getS1(_value); }
    public final int getMidInstructionDescription()               { return (int) (getS2(_value) >> 3); }
    public final int getPendingInterruptInformation()             { return (int) (getS2(_value) & 07); }
    public final int getInterruptClassField()                     { return (int) getS3(_value); }
    public final int getAccessKey()                               { return (int) getH2(_value); }
    public final AccessInfo getAccessInfo()                       { return new AccessInfo(getAccessKey()); }
    public final boolean getInstructionInF0()                     { return (getMidInstructionDescription() & 04) != 0; }
    public final boolean getExecuteRepeatedInstruction()          { return (getMidInstructionDescription() & 02) != 0; }
    public final boolean getBreakpointRegisterMatchCondition()    { return (getPendingInterruptInformation() & 04) != 0; }
    public final boolean getSoftwareBreak()                       { return (getPendingInterruptInformation() & 02) != 0; }

    public IndicatorKeyRegister setW(
        final long value
    ) {
        _value = value;
        return this;
    }

    public IndicatorKeyRegister setShortStatusField(
        final int value
    ) {
        _value = Word36.setS1(_value, value);
        return this;
    }

    public IndicatorKeyRegister setMidInstructionDescription(
        final int value
    ) {
        _value = (_value & 0_770777_777777L) | ((value & 07) << 27);
        return this;
    }

    public IndicatorKeyRegister setPendingInterruptInformation(
        final int value
    ) {
        _value = (_value & 0_777077_777777L) | ((value & 07) << 24);
        return this;
    }

    public IndicatorKeyRegister setInterruptClassField(final int value) { setS3(value); return this; }
    public IndicatorKeyRegister setAccessKey(final int value) { setH2(value); return this; }

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
