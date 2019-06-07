/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib.functions.conditionalJump;

import com.kadware.em2200.baselib.GeneralRegister;
import com.kadware.em2200.baselib.InstructionWord;
import com.kadware.em2200.baselib.OnesComplement;
import com.kadware.em2200.hardwarelib.InstructionProcessor;
import com.kadware.em2200.hardwarelib.exceptions.UnresolvedAddressException;
import com.kadware.em2200.hardwarelib.interrupts.MachineInterrupt;
import com.kadware.em2200.hardwarelib.functions.*;

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
            int counter = (int)ip.getJumpOperand();
            ip.setProgramCounter(counter, true);
        }

        reg.setW(OnesComplement.leftShiftCircular36(operand, 1));
    }

    @Override
    public Instruction getInstruction() { return Instruction.JPS; }
}
