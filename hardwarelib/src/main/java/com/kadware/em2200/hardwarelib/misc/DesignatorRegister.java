/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
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
     * Initial value register
     * <p>
     * @param value
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
     * <p>
     * @return
     */
    public boolean getActivityLevelQueueMonitorEnabled(
    ) {
        return (_value & MASK_ActivityLevelQueueMonitorEnabled) != 0;
    }

    /**
     * Getter
     * <p>
     * @return
     */
    public boolean getFaultHandlingInProgress(
    ) {
        return (_value & MASK_FaultHandlingInProgress) != 0;
    }

    /**
     * Getter
     * <p>
     * @return
     */
    public boolean getExecutive24BitIndexingEnabled(
    ) {
        return (_value & MASK_Executive24BitIndexingEnabled) != 0;
    }

    /**
     * Getter
     * <p>
     * @return
     */
    public boolean getQuantumTimerEnabled(
    ) {
        return (_value & MASK_QuantumTimerEnabled) != 0;
    }

    /**
     * Getter
     * <p>
     * @return
     */
    public boolean getDeferrableInterruptEnabled(
    ) {
        return (_value & MASK_DeferrableInterruptEnabled) != 0;
    }

    /**
     * Getter
     * <p>
     * @return
     */
    public int getProcessorPrivilege(
    ) {
        return (int)((_value & (MASK_B14 | MASK_B15)) >> 20);
    }

    /**
     * Getter
     * <p>
     * @return
     */
    public boolean getBasicModeEnabled(
    ) {
        return (_value & MASK_BasicModeEnabled) != 0;
    }

    /**
     * Getter
     * <p>
     * @return
     */
    public boolean getExecRegisterSetSelected(
    ) {
        return (_value & MASK_ExecRegisterSetSelected) != 0;
    }

    /**
     * Getter
     * <p>
     * @return
     */
    public boolean getCarry(
    ) {
        return (_value & MASK_Carry) != 0;
    }

    /**
     * Getter
     * <p>
     * @return
     */
    public boolean getOverflow(
    ) {
        return (_value & MASK_Overflow) != 0;
    }

    /**
     * Getter
     * <p>
     * @return
     */
    public boolean getCharacteristicUnderflow(
    ) {
        return (_value & MASK_CharacteristicUnderflow) != 0;
    }

    /**
     * Getter
     * <p>
     * @return
     */
    public boolean getCharacteristicOverflow(
    ) {
        return (_value & MASK_CharacteristicOverflow) != 0;
    }

    /**
     * Getter
     * <p>
     * @return
     */
    public boolean getDivideCheck(
    ) {
        return (_value & MASK_DivideCheck) != 0;
    }

    /**
     * Getter
     * <p>
     * @return
     */
    public boolean getOperationTrapEnabled(
    ) {
        return (_value & MASK_OperationTrapEnabled) != 0;
    }

    /**
     * Getter
     * <p>
     * @return
     */
    public boolean getArithmeticExceptionEnabled(
    ) {
        return (_value & MASK_ArithmeticExceptionEnabled) != 0;
    }

    /**
     * Getter
     * <p>
     * @return
     */
    public boolean getBasicModeBaseRegisterSelection(
    ) {
        return (_value & MASK_BasicModeBaseRegisterSelection) != 0;
    }

    /**
     * Getter
     * <p>
     * @return
     */
    public boolean getQuarterWordModeEnabled(
    ) {
        return (_value & MASK_QuarterWordModeEnabled) != 0;
    }

    /**
     * Setter
     * <p>
     * @param flag
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
     * <p>
     * @param flag
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
     * <p>
     * @param flag
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
     * <p>
     * @param flag
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
     * <p>
     * @param flag
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
     * <p>
     * @param value
     */
    public void setProcessorPrivilege(
        final int value
    ) {
        _value = (value & ~MASK_ProcessorPrivilege) | ((value & 03) << 20);
    }

    /**
     * Setter
     * <p>
     * @param flag
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
     * <p>
     * @param flag
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
     * <p>
     * @param flag
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
     * <p>
     * @param flag
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
     * <p>
     * @param flag
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
     * <p>
     * @param flag
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
     * <p>
     * @param flag
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
     * <p>
     * @param flag
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
     * <p>
     * @param flag
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
     * <p>
     * @param flag
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
     * <p>
     * @param flag
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
