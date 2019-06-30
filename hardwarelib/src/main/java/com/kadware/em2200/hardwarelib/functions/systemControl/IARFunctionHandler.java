/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib.functions.systemControl;

import com.kadware.komodo.baselib.InstructionWord;
import com.kadware.em2200.hardwarelib.InstructionProcessor;
import com.kadware.em2200.hardwarelib.interrupts.*;
import com.kadware.em2200.hardwarelib.functions.*;

/**
 * Handles the IAR instruction (extended mode only f=073 j=017 a=06)
 * Causes the processor to error halt and notify the SCF.
 * <p>
 * @throws MachineInterrupt
 */
public class IARFunctionHandler extends InstructionHandler {

    @Override
    public void handle(
        final InstructionProcessor ip,
        final InstructionWord instructionWord
    ) throws MachineInterrupt {
        if (ip.getDesignatorRegister().getProcessorPrivilege() > 0) {
            throw new InvalidInstructionInterrupt(InvalidInstructionInterrupt.Reason.InvalidProcessorPrivilege);
        }

        long detail = ip.getImmediateOperand();
        ip.stop(InstructionProcessor.StopReason.InitiateAutoRecovery, detail);
    }

    @Override
    public Instruction getInstruction() { return Instruction.IAR; }
}
