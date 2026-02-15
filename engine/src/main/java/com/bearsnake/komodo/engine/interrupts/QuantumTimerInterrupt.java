/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.interrupts;

/**
 * Represents a particular machine interrupt class
 * Generated if the quantum timer goes negative and DB12 is set (enabling this interrupt).
 */

public class QuantumTimerInterrupt extends MachineInterrupt {

    public QuantumTimerInterrupt() {
        super(InterruptClass.QuantumTimer, ConditionCategory.NonFault, Synchrony.Pended, Deferrability.Exigent, InterruptPoint.MidExecution);
    }
}
