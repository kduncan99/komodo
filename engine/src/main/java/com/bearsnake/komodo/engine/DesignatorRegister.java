/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine;

import com.bearsnake.komodo.baselib.Word36;

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

    private boolean _activityLevelQueueMonitorEnabled;
    private boolean _faultHandlingInProgress;
    private boolean _executive24BitIndexingEnabled;
    private boolean _quantumTimerEnabled;
    private boolean _deferrableInterruptEnabled;
    private short _processorPrivilege;
    private boolean _basicModeEnabled;
    private boolean _execRegisterSetSelected;
    private boolean _carry;
    private boolean _overflow;
    private boolean _characteristicUnderflow;
    private boolean _characteristicOverflow;
    private boolean _divideCheck;
    private boolean _operationTrapEnabled;
    private boolean _arithmeticExceptionEnabled;
    private boolean _basicModeBaseRegisterSelection;
    private boolean _quarterWordModeEnabled;

    public DesignatorRegister() {}

    public DesignatorRegister clear() {
        _activityLevelQueueMonitorEnabled = false;
        _faultHandlingInProgress = false;
        _executive24BitIndexingEnabled = false;
        _quantumTimerEnabled = false;
        _deferrableInterruptEnabled = false;
        _processorPrivilege = 0;
        _basicModeEnabled = false;
        _execRegisterSetSelected = false;
        _carry = false;
        _overflow = false;
        _characteristicUnderflow = false;
        _characteristicOverflow = false;
        _divideCheck = false;
        _operationTrapEnabled = false;
        _arithmeticExceptionEnabled = false;

        return this;
    }

    public long getWord36() {
        long result = 0;
        result |= (_activityLevelQueueMonitorEnabled ? MASK_ActivityLevelQueueMonitorEnabled : 0);
        result |= (_faultHandlingInProgress ? MASK_FaultHandlingInProgress : 0);
        result |= (_executive24BitIndexingEnabled ? MASK_Executive24BitIndexingEnabled : 0);
        result |= (_quantumTimerEnabled ? MASK_QuantumTimerEnabled : 0);
        result |= (_deferrableInterruptEnabled ? MASK_DeferrableInterruptEnabled : 0);
        result |= ((long)_processorPrivilege << 20);
        result |= (_basicModeEnabled ? MASK_BasicModeEnabled : 0);
        result |= (_execRegisterSetSelected ? MASK_ExecRegisterSetSelected : 0);
        result |= (_carry ? MASK_Carry : 0);
        result |= (_overflow ? MASK_Overflow : 0);
        result |= (_characteristicUnderflow ? MASK_CharacteristicUnderflow : 0);
        result |= (_characteristicOverflow ? MASK_CharacteristicOverflow : 0);
        result |= (_divideCheck ? MASK_DivideCheck : 0);
        result |= (_operationTrapEnabled ? MASK_OperationTrapEnabled : 0);
        result |= (_arithmeticExceptionEnabled ? MASK_ArithmeticExceptionEnabled : 0);
        result |= (_basicModeBaseRegisterSelection ? MASK_BasicModeBaseRegisterSelection : 0);
        result |= (_quarterWordModeEnabled ? MASK_QuarterWordModeEnabled : 0);
        return result;
    }

    public void setWord36(final long value) {
        _activityLevelQueueMonitorEnabled = (value & MASK_ActivityLevelQueueMonitorEnabled) != 0;
        _faultHandlingInProgress = (value & MASK_FaultHandlingInProgress) != 0;
        _executive24BitIndexingEnabled = (value & MASK_Executive24BitIndexingEnabled) != 0;
        _quantumTimerEnabled = (value & MASK_QuantumTimerEnabled) != 0;
        _deferrableInterruptEnabled = (value & MASK_DeferrableInterruptEnabled) != 0;
        _processorPrivilege = (short)((value >> 20) & 0x03);
        _basicModeEnabled = (value & MASK_BasicModeEnabled) != 0;
        _execRegisterSetSelected = (value & MASK_ExecRegisterSetSelected) != 0;
        _carry = (value & MASK_Carry) != 0;
        _overflow = (value & MASK_Overflow) != 0;
        _characteristicUnderflow = (value & MASK_CharacteristicUnderflow) != 0;
        _characteristicOverflow = (value & MASK_CharacteristicOverflow) != 0;
        _divideCheck = (value & MASK_DivideCheck) != 0;
        _operationTrapEnabled = (value & MASK_OperationTrapEnabled) != 0;
        _arithmeticExceptionEnabled = (value & MASK_ArithmeticExceptionEnabled) != 0;
        _basicModeBaseRegisterSelection = (value & MASK_BasicModeBaseRegisterSelection) != 0;
        _quarterWordModeEnabled = (value & MASK_QuarterWordModeEnabled) != 0;
    }

    public boolean isActivityLevelQueueMonitorEnabled() { return _activityLevelQueueMonitorEnabled; }
    public boolean isFaultHandlingInProgress()          { return _faultHandlingInProgress; }
    public boolean isExecutive24BitIndexingEnabled()    { return _executive24BitIndexingEnabled; }
    public boolean isQuantumTimerEnabled()              { return _quantumTimerEnabled; }
    public boolean isDeferrableInterruptEnabled()       { return _deferrableInterruptEnabled; }
    public short getProcessorPrivilege()                { return _processorPrivilege; }
    public boolean isBasicModeEnabled()                 { return _basicModeEnabled; }
    public boolean isExecRegisterSetSelected()          { return _execRegisterSetSelected; }
    public boolean isCarry()                            { return _carry; }
    public boolean isOverflow()                         { return _overflow; }
    public boolean isCharacteristicUnderflow()          { return _characteristicUnderflow; }
    public boolean isCharacteristicOverflow()           { return _characteristicOverflow; }
    public boolean isDivideCheck()                      { return _divideCheck; }
    public boolean isOperationTrapEnabled()             { return _operationTrapEnabled; }
    public boolean isArithmeticExceptionEnabled()       { return _arithmeticExceptionEnabled; }
    public boolean isBasicModeBaseRegisterSelection()   { return _basicModeBaseRegisterSelection; }
    public boolean isQuarterWordModeEnabled()           { return _quarterWordModeEnabled; }

    public DesignatorRegister setActivityLevelQueueMonitorEnabled(final boolean flag) {
        _activityLevelQueueMonitorEnabled = flag;
        return this;
    }

    public DesignatorRegister setFaultHandlingInProgress(final boolean flag) {
        _faultHandlingInProgress = flag;
        return this;
    }

    public DesignatorRegister setExecutive24BitIndexingEnabled(final boolean flag) {
        _executive24BitIndexingEnabled = flag;
        return this;
    }

    public DesignatorRegister setQuantumTimerEnabled(final boolean flag) {
        _quantumTimerEnabled = flag;
        return this;
    }

    public DesignatorRegister setDeferrableInterruptEnabled(final boolean flag) {
        _deferrableInterruptEnabled = flag;
        return this;
    }

    public DesignatorRegister setProcessorPrivilege(final short value) {
        _processorPrivilege = (short)(value & 0x03);
        return this;
    }

    public DesignatorRegister setBasicModeEnabled(final boolean flag) {
        _basicModeEnabled = flag;
        return this;
    }

    public DesignatorRegister setExecRegisterSetSelected(final boolean flag) {
        _execRegisterSetSelected = flag;
        return this;
    }

    public DesignatorRegister setCarry(final boolean flag) {
        _carry = flag;
        return this;
    }

    public DesignatorRegister setOverflow(final boolean flag) {
        _overflow = flag;
        return this;
    }

    public DesignatorRegister setCharacteristicUnderflow(final boolean flag) {
        _characteristicUnderflow = flag;
        return this;
    }

    public DesignatorRegister setCharacteristicOverflow(final boolean flag) {
        _characteristicOverflow = flag;
        return this;
    }

    public DesignatorRegister setDivideCheck(final boolean flag) {
        _divideCheck = flag;
        return this;
    }

    public DesignatorRegister setOperationTrapEnabled(final boolean flag) {
        _operationTrapEnabled = flag;
        return this;
    }

    public DesignatorRegister setArithmeticExceptionEnabled(final boolean flag) {
        _arithmeticExceptionEnabled = flag;
        return this;
    }

    public DesignatorRegister setBasicModeBaseRegisterSelection(final boolean flag) {
        _basicModeBaseRegisterSelection = flag;
        return this;
    }

    public DesignatorRegister setQuarterWordModeEnabled(final boolean flag) {
        _quarterWordModeEnabled = flag;
        return this;
    }

}
