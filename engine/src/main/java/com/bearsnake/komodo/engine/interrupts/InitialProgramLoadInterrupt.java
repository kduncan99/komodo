/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.interrupts;

/**
 * Represents a particular machine interrupt class
 * Generated internally when an IP receives a request from the entity controlling IPL, to perform an IPL.
 */

public class InitialProgramLoadInterrupt extends MachineInterrupt {

    public InitialProgramLoadInterrupt() {
        super(InterruptClass.InitialProgramLoad,
              ConditionCategory.NonFault,
              Synchrony.None,
              Deferrability.None,
              InterruptPoint.MidExecution);
    }
}
