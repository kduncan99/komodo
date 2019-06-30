/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.interrupts;

/**
 * Represents a particular machine interrupt class
 * Some operation completed, but with a status that must be reported to software.
 */
public class OperationTrapInterrupt extends MachineInterrupt {

    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Nested enumerations
    //  ----------------------------------------------------------------------------------------------------------------------------

    public enum Reason {
        FixedPointBinaryIntegerOverflow(0),
        FixedPointDecimalIntegerOverflow(1),
        MultiplySingleIntegerOverflow(2);

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
    public OperationTrapInterrupt(
        final Reason reason
    ) {
        super(InterruptClass.OperationTrap, ConditionCategory.NonFault, Synchrony.Synchronous, Deferrability.Exigent, InterruptPoint.IndirectExecute);
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
