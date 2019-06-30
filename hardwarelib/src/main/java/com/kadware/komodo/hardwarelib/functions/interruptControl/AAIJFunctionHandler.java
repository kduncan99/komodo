/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.functions.interruptControl;

import com.kadware.komodo.baselib.InstructionWord;
import com.kadware.komodo.hardwarelib.InstructionProcessor;
import com.kadware.komodo.hardwarelib.exceptions.UnresolvedAddressException;
import com.kadware.komodo.hardwarelib.functions.InstructionHandler;
import com.kadware.komodo.hardwarelib.interrupts.InvalidInstructionInterrupt;
import com.kadware.komodo.hardwarelib.interrupts.MachineInterrupt;

/**
 * Handles the AAIJ instruction f=074 j=014 a=06 for extended mode (requires PP=0),
 *                              f=074 j=07  a=not-used for basic mode (any PP)
 */
public class AAIJFunctionHandler extends InstructionHandler {

    @Override
    public void handle(
        final InstructionProcessor ip,
        final InstructionWord iw
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        if (!ip.getDesignatorRegister().getBasicModeEnabled()
            && (ip.getDesignatorRegister().getProcessorPrivilege() > 0)) {
            throw new InvalidInstructionInterrupt(InvalidInstructionInterrupt.Reason.InvalidProcessorPrivilege);
        }

        ip.getDesignatorRegister().setDeferrableInterruptEnabled(true);
        int counter = (int)ip.getJumpOperand(true);
        ip.setProgramCounter(counter, true);
    }

    @Override
    public Instruction getInstruction() { return Instruction.AAIJ; }
}
