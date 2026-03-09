/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.interrupts;

/**
 * Represents a particular machine interrupt class
 */
public class HardwareDefaultInterrupt extends MachineInterrupt {

    /**
     * Constructor
     */
    public HardwareDefaultInterrupt() {
        super(InterruptClass.HardwareDefault,
              ConditionCategory.None,
              Synchrony.None,
              Deferrability.None,
              InterruptPoint.None);
    }
}
