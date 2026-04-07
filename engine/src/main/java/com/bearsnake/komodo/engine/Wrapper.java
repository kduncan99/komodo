/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine;

import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;

/**
 * A wrapper around the engine which manages the actual work flow of the engine.
 */
public interface Wrapper {

    /**
     * Retrieves an interrupt if one has been posted, otherwise returns null.
     * If multiple interrupts are pending, the highest priority interrupt is returned.
     * @return the highest-priority interrupt currently pending, or null if none are pending
     */
    MachineInterrupt getInterrupt();

    /**
     * Posts an interrupt to be processed by the wrapper's interrupt handler.
     * @param interrupt interrupt to be posted
     */
    void postInterrupt(final MachineInterrupt interrupt);

    /**
     * Retrieves the halt code indicating the most recent halt.
     */
    HaltCode getHaltCode();

    /**
     * Indicates whether the engine is currently halted.
     */
    boolean isHalted();

    /**
     * Sets the halt code. The wrapper should take any necessary action.
     * Unless the halt is cleared, the wrapper should not invoke the engine's cycle.
     */
    void setHalted(final HaltCode haltCode);
}
