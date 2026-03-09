/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.interrupts;

/**
 * Represents a particular machine interrupt class
 * Occurs during execution of a TS instruction, when bit 5 of the operand is set.
 */
public class TestAndSetInterrupt extends MachineInterrupt {

    private final int _baseRegisterIndex;
    private final int _relativeAddress;

    public TestAndSetInterrupt(
        final int baseRegisterIndex,
        final int relativeAddress
    ) {
        super(InterruptClass.TestAndSet,
              ConditionCategory.Fault,
              Synchrony.Synchronous,
              Deferrability.Exigent,
              InterruptPoint.IndirectExecute);

        _baseRegisterIndex = baseRegisterIndex;
        _relativeAddress = relativeAddress;
    }

    @Override
    public long getInterruptStatusWord0() {
        long value = ((long)_baseRegisterIndex) << 30;
        value |= (_relativeAddress & 0_77_777777);
        return value;
    }
}
