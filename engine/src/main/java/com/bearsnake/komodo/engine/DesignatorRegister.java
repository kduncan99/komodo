/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine;

import com.bearsnake.komodo.baselib.Word36;

/**
 * Describes a designator register
 */
public class DesignatorRegister {

    static final long MASK_ActivityLevelQueueMonitorEnabled             = Word36.MASK_B0;
    static final long MASK_PerformanceMonitoringCounterEnabled          = Word36.MASK_B1;
    static final long MASK_PerformanceMonitoringCounterInterruptControl = Word36.MASK_B2;
    static final long MASK_SoftwarePerformanceMonitor                   = Word36.MASK_B3 | Word36.MASK_B4 | Word36.MASK_B5;
    static final long MASK_FaultHandlingInProgress                      = Word36.MASK_B6;
    static final long MASK_Executive24BitIndexingEnabled                = Word36.MASK_B11;
    static final long MASK_QuantumTimerEnabled                          = Word36.MASK_B12;
    static final long MASK_DeferrableInterruptEnabled                   = Word36.MASK_B13;
    static final long MASK_ProcessorPrivilege                           = Word36.MASK_B14 | Word36.MASK_B15;
    static final long MASK_BasicModeEnabled                             = Word36.MASK_B16;
    static final long MASK_ExecRegisterSetSelected                      = Word36.MASK_B17;
    static final long MASK_Carry                                        = Word36.MASK_B18;
    static final long MASK_Overflow                                     = Word36.MASK_B19;
    static final long MASK_CharacteristicUnderflow                      = Word36.MASK_B21;
    static final long MASK_CharacteristicOverflow                       = Word36.MASK_B22;
    static final long MASK_DivideCheck                                  = Word36.MASK_B23;
    static final long MASK_OperationTrapEnabled                         = Word36.MASK_B27;
    static final long MASK_ArithmeticExceptionEnabled                   = Word36.MASK_B29;
    static final long MASK_BasicModeBaseRegisterSelection               = Word36.MASK_B31;
    static final long MASK_QuarterWordModeEnabled                       = Word36.MASK_B32;

    public static final long MASK_SetToZero = (Word36.MASK_B7 | Word36.MASK_B8 | Word36.MASK_B9 | Word36.MASK_B10
                                               | Word36.MASK_B20 | Word36.MASK_B24 | Word36.MASK_B25 | Word36.MASK_B26
                                               | Word36.MASK_B28 | Word36.MASK_B30
                                               | Word36.MASK_B33 | Word36.MASK_B34 | Word36.MASK_B35);

    private boolean _activityLevelQueueMonitorEnabled;
    private boolean _performanceMonitoringCounterEnabled;
    private boolean _performanceMonitoringCounterInterruptControl;
    private short _softwarePerformanceMonitor;
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
    private boolean _basicModeBaseRegisterSelection;    // If true, B13/B15 are the primary pair. When false, B12/B14 are primary.
    private boolean _quarterWordModeEnabled;

    public DesignatorRegister() {}

    public DesignatorRegister clear() {
        _activityLevelQueueMonitorEnabled = false;
        _performanceMonitoringCounterEnabled = false;
        _performanceMonitoringCounterInterruptControl = false;
        _softwarePerformanceMonitor = 0;
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

    public long getCompositeValue() {
        long result = 0;
        result |= (_activityLevelQueueMonitorEnabled ? MASK_ActivityLevelQueueMonitorEnabled : 0);
        result |= (_performanceMonitoringCounterEnabled ? MASK_PerformanceMonitoringCounterEnabled : 0);
        result |= (_performanceMonitoringCounterInterruptControl ? MASK_PerformanceMonitoringCounterInterruptControl : 0);
        result |= ((long)(_softwarePerformanceMonitor & 07) << 30);
        result |= (_faultHandlingInProgress ? MASK_FaultHandlingInProgress : 0);
        result |= (_executive24BitIndexingEnabled ? MASK_Executive24BitIndexingEnabled : 0);
        result |= (_quantumTimerEnabled ? MASK_QuantumTimerEnabled : 0);
        result |= (_deferrableInterruptEnabled ? MASK_DeferrableInterruptEnabled : 0);
        result |= ((long)(_processorPrivilege & 03) << 20);
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

    public short getDB12to17() {
        short result = 0;
        result |= (short)(_quantumTimerEnabled ? 0_40 : 0);
        result |= (short)(_deferrableInterruptEnabled ? 0_20 : 0);
        result |= (short)(_processorPrivilege << 2);
        result |= (short)(_basicModeEnabled ? 0_02 : 0);
        result |= (short)(_execRegisterSetSelected ? 0_01 : 0);
        return result;
    }

    public short getSoftwarePerformanceMonitor()        { return (short)(_softwarePerformanceMonitor & 07); }
    public short getProcessorPrivilege()                { return (short)(_processorPrivilege & 03); }
    public boolean getBasicModeBaseRegisterSelection()  { return _basicModeBaseRegisterSelection; }
    public boolean isActivityLevelQueueMonitorEnabled() { return _activityLevelQueueMonitorEnabled; }
    public boolean isPerformanceMonitoringCounterEnabled() { return _performanceMonitoringCounterEnabled; }
    public boolean isPerformanceMonitoringCounterInterruptControl() { return _performanceMonitoringCounterInterruptControl; }
    public boolean isFaultHandlingInProgress()          { return _faultHandlingInProgress; }
    public boolean isExecutive24BitIndexingEnabled()    { return _executive24BitIndexingEnabled; }
    public boolean isQuantumTimerEnabled()              { return _quantumTimerEnabled; }
    public boolean isDeferrableInterruptEnabled()       { return _deferrableInterruptEnabled; }
    public boolean isBasicModeEnabled()                 { return _basicModeEnabled; }
    public boolean isExecRegisterSetSelected()          { return _execRegisterSetSelected; }
    public boolean isCarry()                            { return _carry; }
    public boolean isOverflow()                         { return _overflow; }
    public boolean isCharacteristicUnderflow()          { return _characteristicUnderflow; }
    public boolean isCharacteristicOverflow()           { return _characteristicOverflow; }
    public boolean isDivideCheck()                      { return _divideCheck; }
    public boolean isOperationTrapEnabled()             { return _operationTrapEnabled; }
    public boolean isArithmeticExceptionEnabled()       { return _arithmeticExceptionEnabled; }
    public boolean isQuarterWordModeEnabled()           { return _quarterWordModeEnabled; }

    public DesignatorRegister set(
        final DesignatorRegister source
    ) {
        _activityLevelQueueMonitorEnabled = source._activityLevelQueueMonitorEnabled;
        _performanceMonitoringCounterEnabled = source._performanceMonitoringCounterEnabled;
        _performanceMonitoringCounterInterruptControl = source._performanceMonitoringCounterInterruptControl;
        _softwarePerformanceMonitor = source._softwarePerformanceMonitor;
        _faultHandlingInProgress = source._faultHandlingInProgress;
        _executive24BitIndexingEnabled = source._executive24BitIndexingEnabled;
        _quantumTimerEnabled = source._quantumTimerEnabled;
        _deferrableInterruptEnabled = source._deferrableInterruptEnabled;
        _processorPrivilege = source._processorPrivilege;
        _basicModeEnabled = source._basicModeEnabled;
        _execRegisterSetSelected = source._execRegisterSetSelected;
        _carry = source._carry;
        _overflow = source._overflow;
        _characteristicUnderflow = source._characteristicUnderflow;
        _characteristicOverflow = source._characteristicOverflow;
        _divideCheck = source._divideCheck;
        _operationTrapEnabled = source._operationTrapEnabled;
        _arithmeticExceptionEnabled = source._arithmeticExceptionEnabled;
        _basicModeBaseRegisterSelection = source._basicModeBaseRegisterSelection;
        _quarterWordModeEnabled = source._quarterWordModeEnabled;
        return this;
    }

    public DesignatorRegister set(
        final long value
    ) {
        _activityLevelQueueMonitorEnabled = (value & MASK_ActivityLevelQueueMonitorEnabled) != 0;
        _performanceMonitoringCounterEnabled = (value & MASK_PerformanceMonitoringCounterEnabled) != 0;
        _performanceMonitoringCounterInterruptControl = (value & MASK_PerformanceMonitoringCounterInterruptControl) != 0;
        _softwarePerformanceMonitor = (short)((value >> 30) & 0x07);
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
        return this;
    }

    public DesignatorRegister setActivityLevelQueueMonitorEnabled(
        final boolean flag
    ) {
        _activityLevelQueueMonitorEnabled = flag;
        return this;
    }

    public DesignatorRegister setPerformanceMonitoringCounterEnabled(
        final boolean flag
    ) {
        _performanceMonitoringCounterEnabled = flag;
        return this;
    }

    public DesignatorRegister setPerformanceMonitoringCounterInterruptControl(
        final boolean flag
    ) {
        _performanceMonitoringCounterInterruptControl = flag;
        return this;
    }

    public DesignatorRegister setSoftwarePerformanceMonitor(
        final short value
    ) {
        _softwarePerformanceMonitor = (short)(value & 0x07);
        return this;
    }

    public DesignatorRegister setFaultHandlingInProgress(
        final boolean flag
    ) {
        _faultHandlingInProgress = flag;
        return this;
    }

    public DesignatorRegister setExecutive24BitIndexingEnabled(
        final boolean flag
    ) {
        _executive24BitIndexingEnabled = flag;
        return this;
    }

    public DesignatorRegister setQuantumTimerEnabled(
        final boolean flag
    ) {
        _quantumTimerEnabled = flag;
        return this;
    }

    public DesignatorRegister setDeferrableInterruptEnabled(
        final boolean flag
    ) {
        _deferrableInterruptEnabled = flag;
        return this;
    }

    public DesignatorRegister setProcessorPrivilege(
        final short value
    ) {
        _processorPrivilege = (short)(value & 0x03);
        return this;
    }

    public DesignatorRegister setBasicModeEnabled(
        final boolean flag
    ) {
        _basicModeEnabled = flag;
        return this;
    }

    public DesignatorRegister setExecRegisterSetSelected(
        final boolean flag
    ) {
        _execRegisterSetSelected = flag;
        return this;
    }

    public DesignatorRegister setCarry(
        final boolean flag
    ) {
        _carry = flag;
        return this;
    }

    public DesignatorRegister setOverflow(
        final boolean flag
    ) {
        _overflow = flag;
        return this;
    }

    public DesignatorRegister setCharacteristicUnderflow(
        final boolean flag
    ) {
        _characteristicUnderflow = flag;
        return this;
    }

    public DesignatorRegister setCharacteristicOverflow(
        final boolean flag
    ) {
        _characteristicOverflow = flag;
        return this;
    }

    public DesignatorRegister setDivideCheck(
        final boolean flag
    ) {
        _divideCheck = flag;
        return this;
    }

    public DesignatorRegister setOperationTrapEnabled(
        final boolean flag
    ) {
        _operationTrapEnabled = flag;
        return this;
    }

    public DesignatorRegister setArithmeticExceptionEnabled(
        final boolean flag
    ) {
        _arithmeticExceptionEnabled = flag;
        return this;
    }

    public DesignatorRegister setBasicModeBaseRegisterSelection(
        final boolean flag
    ) {
        _basicModeBaseRegisterSelection = flag;
        return this;
    }

    public DesignatorRegister setQuarterWordModeEnabled(
        final boolean flag
    ) {
        _quarterWordModeEnabled = flag;
        return this;
    }

    public void setDB12to17(
        final short value
    ) {
        _quantumTimerEnabled = (value & 0_40) != 0;
        _deferrableInterruptEnabled = (value & 0_20) != 0;
        _processorPrivilege = (short)((value >> 2) & 0x03);
        _basicModeEnabled = (value & 0_02) != 0;
        _execRegisterSetSelected = (value & 0_01) != 0;
    }

    public void display() {
        System.out.println("ActivityLevelQueueMonitorEnabled: " + _activityLevelQueueMonitorEnabled);
        System.out.println("PerformanceMonitoringCounterEnabled: " + _performanceMonitoringCounterEnabled);
        System.out.println("PerformanceMonitoringCounterInterruptControl: " + _performanceMonitoringCounterInterruptControl);
        System.out.println("SoftwarePerformanceMonitor: " + _softwarePerformanceMonitor);
        System.out.println("FaultHandlingInProgress: " + _faultHandlingInProgress);
        System.out.println("Executive24BitIndexingEnabled: " + _executive24BitIndexingEnabled);
        System.out.println("QuantumTimerEnabled: " + _quantumTimerEnabled);
        System.out.println("DeferrableInterruptEnabled: " + _deferrableInterruptEnabled);
        System.out.println("ProcessorPrivilege: " + _processorPrivilege);
        System.out.println("BasicModeEnabled: " + _basicModeEnabled);
        System.out.println("ExecRegisterSetSelected: " + _execRegisterSetSelected);
        System.out.println("Carry: " + _carry);
        System.out.println("Overflow: " + _overflow);
        System.out.println("CharacteristicUnderflow: " + _characteristicUnderflow);
        System.out.println("CharacteristicOverflow: " + _characteristicOverflow);
        System.out.println("DivideCheck: " + _divideCheck);
        System.out.println("OperationTrapEnabled: " + _operationTrapEnabled);
        System.out.println("ArithmeticExceptionEnabled: " + _arithmeticExceptionEnabled);
        System.out.println("BasicModeBaseRegisterSelection: " + _basicModeBaseRegisterSelection);
        System.out.println("QuarterWordModeEnabled: " + _quarterWordModeEnabled);
    }
}
