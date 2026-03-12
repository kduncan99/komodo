/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.interrupts;

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

    private final long _realAddressLower;
    private final long _realAddressUpper;
    private final RecoveryAction _recoveryAction;
    private boolean _restartable;

    public HardwareCheckInterrupt(
        final RecoveryAction recoveryAction,
        final boolean restartable,
        final long realAddressUpper,
        final long realAddressLower
    ) {
        super(InterruptClass.HardwareCheck,
              ConditionCategory.None,
              Synchrony.None,
              Deferrability.Exigent,
              InterruptPoint.None);
        _recoveryAction = recoveryAction;
        _restartable = restartable;
        _realAddressUpper = realAddressUpper;
        _realAddressLower = realAddressLower;
    }

    public long getRealAddressLower() {
        return _realAddressLower;
    }

    public long getRealAddressUpper() {
        return _realAddressUpper;
    }

    public RecoveryAction getRecoveryAction() {
        return _recoveryAction;
    }

    public boolean isRestartable() {
        return _restartable;
    }

    @Override
    public long getInterruptStatusWord0() {
        var result = (long)(_recoveryAction.getCode() & 0_77) << 30;
        if (_restartable) {
            result |= 0_004000_000000L;
        }
        result |= _realAddressUpper & 0_003777_777777L; // 28 bits
        return result;
    }

    @Override
    public long getInterruptStatusWord1() {
        return _realAddressLower & 0_777777_777777L;
    }
}
