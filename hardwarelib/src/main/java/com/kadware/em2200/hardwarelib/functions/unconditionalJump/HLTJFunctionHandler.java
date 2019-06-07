/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib.functions.unconditionalJump;

import com.kadware.em2200.baselib.InstructionWord;
import com.kadware.em2200.hardwarelib.InstructionProcessor;
import com.kadware.em2200.hardwarelib.exceptions.UnresolvedAddressException;
import com.kadware.em2200.hardwarelib.interrupts.InvalidInstructionInterrupt;
import com.kadware.em2200.hardwarelib.interrupts.MachineInterrupt;
import com.kadware.em2200.hardwarelib.functions.*;

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
        int counter = (int)ip.getJumpOperand();
        ip.setProgramCounter(counter, true);
        ip.stop(InstructionProcessor.StopReason.HaltJumpExecuted, 0);
    }

    @Override
    public Instruction getInstruction() { return Instruction.HLTJ; }
}
