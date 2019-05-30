/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib.interrupts;

import com.kadware.em2200.baselib.Word36;

/**
 * Represents a particular machine interrupt class
 */
public class AddressingExceptionInterrupt extends MachineInterrupt {

    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Nested enumerations
    //  ----------------------------------------------------------------------------------------------------------------------------

    public enum Reason {
        FatalAddressingException(0),
        GBitSetGate(1),
        EnterAccessDenied(2),
        InvalidSourceLevelBDI(3),
        GateBankBoundaryViolation(4),
        InvalidISValue(5),
        GoToInhibitSet(6),
        BGitSetIndirect(9);

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
    public AddressingExceptionInterrupt(
        final Reason reason,
        final int bankLevel,
        final int bankDescriptorIndex
    ) {
        super(InterruptClass.AddressingException, ConditionCategory.Fault, Synchrony.Pended, Deferrability.Exigent, InterruptPoint.MidExecution);
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
