/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib.functions;

import com.kadware.komodo.baselib.InstructionWord;
import com.kadware.em2200.hardwarelib.InstructionProcessor;
import com.kadware.em2200.hardwarelib.exceptions.UnresolvedAddressException;
import com.kadware.em2200.hardwarelib.interrupts.MachineInterrupt;

/**
 * Base class for all the instruction handlers
 */
@SuppressWarnings("Duplicates")
public abstract class FunctionHandler {

    /**
     * Handles a function (i.e., an instruction)
     * @param ip reference to the owning InstructionProcessor
     * @param instructionWord reference to F0 (_currentInstruction)
     * @throws MachineInterrupt for any conditions which need to raise an interrupt
     * @throws UnresolvedAddressException for basic mode indirect addressing mid-resolution situation
     */
    public abstract void handle(
        final InstructionProcessor ip,
        final InstructionWord instructionWord
    ) throws MachineInterrupt,
             UnresolvedAddressException;
}
