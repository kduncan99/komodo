/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine;

import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;

/**
 * A subset of the external operating system.
 * The Engine calls us if we are defined to the engine, whenever an interrupt needs processed.
 */
public interface InterruptHandler {

    // Note that there is no direct means of halting the engine...
    // TODO what should we do for this? Anything?
    void handleInterrupt(MachineInterrupt interrupt);
}
