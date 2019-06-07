/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib.functions.test;

import com.kadware.em2200.baselib.InstructionWord;
import com.kadware.em2200.baselib.OnesComplement;
import com.kadware.em2200.hardwarelib.InstructionProcessor;
import com.kadware.em2200.hardwarelib.exceptions.UnresolvedAddressException;
import com.kadware.em2200.hardwarelib.interrupts.MachineInterrupt;
import com.kadware.em2200.hardwarelib.functions.*;

/**
 * Handles the TPZL instruction extended mode f=050 a=012
 */
public class TPZLFunctionHandler extends InstructionHandler {

    @Override
    public void handle(
        final InstructionProcessor ip,
        final InstructionWord iw
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        //  Skip NI if (U) == +0 or (U) < -0
        long op = ip.getOperand(true, true, true, true);
        if (OnesComplement.isPositiveZero36(op)
                || (OnesComplement.isNegative36(op) && !OnesComplement.isNegativeZero36(op))) {
            ip.setProgramCounter(ip.getProgramAddressRegister().getProgramCounter() + 1, false);
        }
    }

    @Override
    public Instruction getInstruction() { return Instruction.TPZL; }
}
