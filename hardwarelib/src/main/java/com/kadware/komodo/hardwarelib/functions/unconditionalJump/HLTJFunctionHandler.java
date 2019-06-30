/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.functions.unconditionalJump;

import com.kadware.komodo.baselib.InstructionWord;
import com.kadware.komodo.hardwarelib.InstructionProcessor;
import com.kadware.komodo.hardwarelib.exceptions.UnresolvedAddressException;
import com.kadware.komodo.hardwarelib.interrupts.InvalidInstructionInterrupt;
import com.kadware.komodo.hardwarelib.interrupts.MachineInterrupt;
import com.kadware.komodo.hardwarelib.functions.InstructionHandler;

/**
 * Handles the HLTJ instruction basic mode f=074 j=015 a=05
 */
public class HLTJFunctionHandler extends InstructionHandler {

    @Override
    public void handle(
        final InstructionProcessor ip,
        final InstructionWord iw
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        if (ip.getDesignatorRegister().getProcessorPrivilege() > 0) {
            throw new InvalidInstructionInterrupt(InvalidInstructionInterrupt.Reason.InvalidProcessorPrivilege);
        }

        //  Always conditionalJump, but halt thereafter
        int counter = (int)ip.getJumpOperand(true);
        ip.setProgramCounter(counter, true);
        ip.stop(InstructionProcessor.StopReason.HaltJumpExecuted, 0);
    }

    @Override
    public Instruction getInstruction() { return Instruction.HLTJ; }
}
