/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine;

import com.bearsnake.komodo.baselib.Word36;

public class IndicatorKeyRegister {

    private int _shortStatusField;
    private int _midInstructionDescription;
    private int _pendingInterruptInformation;
    private int _interruptClassField;
    private int _accessKey;

    public IndicatorKeyRegister() {
        _shortStatusField = 0;
        _midInstructionDescription = 0;
        _pendingInterruptInformation = 0;
        _interruptClassField = 0;
        _accessKey = 0;
    }

    public IndicatorKeyRegister(final long value) {
        setWord36(value);
    }

    public final int getShortStatusField()                        { return _shortStatusField; }
    public final int getMidInstructionDescription()               { return _midInstructionDescription; }
    public final int getPendingInterruptInformation()             { return _pendingInterruptInformation; }
    public final int getInterruptClassField()                     { return _interruptClassField; }
    public final AccessKey getAccessKey()                         { return new AccessKey(_accessKey); }
    public final AccessInfo getAccessInfo()                       { return new AccessInfo(getAccessKey()); }
    public final boolean getInstructionInF0()                     { return (getMidInstructionDescription() & 04) != 0; }
    public final boolean isExecuteRepeatedInstruction()          { return (getMidInstructionDescription() & 02) != 0; }
    public final boolean getBreakpointRegisterMatchCondition()    { return (getPendingInterruptInformation() & 04) != 0; }
    public final boolean getSoftwareBreak()                       { return (getPendingInterruptInformation() & 02) != 0; }

    public long getWord36() {
        long value = Word36.setS1(0, _shortStatusField);
        value = Word36.setS2(value, (_midInstructionDescription << 3) | _pendingInterruptInformation);
        value = Word36.setS3(value, _interruptClassField);
        value = Word36.setH2(value, _accessKey);
        return value;
    }

    public IndicatorKeyRegister setWord36(
        final long value
    ) {
        _shortStatusField = (int) Word36.getS1(value);
        _midInstructionDescription = (int) (Word36.getS2(value) >> 3);
        _pendingInterruptInformation = (int) (Word36.getS2(value) & 07);
        _interruptClassField = (int) Word36.getS3(value);
        _accessKey = (int) Word36.getH2(value);
        return this;
    }

    public IndicatorKeyRegister setShortStatusField(
        final int value
    ) {
        _shortStatusField = value & 077;
        return this;
    }

    public IndicatorKeyRegister setMidInstructionDescription(
        final int value
    ) {
        _midInstructionDescription = value & 07;
        return this;
    }

    public IndicatorKeyRegister setPendingInterruptInformation(
        final int value
    ) {
        _pendingInterruptInformation = value & 07;
        return this;
    }

    public IndicatorKeyRegister setInterruptClassField(final int value) {
        _interruptClassField = value & 077;
        return this;
    }

    public IndicatorKeyRegister setAccessKey(final int value) {
        _accessKey = value & 0777777;
        return this;
    }

    public void setInstructionInF0(
        final boolean flag
    ) {
        if (flag) {
            _midInstructionDescription |= 04;
        } else {
            _midInstructionDescription &= 03;
        }
    }

    public void setExecuteRepeatedInstruction(
        final boolean flag
    ) {
        if (flag) {
            _midInstructionDescription |= 02;
        } else {
            _midInstructionDescription &= 05;
        }
    }

    public void setBreakpointRegisterMatchCondition(
        final boolean flag
    ) {
        if (flag) {
            _pendingInterruptInformation |= 04;
        } else {
            _pendingInterruptInformation &= 03;
        }
    }

    public void setSoftwareBreak(
        final boolean flag
    ) {
        if (flag) {
            _pendingInterruptInformation |= 02;
        } else {
            _pendingInterruptInformation &= 05;
        }
    }
}
