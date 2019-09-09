/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.functions.generalLoad;

import com.kadware.komodo.baselib.InstructionWord;
import com.kadware.komodo.baselib.Word36;
import com.kadware.komodo.hardwarelib.InstructionProcessor;
import com.kadware.komodo.hardwarelib.exceptions.UnresolvedAddressException;
import com.kadware.komodo.hardwarelib.interrupts.MachineInterrupt;
import com.kadware.komodo.hardwarelib.functions.InstructionHandler;

/**
 * Handles the DLM instruction f=071 j=015
 */
public class DLMFunctionHandler extends InstructionHandler {

    @Override
    public void handle(
        final InstructionProcessor ip,
        final InstructionWord iw
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        long[] operands = new long[2];
        ip.getConsecutiveOperands(true, operands);
        if (Word36.isNegative(operands[0])) {
            operands[0] = (~operands[0]) & Word36.BIT_MASK;
            operands[1] = (~operands[1]) & Word36.BIT_MASK;
        }

        int grsIndex = ip.getExecOrUserARegisterIndex((int)iw.getA());
        ip.setGeneralRegister(grsIndex, operands[0]);
        ip.setGeneralRegister(grsIndex + 1, operands[1]);
    }

    @Override
    public Instruction getInstruction() { return Instruction.DLM; }
}
