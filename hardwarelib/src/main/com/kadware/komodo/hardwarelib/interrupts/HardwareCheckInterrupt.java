/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.interrupts;

import com.kadware.komodo.baselib.Word36;

/**
 * Represents a particular machine interrupt class
 */
public class HardwareCheckInterrupt extends MachineInterrupt {

    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Nested enumerations
    //  ----------------------------------------------------------------------------------------------------------------------------

    public enum RecoveryAction {
        NoRecoveryActionRequired(0),
        ReinitializeWord(1),
        DownWord(2),
        DownPartitionableStorageUnit(3),
        CheckStorageAddress(4),
        DownIP(8),
        DownIPStorageInterface(9);

        private final short _code;

        RecoveryAction(
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

    private final long _realAddress;
    private final RecoveryAction _recoveryAction;
    private boolean _restartable;


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Constructors
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Constructor
     * <p>
     * @param recoveryAction
     * @param restartable
     * @param realAddress
     */
    public HardwareCheckInterrupt(
        final RecoveryAction recoveryAction,
        final boolean restartable,
        final long realAddress
    ) {
        super(InterruptClass.HardwareCheck, ConditionCategory.None, Synchrony.None, Deferrability.Exigent, InterruptPoint.None);
        _recoveryAction = recoveryAction;
        _restartable = restartable;
        _realAddress = realAddress;
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Accessors
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Getter
     * <p>
     * @return
     */
    public long getRealAddress(
    ) {
        return _realAddress;
    }

    /**
     * Getter
     * <p>
     * @return
     */
    public RecoveryAction getRecoveryAction(
    ) {
        return _recoveryAction;
    }

    /**
     * Getter
     * <p>
     * @return
     */
    public boolean isRestartable(
    ) {
        return _restartable;
    }

    /**
     * Getter
     * <p>
     * @return
     */
    @Override
    public Word36 getInterruptStatusWord0(
    ) {
        return new Word36(_realAddress);
    }

    @Override
    public Word36 getInterruptStatusWord1(
    ) {
        Word36 result = new Word36();
        result.setS1(_restartable ? 1 : 0);
        result.setH2(_recoveryAction.getCode());
        return result;
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Instance methods
    //  ----------------------------------------------------------------------------------------------------------------------------


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Static methods
    //  ----------------------------------------------------------------------------------------------------------------------------
}
