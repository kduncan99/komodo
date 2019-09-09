/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.functions.conditionalJump;

import com.kadware.komodo.baselib.DoubleWord36;
import com.kadware.komodo.baselib.InstructionWord;
import com.kadware.komodo.hardwarelib.InstructionProcessor;
import com.kadware.komodo.hardwarelib.exceptions.UnresolvedAddressException;
import com.kadware.komodo.hardwarelib.interrupts.MachineInterrupt;
import com.kadware.komodo.hardwarelib.functions.InstructionHandler;

/**
 * Handles the DJZ instruction f=071 j=016
 */
public class DJZFunctionHandler extends InstructionHandler {

    @Override
    public void handle(
        final InstructionProcessor ip,
        final InstructionWord iw
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        DoubleWord36 dw36 = new DoubleWord36(ip.getExecOrUserARegister((int)iw.getA()).getW(),
                                             ip.getExecOrUserARegister((int)iw.getA() + 1).getW());
        if (dw36.isZero()) {
            int counter = ip.getJumpOperand(true);
            ip.setProgramCounter(counter, true);
        }
    }

    @Override
    public Instruction getInstruction() { return Instruction.DJZ; }
}
