/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.functions.conditionalJump;

import com.kadware.komodo.baselib.GeneralRegister;
import com.kadware.komodo.baselib.InstructionWord;
import com.kadware.komodo.baselib.OnesComplement;
import com.kadware.komodo.hardwarelib.InstructionProcessor;
import com.kadware.komodo.hardwarelib.exceptions.UnresolvedAddressException;
import com.kadware.komodo.hardwarelib.interrupts.MachineInterrupt;
import com.kadware.komodo.hardwarelib.functions.InstructionHandler;

/**
 * Handles the JPS instruction f=072 j=02
 */
public class JPSFunctionHandler extends InstructionHandler {

    @Override
    public void handle(
        final InstructionProcessor ip,
        final InstructionWord iw
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        GeneralRegister reg = ip.getExecOrUserARegister((int)iw.getA());
        long operand = reg.getW();
        if (OnesComplement.isPositive36(operand)) {
            int counter = (int)ip.getJumpOperand(true);
            ip.setProgramCounter(counter, true);
        }

        reg.setW(OnesComplement.leftShiftCircular36(operand, 1));
    }

    @Override
    public Instruction getInstruction() { return Instruction.JPS; }
}
