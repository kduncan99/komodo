/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib.interrupts;

import com.kadware.em2200.baselib.Word36;

/**
 * Represents a particular machine interrupt class
 * Results when address resolution completes successfully, but the target address exists in a context
 * in which execution cannot continue.
 */
public class TerminalAddressingExceptionInterrupt extends MachineInterrupt {

    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Nested enumerations
    //  ----------------------------------------------------------------------------------------------------------------------------

    public enum Reason {
        GBitSetInTargetBD(1),
        EnterAccessDenied(2),
        ValidatedEntryError(2),
        BaseRegisterSelectionError(2),
        FunctionalityNotSupported(010),
        RCSTrap(012);

        private final short _code;

        Reason(
            final int code
        ) {
            _code = (short)code;
        }

        public short getCode(
        ) {
            return _code;
        }
    };


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Class attributes
    //  ----------------------------------------------------------------------------------------------------------------------------

    private final short _bankDescriptorIndex;   // 15 bits significant
    private final byte _bankLevel;              // 3 bits significant
    private final Reason _reason;


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Constructors
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Constructor
     * <p>
     * @param reason
     * @param bankLevel
     * @param bankDescriptorIndex
     */
    public TerminalAddressingExceptionInterrupt(
        final Reason reason,
        final int bankLevel,
        final int bankDescriptorIndex
    ) {
        super(InterruptClass.Diagnostic, ConditionCategory.Fault, Synchrony.Pended, Deferrability.Exigent, InterruptPoint.MidExecution);
        _bankLevel = (byte)(bankLevel & 07);
        _bankDescriptorIndex = (short)(bankDescriptorIndex & 077777);
        _reason = reason;
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Accessors
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Getter
     * <p>
     * @return
     */
    public short getBankDescriptorIndex(
    ) {
        return _bankDescriptorIndex;
    }

    /**
     * Getter
     * <p>
     * @return
     */
    public byte getBankLevel(
    ) {
        return _bankLevel;
    }

    /**
     * Getter
     * <p>
     * @return
     */
    public Reason getReason(
    ) {
        return _reason;
    }

    /**
     * Getter
     * <p>
     * @return
     */
    @Override
    public Word36 getInterruptStatusWord1(
    ) {
        long levelBDI = (_bankLevel << 15) | _bankDescriptorIndex;
        Word36 result = new Word36();
        result.setH1(levelBDI);
        return result;
    }

    /**
     * Getter
     * <p>
     * @return
     */
    @Override
    public byte getShortStatusField(
    ) {
        return (byte)_reason.getCode();
    }

    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Instance methods
    //  ----------------------------------------------------------------------------------------------------------------------------


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Static methods
    //  ----------------------------------------------------------------------------------------------------------------------------
}
