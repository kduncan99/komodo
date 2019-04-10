/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib.interrupts;

import com.kadware.em2200.baselib.Word36;

/**
 * Represents a particular machine interrupt class
 * Intended for use in debugging and testing.
 */
public class DiagnosticInterrupt extends MachineInterrupt {

    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Nested enumerations
    //  ----------------------------------------------------------------------------------------------------------------------------

    public enum Reason {
        InvalidAbsoluteAddress(0);

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
    private final Word36 _statusWord0;
    private final Word36 _statusWord1;


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Constructors
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Constructor
     * <p>
     * @param reason
     * @param statusWord0
     * @param statusWord1
     */
    public DiagnosticInterrupt(
        final Reason reason,
        final Word36 statusWord0,
        final Word36 statusWord1
    ) {
        super(InterruptClass.Diagnostic, ConditionCategory.Fault, Synchrony.Pended, Deferrability.Exigent, InterruptPoint.MidExecution);
        _reason = reason;
        _statusWord0 = statusWord0;
        _statusWord1 = statusWord1;
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
    public Word36 getInterruptStatusWord0(
    ) {
        return _statusWord0;
    }

    /**
     * Getter
     * <p>
     * @return
     */
    @Override
    public Word36 getInterruptStatusWord1(
    ) {
        return _statusWord1;
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
