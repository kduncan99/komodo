/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.interrupts;

import com.bearsnake.komodo.baselib.Word36;

/**
 * Represents a particular machine interrupt class
 */
public class HardwareCheckInterrupt extends MachineInterrupt {

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

        public short getCode() {
            return _code;
        }
    }

    private final long _realAddress;
    private final RecoveryAction _recoveryAction;
    private boolean _restartable;

    public HardwareCheckInterrupt(
        final RecoveryAction recoveryAction,
        final boolean restartable,
        final long realAddress
    ) {
        super(InterruptClass.HardwareCheck,
              ConditionCategory.None,
              Synchrony.None,
              Deferrability.Exigent,
              InterruptPoint.None);
        _recoveryAction = recoveryAction;
        _restartable = restartable;
        _realAddress = realAddress;
    }

    public long getRealAddress() {
        return _realAddress;
    }

    public RecoveryAction getRecoveryAction() {
        return _recoveryAction;
    }

    public boolean isRestartable() {
        return _restartable;
    }

    @Override
    public long getInterruptStatusWord0() {
        return _realAddress;
    }

    @Override
    public long getInterruptStatusWord1() {
        long result = 0;
        result = Word36.setS1(result, _restartable ? 1 : 0);
        result = Word36.setH2(result, _recoveryAction.getCode());
        return result;
    }
}
