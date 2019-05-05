/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib.misc;

import com.kadware.em2200.baselib.Word36;

/**
 * Describes a designator register
 */
public class DesignatorRegister extends Word36 {

    public static final long MASK_ActivityLevelQueueMonitorEnabled   = MASK_B0;
    public static final long MASK_FaultHandlingInProgress            = MASK_B6;
    public static final long MASK_Executive24BitIndexingEnabled      = MASK_B11;
    public static final long MASK_QuantumTimerEnabled                = MASK_B12;
    public static final long MASK_DeferrableInterruptEnabled         = MASK_B13;
    public static final long MASK_ProcessorPrivilege                 = MASK_B14 | MASK_B15;
    public static final long MASK_BasicModeEnabled                   = MASK_B16;
    public static final long MASK_ExecRegisterSetSelected            = MASK_B17;
    public static final long MASK_Carry                              = MASK_B18;
    public static final long MASK_Overflow                           = MASK_B19;
    public static final long MASK_CharacteristicUnderflow            = MASK_B21;
    public static final long MASK_CharacteristicOverflow             = MASK_B22;
    public static final long MASK_DivideCheck                        = MASK_B23;
    public static final long MASK_OperationTrapEnabled               = MASK_B27;
    public static final long MASK_ArithmeticExceptionEnabled         = MASK_B29;
    public static final long MASK_BasicModeBaseRegisterSelection     = MASK_B31;
    public static final long MASK_QuarterWordModeEnabled             = MASK_B32;

    /**
     * Standard Constructor
     */
    public DesignatorRegister(
    ) {
    }

    /**
     * Initial value constructor
     * @param value source value
     */
    public DesignatorRegister(
        final long value
    ) {
        super(value);
    }

    /**
     * Convenience method to clear all the fields
     */
    public void clear(
    ) {
        setW(0);
    }

    /**
     * Getter
     * @return value
     */
    public boolean getActivityLevelQueueMonitorEnabled(
    ) {
        return (_value & MASK_ActivityLevelQueueMonitorEnabled) != 0;
    }

    /**
     * Getter
     * @return value
     */
    public boolean getFaultHandlingInProgress(
    ) {
        return (_value & MASK_FaultHandlingInProgress) != 0;
    }

    /**
     * Getter
     * @return value
     */
    public boolean getExecutive24BitIndexingEnabled(
    ) {
        return (_value & MASK_Executive24BitIndexingEnabled) != 0;
    }

    /**
     * Getter
     * @return value
     */
    public boolean getQuantumTimerEnabled(
    ) {
        return (_value & MASK_QuantumTimerEnabled) != 0;
    }

    /**
     * Getter
     * @return value
     */
    public boolean getDeferrableInterruptEnabled(
    ) {
        return (_value & MASK_DeferrableInterruptEnabled) != 0;
    }

    /**
     * Getter
     * @return value
     */
    public int getProcessorPrivilege(
    ) {
        return (int)((_value & (MASK_B14 | MASK_B15)) >> 20);
    }

    /**
     * Getter
     * @return value
     */
    public boolean getBasicModeEnabled(
    ) {
        return (_value & MASK_BasicModeEnabled) != 0;
    }

    /**
     * Getter
     * @return value
     */
    public boolean getExecRegisterSetSelected(
    ) {
        return (_value & MASK_ExecRegisterSetSelected) != 0;
    }

    /**
     * Getter
     * @return value
     */
    public boolean getCarry(
    ) {
        return (_value & MASK_Carry) != 0;
    }

    /**
     * Getter
     * @return value
     */
    public boolean getOverflow(
    ) {
        return (_value & MASK_Overflow) != 0;
    }

    /**
     * Getter
     * @return value
     */
    public boolean getCharacteristicUnderflow(
    ) {
        return (_value & MASK_CharacteristicUnderflow) != 0;
    }

    /**
     * Getter
     * @return value
     */
    public boolean getCharacteristicOverflow(
    ) {
        return (_value & MASK_CharacteristicOverflow) != 0;
    }

    /**
     * Getter
     * @return value
     */
    public boolean getDivideCheck(
    ) {
        return (_value & MASK_DivideCheck) != 0;
    }

    /**
     * Getter
     * @return value
     */
    public boolean getOperationTrapEnabled(
    ) {
        return (_value & MASK_OperationTrapEnabled) != 0;
    }

    /**
     * Getter
     * @return value
     */
    public boolean getArithmeticExceptionEnabled(
    ) {
        return (_value & MASK_ArithmeticExceptionEnabled) != 0;
    }

    /**
     * Getter
     * @return value
     */
    public boolean getBasicModeBaseRegisterSelection(
    ) {
        return (_value & MASK_BasicModeBaseRegisterSelection) != 0;
    }

    /**
     * Getter
     * @return value
     */
    public boolean getQuarterWordModeEnabled(
    ) {
        return (_value & MASK_QuarterWordModeEnabled) != 0;
    }

    /**
     * Setter
     * @param flag new value
     */
    public void setActivityLevelQueueMonitorEnabled(
        final boolean flag
    ) {
        _value &= ~MASK_ActivityLevelQueueMonitorEnabled;
        if (flag) {
            _value |= MASK_ActivityLevelQueueMonitorEnabled;
        }
    }

    /**
     * Setter
     * @param flag new value
     */
    public void setFaultHandlingInProgress(
        final boolean flag
    ) {
        _value &= ~MASK_FaultHandlingInProgress;
        if (flag) {
            _value |= MASK_FaultHandlingInProgress;
        }
    }

    /**
     * Setter
     * @param flag new value
     */
    public void setExecutive24BitIndexingEnabled(
        final boolean flag
    ) {
        _value &= ~MASK_Executive24BitIndexingEnabled;
        if (flag) {
            _value |= MASK_Executive24BitIndexingEnabled;
        }
    }

    /**
     * Setter
     * @param flag new value
     */
    public void setQuantumTimerEnabled(
        final boolean flag
    ) {
        _value &= ~MASK_QuantumTimerEnabled;
        if (flag) {
            _value |= MASK_QuantumTimerEnabled;
        }
    }

    /**
     * Setter
     * @param flag new value
     */
    public void setDeferrableInterruptEnabled(
        final boolean flag
    ) {
        _value &= ~MASK_DeferrableInterruptEnabled;
        if (flag) {
            _value |= MASK_DeferrableInterruptEnabled;
        }
    }

    /**
     * Setter
     * @param value new value
     */
    public void setProcessorPrivilege(
        final int value
    ) {
        _value = (_value & ~MASK_ProcessorPrivilege) | ((value & 03L) << 20);
    }

    /**
     * Setter
     * @param flag new value
     */
    public void setBasicModeEnabled(
        final boolean flag
    ) {
        _value &= ~MASK_BasicModeEnabled;
        if (flag) {
            _value |= MASK_BasicModeEnabled;
        }
    }

    /**
     * Setter
     * @param flag new value
     */
    public void setExecRegisterSetSelected(
        final boolean flag
    ) {
        _value &= ~MASK_ExecRegisterSetSelected;
        if (flag) {
            _value |= MASK_ExecRegisterSetSelected;
        }
    }

    /**
     * Setter
     * @param flag new value
     */
    public void setCarry(
        final boolean flag
    ) {
        _value &= ~MASK_Carry;
        if (flag) {
            _value |= MASK_Carry;
        }
    }

    /**
     * Setter
     * @param flag new value
     */
    public void setOverflow(
        final boolean flag
    ) {
        _value &= ~MASK_Overflow;
        if (flag) {
            _value |= MASK_Overflow;
        }
    }

    /**
     * Setter
     * @param flag new value
     */
    public void setCharacteristicUnderflow(
        final boolean flag
    ) {
        _value &= ~MASK_CharacteristicUnderflow;
        if (flag) {
            _value |= MASK_CharacteristicUnderflow;
        }
    }

    /**
     * Setter
     * @param flag new value
     */
    public void setCharacteristicOverflow(
        final boolean flag
    ) {
        _value &= ~MASK_CharacteristicOverflow;
        if (flag) {
            _value |= MASK_CharacteristicOverflow;
        }
    }

    /**
     * Setter
     * @param flag new value
     */
    public void setDivideCheck(
        final boolean flag
    ) {
        _value &= ~MASK_DivideCheck;
        if (flag) {
            _value |= MASK_DivideCheck;
        }
    }

    /**
     * Setter
     * @param flag new value
     */
    public void setOperationTrapEnabled(
        final boolean flag
    ) {
        _value &= ~MASK_OperationTrapEnabled;
        if (flag) {
            _value |= MASK_OperationTrapEnabled;
        }
    }

    /**
     * Setter
     * @param flag new value
     */
    public void setArithmeticExceptionEnabled(
        final boolean flag
    ) {
        _value &= ~MASK_ArithmeticExceptionEnabled;
        if (flag) {
            _value |= MASK_ArithmeticExceptionEnabled;
        }
    }

    /**
     * Setter
     * @param flag new value
     */
    public void setBasicModeBaseRegisterSelection(
        final boolean flag
    ) {
        _value &= ~MASK_BasicModeBaseRegisterSelection;
        if (flag) {
            _value |= MASK_BasicModeBaseRegisterSelection;
        }
    }

    /**
     * Setter
     * @param flag new value
     */
    public void setQuarterWordModeEnabled(
        final boolean flag
    ) {
        _value &= ~MASK_QuarterWordModeEnabled;
        if (flag) {
            _value |= MASK_QuarterWordModeEnabled;
        }
    }
}
