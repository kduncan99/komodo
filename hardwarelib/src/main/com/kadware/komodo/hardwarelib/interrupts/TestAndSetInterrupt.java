/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.interrupts;

import com.kadware.komodo.baselib.Word36;

/**
 * Represents a particular machine interrupt class
 * Occurs during execution of a TS instruction, when bit 5 of the operand is set.
 */
public class TestAndSetInterrupt extends MachineInterrupt {

    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Nested enumerations
    //  ----------------------------------------------------------------------------------------------------------------------------


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Class attributes
    //  ----------------------------------------------------------------------------------------------------------------------------

    private final int _baseRegisterIndex;
    private final int _relativeAddress;


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Constructors
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Constructor
     */
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


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Accessors
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Getter which may be overridden by the subclass
     * <p>
     * @return
     */
    @Override
    public Word36 getInterruptStatusWord0(
    ) {
        long value = ((long)_baseRegisterIndex) << 30;
        value |= (_relativeAddress & 0_77_777777);
        return new Word36(value);
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Instance methods
    //  ----------------------------------------------------------------------------------------------------------------------------


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Static methods
    //  ----------------------------------------------------------------------------------------------------------------------------
}
