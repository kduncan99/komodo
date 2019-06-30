/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.interrupts;

/**
 * Represents a particular machine interrupt class
 * Something went wrong during a BIMT or EDDE instruction
 */
public class DataExceptionInterrupt extends MachineInterrupt {

    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Nested enumerations
    //  ----------------------------------------------------------------------------------------------------------------------------

    public enum Reason {
        IllegalControlCharacter(0),
        TooManyStorageReferences(1),
        InvalidBitCount(2),
        SimulationControlISCINonZero(3),
        InvalidCharacterSize(3),
        InvalidStartingBit(3),
        XRegisterIsZero(3);

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

    private final Reason _reason;


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Constructors
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Constructor
     * <p>
     * @param reason
     */
    public DataExceptionInterrupt(
        final Reason reason
    ) {
        super(InterruptClass.DataException, ConditionCategory.Fault, Synchrony.Synchronous, Deferrability.Exigent, InterruptPoint.MidExecution);
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
