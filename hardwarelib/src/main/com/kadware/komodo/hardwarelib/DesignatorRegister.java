/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

import com.kadware.komodo.baselib.Word36;

/**
 * Describes a designator register
 */
public class DesignatorRegister {

    private static final long MASK_ActivityLevelQueueMonitorEnabled = Word36.MASK_B0;
    private static final long MASK_FaultHandlingInProgress          = Word36.MASK_B6;
    private static final long MASK_Executive24BitIndexingEnabled    = Word36.MASK_B11;
    private static final long MASK_QuantumTimerEnabled              = Word36.MASK_B12;
    private static final long MASK_DeferrableInterruptEnabled       = Word36.MASK_B13;
    private static final long MASK_ProcessorPrivilege               = Word36.MASK_B14 | Word36.MASK_B15;
    private static final long MASK_BasicModeEnabled                 = Word36.MASK_B16;
    private static final long MASK_ExecRegisterSetSelected          = Word36.MASK_B17;
    private static final long MASK_Carry                            = Word36.MASK_B18;
    private static final long MASK_Overflow                         = Word36.MASK_B19;
    private static final long MASK_CharacteristicUnderflow          = Word36.MASK_B21;
    private static final long MASK_CharacteristicOverflow           = Word36.MASK_B22;
    private static final long MASK_DivideCheck                      = Word36.MASK_B23;
    private static final long MASK_OperationTrapEnabled             = Word36.MASK_B27;
    private static final long MASK_ArithmeticExceptionEnabled       = Word36.MASK_B29;
    private static final long MASK_BasicModeBaseRegisterSelection   = Word36.MASK_B31;
    private static final long MASK_QuarterWordModeEnabled           = Word36.MASK_B32;

    private long _value = 0;

    public DesignatorRegister() {}
    public DesignatorRegister(long value) { _value = value & Word36.BIT_MASK; }

    private void changeBit(
        final long mask,
        final boolean newBit
    ) {
        _value = newBit ? Word36.logicalOr(_value, mask) : Word36.logicalAnd(_value, invertMask(mask));
    }

    private long invertMask(final long mask)                { return mask ^ Word36.BIT_MASK; }

    public void clear()                                     { _value = 0; }
    public boolean getActivityLevelQueueMonitorEnabled()    { return (_value & MASK_ActivityLevelQueueMonitorEnabled) != 0; }
    public boolean getFaultHandlingInProgress()             { return (_value & MASK_FaultHandlingInProgress) != 0; }
    public boolean getExecutive24BitIndexingEnabled()       { return (_value & MASK_Executive24BitIndexingEnabled) != 0; }
    public boolean getQuantumTimerEnabled()                 { return (_value & MASK_QuantumTimerEnabled) != 0; }
    public boolean getDeferrableInterruptEnabled()          { return (_value & MASK_DeferrableInterruptEnabled) != 0; }
    public int getProcessorPrivilege()                      { return (int)((_value & (Word36.MASK_B14 | Word36.MASK_B15)) >> 20); }
    public boolean getBasicModeEnabled()                    { return (_value & MASK_BasicModeEnabled) != 0; }
    public boolean getExecRegisterSetSelected()             { return (_value & MASK_ExecRegisterSetSelected) != 0; }
    public boolean getCarry()                               { return (_value & MASK_Carry) != 0; }
    public boolean getOverflow()                            { return (_value & MASK_Overflow) != 0; }
    public boolean getCharacteristicUnderflow()             { return (_value & MASK_CharacteristicUnderflow) != 0; }
    public boolean getCharacteristicOverflow()              { return (_value & MASK_CharacteristicOverflow) != 0; }
    public boolean getDivideCheck()                         { return (_value & MASK_DivideCheck) != 0; }
    public boolean getOperationTrapEnabled()                { return (_value & MASK_OperationTrapEnabled) != 0; }
    public boolean getArithmeticExceptionEnabled()          { return (_value & MASK_ArithmeticExceptionEnabled) != 0; }
    public boolean getBasicModeBaseRegisterSelection()      { return (_value & MASK_BasicModeBaseRegisterSelection) != 0; }
    public boolean getQuarterWordModeEnabled()              { return (_value & MASK_QuarterWordModeEnabled) != 0; }
    public long getW()                                      { return _value; }
    public long getS4()                                     { return Word36.getS4(_value); }

    public void setActivityLevelQueueMonitorEnabled(boolean flag)   { changeBit(MASK_ActivityLevelQueueMonitorEnabled, flag); }
    public void setFaultHandlingInProgress(boolean flag)            { changeBit(MASK_FaultHandlingInProgress, flag); }
    public void setExecutive24BitIndexingEnabled(boolean flag)      { changeBit(MASK_Executive24BitIndexingEnabled, flag); }
    public void setQuantumTimerEnabled(boolean flag)                { changeBit(MASK_QuantumTimerEnabled, flag); }
    public void setDeferrableInterruptEnabled(boolean flag)         { changeBit(MASK_DeferrableInterruptEnabled, flag); }
    public void setS4(long value)                                   { _value = Word36.setS4(_value, value); }
    public void setW(long value)                                    { _value = value & Word36.BIT_MASK; }

    public void setProcessorPrivilege(
        final int value
    ) {
        long cleared = _value & invertMask(MASK_ProcessorPrivilege);
        _value = cleared | ((value & 03L) << 20);
    }

    public void setBasicModeEnabled(boolean flag)                   { changeBit(MASK_BasicModeEnabled, flag); }
    public void setExecRegisterSetSelected(boolean flag)            { changeBit(MASK_ExecRegisterSetSelected, flag); }
    public void setCarry(boolean flag)                              { changeBit(MASK_Carry, flag); }
    public void setOverflow(boolean flag)                           { changeBit(MASK_Overflow, flag); }
    public void setCharacteristicUnderflow(boolean flag)            { changeBit(MASK_CharacteristicUnderflow, flag); }
    public void setCharacteristicOverflow(boolean flag)             { changeBit(MASK_CharacteristicOverflow, flag); }
    public void setDivideCheck(boolean flag)                        { changeBit(MASK_DivideCheck, flag); }
    public void setOperationTrapEnabled(boolean flag)               { changeBit(MASK_OperationTrapEnabled, flag); }
    public void setArithmeticExceptionEnabled(boolean flag)         { changeBit(MASK_ArithmeticExceptionEnabled, flag); }
    public void setBasicModeBaseRegisterSelection(boolean flag)     { changeBit(MASK_BasicModeBaseRegisterSelection, flag); }
    public void setQuarterWordModeEnabled(boolean flag)             { changeBit(MASK_QuarterWordModeEnabled, flag); }
}
