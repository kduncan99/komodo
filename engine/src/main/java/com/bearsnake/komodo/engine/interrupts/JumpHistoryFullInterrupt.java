/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.interrupts;

/**
 * Represents a particular machine interrupt class
 * When enabled, this interrupt is generated when the conditionalJump history buffer if full.
 * Software must store the buffer *somewhere* to avoid losing conditionalJump history.
 */
public class JumpHistoryFullInterrupt extends MachineInterrupt {

    public JumpHistoryFullInterrupt() {
        super(InterruptClass.JumpHistoryFull,
              ConditionCategory.NonFault,
              Synchrony.Asynchronous,
              Deferrability.Exigent,
              InterruptPoint.BetweenInstructions);
    }
}
