/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.interrupts;

/**
 * Represents a particular machine interrupt class
 * The dayclock's comparator is less than or equal to the dayclock's clock value.
 * We probably won't use this, but just in case...
 */

public class DayclockInterrupt extends MachineInterrupt {

    public DayclockInterrupt(Synchrony synchrony) {
        super(InterruptClass.Dayclock, ConditionCategory.NonFault, synchrony, Deferrability.Deferrable, InterruptPoint.MidExecution);
    }
}
