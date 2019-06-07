/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib.functions.conditionalJump;

import com.kadware.em2200.baselib.InstructionWord;
import com.kadware.em2200.hardwarelib.InstructionProcessor;
import com.kadware.em2200.hardwarelib.exceptions.UnresolvedAddressException;
import com.kadware.em2200.hardwarelib.interrupts.MachineInterrupt;
import com.kadware.em2200.hardwarelib.functions.*;

/**
 * Handles the JNZ instruction f=074 j=01
 */
public class JNZFunctionHandler extends InstructionHandler {

    @Override
    public void handle(
        final InstructionProcessor ip,
        final InstructionWord iw
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        if (!ip.getExecOrUserARegister((int)iw.getA()).isZero()) {
            int counter = (int)ip.getJumpOperand();
            ip.setProgramCounter(counter, true);
        }
    }

    @Override
    public Instruction getInstruction() { return Instruction.JNZ; }
}
