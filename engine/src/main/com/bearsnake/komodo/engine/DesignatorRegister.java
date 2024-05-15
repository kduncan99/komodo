/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine;

import com.bearsnake.komodo.baselib.Word36;

/**
 * Describes a designator register
 */
public class DesignatorRegister extends Word36 {
    
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

    public DesignatorRegister() {}
    public DesignatorRegister(
        final long value
    ) {
        super(value);
    }

    private void changeBit(
        final long mask,
        final boolean newBit
    ) {
        _value = newBit ? (_value | mask) : (_value & invertMask(mask));
    }

    private long invertMask(
        final long mask
    ) {
        return mask ^ Word36.BIT_MASK;
    }

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

    public DesignatorRegister setActivityLevelQueueMonitorEnabled(
        final boolean flag
    ) {
        changeBit(MASK_ActivityLevelQueueMonitorEnabled, flag);
        return this;
    }

    public DesignatorRegister setFaultHandlingInProgress(
        final boolean flag
    ) {
        changeBit(MASK_FaultHandlingInProgress, flag);
        return this;
    }

    public DesignatorRegister setExecutive24BitIndexingEnabled(
        final boolean flag
    ) {
        changeBit(MASK_Executive24BitIndexingEnabled, flag);
        return this;
    }

    public DesignatorRegister setQuantumTimerEnabled(
        final boolean flag
    ) {
        changeBit(MASK_QuantumTimerEnabled, flag);
        return this;
    }

    public DesignatorRegister setDeferrableInterruptEnabled(
        final boolean flag
    ) {
        changeBit(MASK_DeferrableInterruptEnabled, flag);
        return this;
    }

    @Override public DesignatorRegister setS4(final long value) { return (DesignatorRegister) super.setS4(value); }
    @Override public DesignatorRegister setW(final long value) { return (DesignatorRegister) super.setW(value); }

    public DesignatorRegister setProcessorPrivilege(
        final int value
    ) {
        long cleared = _value & invertMask(MASK_ProcessorPrivilege);
        _value = cleared | ((value & 03L) << 20);
        return this;
    }

    public DesignatorRegister setBasicModeEnabled(
        final boolean flag
    ) {
        changeBit(MASK_BasicModeEnabled, flag);
        return this;
    }

    public DesignatorRegister setExecRegisterSetSelected(
        final boolean flag
    ) {
        changeBit(MASK_ExecRegisterSetSelected, flag);
        return this;
    }

    public DesignatorRegister setCarry(
        final boolean flag
    ) {
        changeBit(MASK_Carry, flag);
        return this;
    }

    public DesignatorRegister setOverflow(
        final boolean flag
    ) {
        changeBit(MASK_Overflow, flag);
        return this;
    }

    public DesignatorRegister setCharacteristicUnderflow(
        final boolean flag
    ) {
        changeBit(MASK_CharacteristicUnderflow, flag);
        return this;
    }

    public DesignatorRegister setCharacteristicOverflow(
        final boolean flag
    ) {
        changeBit(MASK_CharacteristicOverflow, flag);
        return this;
    }

    public DesignatorRegister setDivideCheck(
        final boolean flag
    ) {
        changeBit(MASK_DivideCheck, flag);
        return this;
    }

    public DesignatorRegister setOperationTrapEnabled(
        final boolean flag
    ) {
        changeBit(MASK_OperationTrapEnabled, flag);
        return this;
    }

    public DesignatorRegister setArithmeticExceptionEnabled(
        final boolean flag
    ) {
        changeBit(MASK_ArithmeticExceptionEnabled, flag);
        return this;
    }

    public DesignatorRegister setBasicModeBaseRegisterSelection(
        final boolean flag
    ) {
        changeBit(MASK_BasicModeBaseRegisterSelection, flag);
        return this;
    }

    public DesignatorRegister setQuarterWordModeEnabled(
        final boolean flag
    ) {
        changeBit(MASK_QuarterWordModeEnabled, flag);
        return this;
    }
}
