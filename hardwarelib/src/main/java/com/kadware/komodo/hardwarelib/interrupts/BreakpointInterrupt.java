/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.interrupts;

/**
 * Represents a particular machine interrupt class
 * The Breakpoint interrupt occurs if the H-bit of the Breakpoint Register is clear,
 * and one of the following conditions is met:
 *      The Real or Absolute Address contained in the Breakpoint Register is equal to the
 *          Real or Absolute Address of the current instruction, and the breakpoint P-bit is set.
 *      The Real or Absolute Address contained in the Breakpoint Register is equal to a Real or Absolute operand address and
 *          The operation is a read and the breakpoint R-bit is set or
 *          The operation is a write and the breakpoint W-bit is set.
 * A Breakpoint interrupt may be caused without an actual occurrence of a match when a
 * User Return instruction is executed with the Breakpoint Pending bit set in the Indicator/Key Register.
 */

public class BreakpointInterrupt extends MachineInterrupt {

    /**
     * Constructor
     */
    public BreakpointInterrupt(
    ) {
        super(InterruptClass.Breakpoint, ConditionCategory.NonFault, Synchrony.Pended, Deferrability.Exigent, InterruptPoint.BetweenInstructions);
    }
}
