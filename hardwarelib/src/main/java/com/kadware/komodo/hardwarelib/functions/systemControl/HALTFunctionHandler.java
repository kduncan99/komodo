/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.functions.systemControl;

import com.kadware.komodo.baselib.InstructionWord;
import com.kadware.komodo.hardwarelib.InstructionProcessor;
import com.kadware.komodo.hardwarelib.functions.InstructionHandler;
import com.kadware.komodo.hardwarelib.interrupts.InvalidInstructionInterrupt;
import com.kadware.komodo.hardwarelib.interrupts.MachineInterrupt;

/**
 * Handles the HALT instruction (f=077 j=017 a=017)
 * Causes the processor to error halt and notify the SCF.
 */
public class HALTFunctionHandler extends InstructionHandler {

    @Override
    public void handle(
        final InstructionProcessor ip,
        final InstructionWord instructionWord
    ) throws MachineInterrupt {
        if (!ip.getDevelopmentMode() && (ip.getDesignatorRegister().getProcessorPrivilege() > 0)) {
            throw new InvalidInstructionInterrupt(InvalidInstructionInterrupt.Reason.InvalidProcessorPrivilege);
        }

        long detail = ip.getImmediateOperand();
        ip.stop(InstructionProcessor.StopReason.Debug, detail);
    }

    @Override
    public Instruction getInstruction() { return Instruction.HALT; }
}
