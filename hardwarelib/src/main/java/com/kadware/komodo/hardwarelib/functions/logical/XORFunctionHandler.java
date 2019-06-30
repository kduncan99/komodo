/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.functions.logical;

import com.kadware.komodo.baselib.InstructionWord;
import com.kadware.komodo.hardwarelib.InstructionProcessor;
import com.kadware.komodo.hardwarelib.exceptions.UnresolvedAddressException;
import com.kadware.komodo.hardwarelib.interrupts.MachineInterrupt;
import com.kadware.komodo.hardwarelib.functions.InstructionHandler;

/**
 * Handles the XOR instruction f=041
 */
public class XORFunctionHandler extends InstructionHandler {

    @Override
    public void handle(
        final InstructionProcessor ip,
        final InstructionWord iw
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        long operand1 = ip.getExecOrUserARegister((int)iw.getA()).getW();
        long operand2 = ip.getOperand(true, true, true, true);
        long foo = operand1 ^ operand2;//????
        ip.getExecOrUserARegister((int)iw.getA() + 1).setW(operand1 ^ operand2);
    }

    @Override
    public Instruction getInstruction() { return Instruction.XOR; }
}
