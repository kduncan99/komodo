/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib.functions.conditionalJump;

import com.kadware.em2200.baselib.InstructionWord;
import com.kadware.em2200.baselib.OnesComplement;
import com.kadware.em2200.hardwarelib.InstructionProcessor;
import com.kadware.em2200.hardwarelib.exceptions.UnresolvedAddressException;
import com.kadware.em2200.hardwarelib.interrupts.MachineInterrupt;
import com.kadware.em2200.hardwarelib.functions.*;

/**
 * Handles the DJZ instruction f=071 j=016
 */
public class DJZFunctionHandler extends InstructionHandler {

    private final long[] _operand = new long[2];

    @Override
    public synchronized void handle(
        final InstructionProcessor ip,
        final InstructionWord iw
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        _operand[0] = ip.getExecOrUserARegister((int)iw.getA()).getW();
        _operand[1] = ip.getExecOrUserARegister((int)iw.getA() + 1).getW();
        if (OnesComplement.isZero72(_operand)) {
            int counter = (int)ip.getJumpOperand();
            ip.setProgramCounter(counter, true);
        }
    }

    @Override
    public Instruction getInstruction() { return Instruction.DJZ; }
}
