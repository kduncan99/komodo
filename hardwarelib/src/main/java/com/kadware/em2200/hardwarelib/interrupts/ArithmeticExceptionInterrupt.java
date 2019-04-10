/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib.interrupts;

/**
 * Represents a particular machine interrupt class
 */
public class ArithmeticExceptionInterrupt extends MachineInterrupt {

    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Nested enumerations
    //  ----------------------------------------------------------------------------------------------------------------------------

    public enum Reason {
        CharacteristicOverflow(0),
        CharacteristicUnderflow(1),
        DivideCheck(2);                 //  Jose's error

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
    public ArithmeticExceptionInterrupt(
        final Reason reason
    ) {
        super(InterruptClass.Diagnostic, ConditionCategory.Fault, Synchrony.Synchronous, Deferrability.Exigent, InterruptPoint.IndirectExecute);
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
