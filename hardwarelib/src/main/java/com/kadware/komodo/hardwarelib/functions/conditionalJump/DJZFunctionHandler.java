/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.functions.conditionalJump;

import com.kadware.komodo.baselib.InstructionWord;
import com.kadware.komodo.baselib.OnesComplement;
import com.kadware.komodo.hardwarelib.InstructionProcessor;
import com.kadware.komodo.hardwarelib.exceptions.UnresolvedAddressException;
import com.kadware.komodo.hardwarelib.interrupts.MachineInterrupt;
import com.kadware.komodo.hardwarelib.functions.InstructionHandler;

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
            int counter = (int)ip.getJumpOperand(true);
            ip.setProgramCounter(counter, true);
        }
    }

    @Override
    public Instruction getInstruction() { return Instruction.DJZ; }
}
