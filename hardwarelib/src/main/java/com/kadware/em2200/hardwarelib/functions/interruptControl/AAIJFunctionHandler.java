/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib.functions.interruptControl;

import com.kadware.em2200.baselib.InstructionWord;
import com.kadware.em2200.hardwarelib.InstructionProcessor;
import com.kadware.em2200.hardwarelib.exceptions.UnresolvedAddressException;
import com.kadware.em2200.hardwarelib.functions.InstructionHandler;
import com.kadware.em2200.hardwarelib.interrupts.*;

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
