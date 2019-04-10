/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib.interrupts;

import com.kadware.em2200.baselib.Word36;

/**
 * Represents a particular machine interrupt class
 * Occurs when software executes an ER instruction (in basic mode) or a SIGNAL instruction.
 */
public class SignalInterrupt extends MachineInterrupt {

    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Nested enumerations
    //  ----------------------------------------------------------------------------------------------------------------------------

    public enum SignalType {
        ExecutiveRequest(0),
        Signal(1);

        private final short _code;

        SignalType(
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

    private final long _index;
    private final SignalType _signalType;


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Constructors
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Constructor
     * <p>
     * @param signalType
     * @param index
     */
    public SignalInterrupt(
        final SignalType signalType,
        final long index
    ) {
        super(InterruptClass.Signal, ConditionCategory.NonFault, Synchrony.Synchronous, Deferrability.Exigent, InterruptPoint.BetweenInstructions);
        _signalType = signalType;
        _index = index;
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Accessors
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Getter
     * <p>
     * @return
     */
    public long getIndex(
    ) {
        return _index;
    }

    /**
     * Getter
     * <p>
     * @return
     */
    public SignalType getSignalType(
    ) {
        return _signalType;
    }

    /**
     * Getter
     * <p>
     * @return
     */
    @Override
    public Word36 getInterruptStatusWord0(
    ) {
        return new Word36(_index);
    }

    /**
     * Getter
     * <p>
     * @return
     */
    @Override
    public byte getShortStatusField(
    ) {
        return (byte)_signalType.getCode();
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Instance methods
    //  ----------------------------------------------------------------------------------------------------------------------------


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Static methods
    //  ----------------------------------------------------------------------------------------------------------------------------
}
