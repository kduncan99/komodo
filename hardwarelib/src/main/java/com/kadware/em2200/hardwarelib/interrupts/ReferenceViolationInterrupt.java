/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib.interrupts;

/**
 * Represents a particular machine interrupt class
 */
public class ReferenceViolationInterrupt extends MachineInterrupt {

    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Nested enumerations
    //  ----------------------------------------------------------------------------------------------------------------------------

    public enum ErrorType {
        GRSViolation(0),
        StorageLimitsViolation(1),
        ReadAccessViolation(2),
        WriteAccessViolation(3);

        private final short _code;

        ErrorType(
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

    private final ErrorType _errorType;

    /**
     * Indicates that this interrupt was raised when using the program address register to fetch an instruction.
     * Does not include indirect addressing references, nor references to the target of an EX or EXR instruction.
     * _errorType will not be GRSViolation nor WriteAccessViolation if this is set.
     */
    private final boolean _fetchFlag;


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Constructors
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Constructor
     * <p>
     * @param errorType
     * @param fetchFlag
     */
    public ReferenceViolationInterrupt(
        final ErrorType errorType,
        final boolean fetchFlag
    ) {
        super(InterruptClass.ReferenceViolation, ConditionCategory.Fault, Synchrony.Synchronous, Deferrability.Exigent, InterruptPoint.MidExecution);
        _errorType = errorType;
        _fetchFlag = fetchFlag;
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Accessors
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Getter
     * <p>
     * @return
     */
    public ErrorType getErrorType(
    ) {
        return _errorType;
    }

    /**
     * Getter
     * <p>
     * @return
     */
    public boolean getFetchFlag(
    ) {
        return _fetchFlag;
    }

    @Override
    public byte getShortStatusField(
    ) {
        return (byte)((_errorType.getCode() << 4) | (_fetchFlag ? 1 : 0));
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Instance methods
    //  ----------------------------------------------------------------------------------------------------------------------------


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Static methods
    //  ----------------------------------------------------------------------------------------------------------------------------
}
