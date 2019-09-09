/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.functions.conditionalJump;

import com.kadware.komodo.baselib.GeneralRegister;
import com.kadware.komodo.baselib.InstructionWord;
import com.kadware.komodo.baselib.Word36;
import com.kadware.komodo.hardwarelib.InstructionProcessor;
import com.kadware.komodo.hardwarelib.exceptions.UnresolvedAddressException;
import com.kadware.komodo.hardwarelib.interrupts.MachineInterrupt;
import com.kadware.komodo.hardwarelib.functions.InstructionHandler;

/**
 * Handles the JNS instruction f=072 j=03
 */
public class JNSFunctionHandler extends InstructionHandler {

    @Override
    public void handle(
        final InstructionProcessor ip,
        final InstructionWord iw
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        GeneralRegister reg = ip.getExecOrUserARegister((int)iw.getA());
        long operand = reg.getW();
        if (Word36.isNegative(operand)) {
            int counter = ip.getJumpOperand(true);
            ip.setProgramCounter(counter, true);
        }

        reg.setW(Word36.leftShiftCircular(operand, 1));
    }

    @Override
    public Instruction getInstruction() { return Instruction.JNS; }
}
