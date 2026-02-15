/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.interrupts;

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

        Reason(int code) { _code = (short)code; }

        public short getCode() { return _code; }
    }

    private final Reason _reason;

    public DataExceptionInterrupt(
        final Reason reason
    ) {
        super(InterruptClass.DataException, ConditionCategory.Fault, Synchrony.Synchronous, Deferrability.Exigent, InterruptPoint.MidExecution);
        _reason = reason;
    }

    public Reason getReason() { return _reason; }
    @Override public byte getShortStatusField() { return (byte)_reason.getCode(); }
}
